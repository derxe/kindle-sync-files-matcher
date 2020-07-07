package sync;

import sync.ISyncData;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SyncDataHeader implements ISyncData.Header {
    private int headerLength;
    private DataInputStream in;
    private List infoItems;
    private final Map keys2values = new HashMap();
    private int majorVersion;
    private int minorVersion;
    private long overallConfidenceScore;

    SyncDataHeader(IFile f) throws IOException, SyncDataHeaderException {
        this.in = new DataInputStream(f.open());
        try {
            read();
        } finally {
            this.in.close();
        }
    }

    public int getHeaderLength() {
        return this.headerLength;
    }

    static final class Pair {
        final String key;
        final Object val;

        Pair(String key2, Object val2) {
            this.key = key2;
            this.val = val2;
        }

        public int hashCode() {
            return (this.key.hashCode() << 16) * this.val.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair that = (Pair) o;
            if (!this.key.equals(that.key) || !this.val.equals(that.val)) {
                return false;
            }
            return true;
        }
    }

    static Object unroll(Object o) {
        if (!(o instanceof Pair)) {
            return o;
        }
        Pair p = (Pair) o;
        return new Object[]{p.key, unroll(p.val)};
    }

    private static int readMagicNumber(DataInputStream in2) throws IOException, SyncDataHeaderException {
        int magicNumber = u4(in2);
        if (magicNumber == -1596870349) {
            return magicNumber;
        }
        throw new SyncDataHeaderException("Invalid magic number:" + Integer.toHexString(magicNumber) + " != " + Integer.toHexString(-1596870349));
    }

    private int readMagicNumber() throws IOException, SyncDataHeaderException {
        return readMagicNumber(this.in);
    }

    private void read() throws IOException, SyncDataHeaderException {
        readMagicNumber();
        this.majorVersion = u2();
        this.minorVersion = u2();
        this.headerLength = u4();
        this.overallConfidenceScore = (long) u4();
        int itemCount = u4();
        this.infoItems = new ArrayList();
        for (int i = 0; i < itemCount; i++) {
            this.infoItems.add(infoItem());
        }
        this.infoItems.clear();
        this.infoItems = null;
    }

    private Object deRef(int index) {
        return this.infoItems.get(index);
    }

    private Pair readPair() throws IOException {
        String key = readString();
        Object val = infoItem();
        this.keys2values.put(key, val);
        return new Pair(key, val);
    }

    private String readString() throws IOException {
        return new String(readBytes(u2()));
    }

    private Object[] readArray() throws IOException {
        Object[] res = new Object[u2()];
        for (int i = 0; i < res.length; i++) {
            res[i] = infoItem();
        }
        return res;
    }

    private Object readRef() throws IOException {
        u1();
        return unroll(deRef(u2()));
    }

    private Object infoItem() throws IOException {
        int tag = u1();
        switch (tag) {
            case 1:
                return readPair();
            case 2:
                return readString();
            case 3:
                return Integer.valueOf(u4());
            case 4:
                return Float.valueOf(f4());
            case 5:
                return readArray();
            case 6:
                return readRef();
            default:
                throw new IllegalArgumentException("Invalid header info_item tag:" + tag);
        }
    }

    private byte[] readBytes(int length) throws IOException {
        byte[] res = new byte[length];
        this.in.read(res);
        return res;
    }

    private int u1() throws IOException {
        return this.in.readUnsignedByte();
    }

    private int u2() throws IOException {
        return u2(this.in);
    }

    private static int u2(DataInputStream in2) throws IOException {
        return in2.readUnsignedShort();
    }

    private int u4() throws IOException {
        return u4(this.in);
    }

    private static int u4(DataInputStream in2) throws IOException {
        return in2.readInt();
    }

    private float f4() throws IOException {
        return this.in.readFloat();
    }
}
