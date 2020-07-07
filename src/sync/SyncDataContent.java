package sync;

import sync.SyncIntegration;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

public final class SyncDataContent {
    private static final int ABOOK_END_POS_BYTE_OFFSET = 12;
    private static final int ABOOK_START_POS_BYTE_OFFSET = 8;
    private static final int EBOOK_END_POS_BYTE_OFFSET = 4;
    private static final int EBOOK_START_POS_BYTE_OFFSET = 0;
    private static final SyncIntegration.ILogger LOGGER = SyncIntegration.getDelegate().getLogger(SyncDataContent.class);
    private static final int NUM_BYTES_IN_A_RECORD = 16;
    private static final int PREDEFINED_SYNC_AUDIO_GAP = 1000;
    private final byte[] block;
    private final int blockSize;
    private Block[] blocks;
    private volatile int bp;
    private volatile int curBlockIndex;
    private IErrorHandler errorHandler;
    private long hiAbookEndPos;
    private long hiAbookPos;
    private long hiEbookPos;
    private long loAbookPos;
    private long loEbookPos;
    private final int numSearchesBeforeBinarySearch;
    private final RandomAccessFileReader reader;
    private final String source;
    private final long syncDataOffset;
    private boolean trace;

    interface IErrorHandler {
        void handle(Throwable th);
    }

    interface RandomAccessFileReader {
        long length() throws IOException;

        int read(byte[] bArr) throws IOException;

        void seek(long j) throws IOException;
    }

    private static abstract class AbstractRandomAccessFileReader implements RandomAccessFileReader {
        protected final RandomAccessFile file;

        AbstractRandomAccessFileReader(IFile f) throws FileNotFoundException {
            this.file = new RandomAccessFile(f.getPath(), "r");
        }

        public final void seek(long pos) throws IOException {
            this.file.seek(pos);
        }

        public final long length() throws IOException {
            return this.file.length();
        }
    }

    private static final class UnencryptedRandomAccessFileReader extends AbstractRandomAccessFileReader {
        UnencryptedRandomAccessFileReader(IFile f) throws FileNotFoundException {
            super(f);
        }

        public int read(byte[] buf) throws IOException {
            return this.file.read(buf);
        }
    }

    private static final class EncryptedRandomAccessFileReader extends AbstractRandomAccessFileReader {
        private static final SyncIntegration.ILogger LOGGER = SyncIntegration.getDelegate().getLogger(EncryptedRandomAccessFileReader.class);
        private final Cipher c;
        private final byte[] dec = new byte[16];

        EncryptedRandomAccessFileReader(Cipher c2, IFile f) throws FileNotFoundException {
            super(f);
            this.c = c2;
        }

        private void decrypt(byte[] buf) {
            try {
                decryptInLoop(buf);
            } catch (ShortBufferException e) {
                handle(e);
            } catch (IllegalBlockSizeException e2) {
                handle(e2);
            } catch (BadPaddingException e3) {
                handle(e3);
            }
        }

        private void decryptInLoop(byte[] buf) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
            for (int i = 0; i < buf.length; i += 16) {
                this.c.doFinal(buf, i, 16, this.dec);
                System.arraycopy(this.dec, 0, buf, i, 16);
            }
        }

        public int read(byte[] buf) throws IOException {
            int res = this.file.read(buf);
            decrypt(buf);
            return res;
        }

        private void handle(Exception e) {
            LOGGER.error(e.getMessage(), e);
            SyncIntegration.getDelegate().reportFailureMetric();
        }
    }

    private static final class Block {
        final long fp;
        final long hiAbookPos;
        final long hiEbookPos;
        final long length;
        final long loAbookPos;
        final long loEbookPos;

        Block(long loEbookPos2, long loAbookPos2, long hiEbookPos2, long hiAbookPos2, long fp2, long length2) {
            this.loEbookPos = loEbookPos2;
            this.loAbookPos = loAbookPos2;
            this.hiEbookPos = hiEbookPos2;
            this.hiAbookPos = hiAbookPos2;
            this.fp = fp2;
            this.length = length2;
        }
    }

    public SyncDataContent(IFile syncData, int syncDataOffset2, int blockSize2, int numSearchesBeforeBinarySearch2) throws FileNotFoundException {
        this(syncData, syncDataOffset2, blockSize2, numSearchesBeforeBinarySearch2, (RandomAccessFileReader) new UnencryptedRandomAccessFileReader(syncData));
    }

    public SyncDataContent(IFile syncData, int syncDataOffset2, int blockSize2, int numSearchesBeforeBinarySearch2, Cipher cipher) throws FileNotFoundException {
        this(syncData, syncDataOffset2, blockSize2, numSearchesBeforeBinarySearch2, (RandomAccessFileReader) new EncryptedRandomAccessFileReader(cipher, syncData));
    }

    public SyncDataContent(IFile syncData, int syncDataOffset2, int blockSize2, int numSearchesBeforeBinarySearch2, RandomAccessFileReader reader2) throws FileNotFoundException {
        this.trace = true;
        this.syncDataOffset = (long) syncDataOffset2;
        this.numSearchesBeforeBinarySearch = numSearchesBeforeBinarySearch2;
        this.reader = reader2;
        this.bp = 0;
        if (blockSize2 % 16 != 0) {
            throw new RuntimeException("blockSize must be a multiple of 16");
        }
        try {
            if (((long) blockSize2) > reader2.length()) {
                blockSize2 = (int) reader2.length();
            }
        } catch (IOException e) {
            handle(e);
        }
        this.blockSize = blockSize2 & -16;
        this.block = new byte[blockSize2];
        this.source = syncData.getPath();
    }

    /* access modifiers changed from: package-private */
    public void setErrorHandler(IErrorHandler errorHandler2) {
        this.errorHandler = errorHandler2;
    }

    /* access modifiers changed from: package-private */
    public void setTrace(boolean trace2) {
        this.trace = trace2;
    }

    /* access modifiers changed from: package-private */
    public long getLoEbookPos() {
        return this.loEbookPos;
    }

    /* access modifiers changed from: package-private */
    public long getHiEbookPos() {
        return this.hiEbookPos;
    }

    /* access modifiers changed from: package-private */
    public long getLoAudiobookPos() {
        return this.loAbookPos;
    }

    /* access modifiers changed from: package-private */
    public long getHiAudiobookPos() {
        return this.hiAbookPos;
    }

    /* access modifiers changed from: package-private */
    public void init() {
        this.blocks = createBlocks(this.blockSize);
    }

    /* access modifiers changed from: package-private */
    public String getSource() {
        return this.source;
    }

    public long getAudiobookPosFromEBookPos(long ebookPos) {
        long a1 = getAudiobookPosFromEBookPosHelper(ebookPos);
        if (a1 == -1 || ((long) this.bp) >= (curBlock().length - 16) - 1) {
            return a1;
        }
        long value = value(this.block, this.bp + 0);
        long ee1 = value(this.block, this.bp + 4);
        long es2 = value(this.block, this.bp + 16 + 0);
        long value2 = value(this.block, this.bp + 16 + 4);
        long a2 = value(this.block, this.bp + 16 + 8);
        if (ebookPos >= es2) {
            return a2;
        }
        if (ebookPos <= ee1) {
            return a1;
        }
        if (Math.abs(a1 - a2) > 1000) {
            return -1;
        }
        if (ebookPos - ee1 >= es2 - ebookPos) {
            return a2;
        }
        return a1;
    }

    public long getMinAudiobookPosFromEBookRange(long ebookStartPos, long ebookEndPos) {
        long ebookStartPosInRange = Math.max(ebookStartPos, this.loEbookPos);
        long ebookEndPosInRange = Math.min(ebookEndPos, this.hiEbookPos);

        for (long eBookPos = ebookStartPosInRange; eBookPos <= ebookEndPosInRange; eBookPos++) {
            long audiobookPosFromEBookPos = getAudiobookPosFromEBookPos(eBookPos);
            if (audiobookPosFromEBookPos != -1) {
                LOGGER.trace("Found mapped audiobook position for eBook position " + eBookPos);
                return audiobookPosFromEBookPos;
            }
        }
        return -1;
    }

    public long getMaxAudiobookPosFromEBookRange(long ebookStartPos, long ebookEndPos) {
        long ebookStartPosInRange = Math.max(ebookStartPos, this.loEbookPos);
        for (long eBookPos = Math.min(ebookEndPos, this.hiEbookPos); eBookPos >= ebookStartPosInRange; eBookPos--) {
            long audiobookPosFromEBookPos = getAudiobookPosFromEBookPos(eBookPos);
            if (audiobookPosFromEBookPos != -1) {
                return audiobookPosFromEBookPos;
            }
        }
        return -1;
    }

    public long getAudiobookPosFromEBookPosHelper(long ebookPos) {
        if (ebookPos < this.loEbookPos || ebookPos > this.hiEbookPos) {
            return -1;
        }
        try {
            long res = findClosestEbookPos(ebookPos);
            return res == -1 ? value(this.block, this.bp + 8) : res;
        } catch (IOException e) {
            handle(e);
            return -1;
        }
    }

    public long getEBookPosFromAudiobookPos(long audioBookPosMillis) {
        long e1 = getEBookPosFromAudiobookPosHelper(audioBookPosMillis);
        if (e1 == -1) {
            if (this.hiAbookPos > audioBookPosMillis || audioBookPosMillis > this.hiAbookEndPos) {
                return e1;
            }
            return this.hiEbookPos;
        } else if (((long) this.bp) >= (curBlock().length - 16) - 1) {
            return e1;
        } else {
            long ae1 = value(this.block, this.bp + 12);
            long as2 = value(this.block, this.bp + 16 + 8);
            long e2 = value(this.block, this.bp + 16 + 0);
            if (audioBookPosMillis >= as2) {
                return e2;
            }
            if (audioBookPosMillis <= ae1) {
                return e1;
            }
            if (Math.min(audioBookPosMillis - ae1, as2 - audioBookPosMillis) > 1000) {
                return -1;
            }
            if (audioBookPosMillis - ae1 >= as2 - audioBookPosMillis) {
                return e2;
            }
            return e1;
        }
    }

    private long getEBookPosFromAudiobookPosHelper(long audioBookPosMillis) {
        if (audioBookPosMillis < this.loAbookPos || audioBookPosMillis > this.hiAbookPos) {
            return -1;
        }
        try {
            long res = findClosestAbookPos(audioBookPosMillis);
            return res == -1 ? value(this.block, this.bp + 0) : res;
        } catch (IOException e) {
            handle(e);
            return -1;
        }
    }

    private void handle(Exception e) {
        LOGGER.error(e.getMessage(), e);
        SyncIntegration.getDelegate().reportFailureMetric();
    }

    private Block[] createBlocks(int blockSize2) {
        if (blockSize2 <= 0) {
            return new Block[]{new Block(0, 0, 0, 0, 0, 0)};
        }
        int numBlocks = 0;
        try {
            numBlocks = (int) (this.reader.length() / ((long) blockSize2));
            if (this.reader.length() % ((long) blockSize2) != 0) {
                numBlocks++;
            }
        } catch (IOException e) {
            handle(e);
        }
        Block[] blocks2 = new Block[numBlocks];
        byte[] buf = new byte[100];
        for (int i = 0; i < numBlocks; i++) {
            long fp = (long) (i * blockSize2);
            int len = blockSize2;
            try {
                if (this.syncDataOffset + fp + ((long) blockSize2) > this.reader.length()) {
                    len = ((int) (this.reader.length() - (this.syncDataOffset + fp))) & -16;
                }
                LOGGER.debug("createblocks len: " + len);
                LOGGER.debug("createblocks fp: " + fp);
                LOGGER.debug("createblocks reader length: " + this.reader.length());
                seek(fp);
                LOGGER.debug("seek fp: " + fp);
                this.reader.read(buf);
                //LOGGER.debug("createblocks ebookbuf: " + Arrays.toString(buf));
                LOGGER.debug("createblocks ebookbuf: " + printBuffer(buf, 0));
                long loEbookPos2 = value(buf, 0);
                long loAbookPos2 = value(buf, 8);
                long seekPosition = ((long) (len - 16)) + fp;
                seek(seekPosition);
                LOGGER.debug("seek fp: " + seekPosition);
                this.reader.read(buf);
                //LOGGER.debug("createblocks abookbuf: " + Arrays.toString(buf));
                LOGGER.debug("createblocks abookbuf: " + printBuffer(buf, 0));
                long hiEbookPos2 = value(buf, 0);
                long hiAbookPos2 = value(buf, 8);
                this.hiAbookEndPos = value(buf, 12);
                blocks2[i] = new Block(loEbookPos2, loAbookPos2, hiEbookPos2, hiAbookPos2, fp, (long) len);
            } catch (IOException e2) {
                handle(e2);
            }
        }
        this.curBlockIndex = 0;
        this.bp = 0;
        try {
            seek(0);
            this.reader.read(this.block);
        } catch (IOException e3) {
            handle(e3);
        }
        this.loEbookPos = blocks2[0].loEbookPos;
        this.hiEbookPos = blocks2[blocks2.length - 1].hiEbookPos;
        this.loAbookPos = blocks2[0].loAbookPos;
        this.hiAbookPos = blocks2[blocks2.length - 1].hiAbookPos;
        return blocks2;
    }

    private String printBuffer(byte[] buf, int shift) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i=shift; i<buf.length-4; i+=4) {
            sb.append(String.format("%7d ", value(buf, i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private long findClosestEbookPos(long val) throws IOException {
        return findClosestPos(val, curBlock().loEbookPos, curBlock().hiEbookPos, 0, this.loEbookPos, this.hiEbookPos, this.loAbookPos, this.hiAbookPos);
    }

    private long findClosestAbookPos(long val) throws IOException {
        return findClosestPos(val, curBlock().loAbookPos, curBlock().hiAbookPos, 8, this.loAbookPos, this.hiAbookPos, this.loEbookPos, this.hiEbookPos);
    }

    private Block curBlock() {
        try {
            return this.blocks[this.curBlockIndex];
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error(getClass().getSimpleName() + (": curBlockIndex=" + this.curBlockIndex + ", blocks.length=" + this.blocks.length));
            throw e;
        } catch (NullPointerException e2) {
            LOGGER.error("You have to call init() before doing anything");
            throw e2;
        }
    }

    private long findClosestPos(long val, long lo, long hi, int offset, long globalLo, long globalHi, long globalLoOther, long globalHiOther) throws IOException {
        if (val < globalLo) {
            if (!this.trace) {
                return globalLoOther;
            }
            note("Too low");
            return globalLoOther;
        } else if (val > globalHi) {
            if (this.trace) {
                note("Too high");
            }
            return globalHiOther;
        } else {
            boolean readBlockIntoMemory = false;
            if (val < lo) {
                if (this.trace) {
                    note(val + " < " + lo);
                }
                this.curBlockIndex = binarySearch(this.blocks, 0, this.curBlockIndex, val, offset);
                if (this.trace) {
                    note("curBlockIndex=" + this.curBlockIndex);
                }
                readBlock(curBlock().fp);
                readBlockIntoMemory = true;
            } else if (val > hi) {
                if (this.trace) {
                    note(val + " > " + hi);
                }
                this.curBlockIndex = binarySearch(this.blocks, this.curBlockIndex, this.blocks.length - 1, val, offset);
                if (this.trace) {
                    note("curBlockIndex=" + this.curBlockIndex);
                }
                try {
                    readBlock(curBlock().fp);
                    readBlockIntoMemory = true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    if (this.errorHandler != null) {
                        this.errorHandler.handle(e);
                    }
                    throw e;
                }
            }
            long cur = value(this.block, this.bp + offset);
            if (val == lo) {
                this.bp = 0;
            } else if (val == hi) {
                this.bp = (int) (curBlock().length - 16);
            } else if (val == cur) {
                this.bp += 0;
            } else {
                boolean found = false;
                if (!readBlockIntoMemory) {
                    if (val <= cur) {
                        int i = 0;
                        int newBp = this.bp;
                        while (!found) {
                            if (i >= this.numSearchesBeforeBinarySearch || newBp < 0) {
                                break;
                            } else if (value(this.block, newBp + offset) < val) {
                                if (this.trace) {
                                    note("Found value on #" + i + " try in linear search");
                                }
                                this.bp = newBp;
                                found = true;
                            } else {
                                i++;
                                newBp -= 16;
                            }
                        }
                    } else {
                        /*
                        LINEARY try to find the value
                         */
                        Block b = curBlock();
                        int i2 = 0;
                        int newBp2 = this.bp;

                        int stepsAllowed = 100000000;
                        int steps = 0;
                        while (!found) {
                            if (i2 >= this.numSearchesBeforeBinarySearch || ((long) newBp2) >= b.length) {
                                break;
                            } else if (value(this.block, newBp2 + offset) > val) {
                                if (this.trace) {
                                    note("Found value on #" + i2 + " try in linear search");
                                }
                                this.bp = newBp2 - 16;
                                found = true;
                            } else {
                                i2++;
                                newBp2 += 16;
                            }
                        }
//                        System.out.println("\n Steps: " + steps);
                    }
                } else {
//                    System.err.println("Block is read into the memory!. Nothing wrong with that. Just a debug message");
//                    System.exit(1);
                }
                if (!found) {
                    if (this.bp == 0 || this.bp == this.blockSize) {
                        this.bp = binarySearch(this.block, 0, (int) curBlock().length, val, offset);
                    } else if (lo <= val && val < cur) {
                        this.bp = binarySearch(this.block, 0, this.bp, val, offset);
                    } else if (cur < val && val <= hi) {
                        this.bp = binarySearch(this.block, this.bp, (int) curBlock().length, val, offset);
                    }
                }
            }
            return -1;
        }
    }

    /* access modifiers changed from: protected */
    public int binarySearch(Block[] array, int start, int end, long value, int offset) {
        long loVal;
        long hiVal;
        int low = start;
        int high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (offset == 0) {
                loVal = array[mid].loEbookPos;
                hiVal = array[mid].hiEbookPos;
            } else {
                loVal = array[mid].loAbookPos;
                hiVal = array[mid].hiAbookPos;
            }
            if (value > hiVal) {
                low = mid + 1;
            } else if (loVal <= value && value <= hiVal) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private void seek(long pos) throws IOException {
        this.reader.seek(this.syncDataOffset + pos);
    }

    private int binarySearch(byte[] array, int startIndex, int endIndex, long value, int offset) {
        int low = startIndex;
        int high = endIndex - 16;
//        System.out.println("Searching for: " + value);
        while (low <= high) {
            int mid = ((low + high) >>> 1) & -16;
            long val = value(array, mid + offset);
//            System.out.printf("%5d : %5d : %5d, %5d/%5d\n", low, mid, high, value, val);
            if (value > val) {
                low = mid + 16;
            } else if (value == val) {
                return mid;
            } else {
                high = mid - 16;
            }
        }
        int ret = low > startIndex ? low - 16 : startIndex;
//        System.out.println("Found?? : " + ret);
        return ret;
    }

    private int readBlock(long fp) throws IOException {
        int read;
        if (this.trace) {
            long start = System.currentTimeMillis();
            seek(fp);
            read = this.reader.read(this.block);
            note("Block read " + read + " bytes in " + (System.currentTimeMillis() - start) + " ms");
        } else {
            seek(fp);
            read = this.reader.read(this.block);
        }
        this.bp = 0;
        return read;
    }

    static long value(byte[] buf, int pos) {
        if (pos + 3 >= buf.length) {
            return -1;
        }
        return (long) (((buf[pos + 3] & 255) << 24) | ((buf[pos + 2] & 255) << 16) | ((buf[pos + 1] & 255) << 8) | (buf[pos + 0] & 255));
    }

    private void note(String msg) {
        LOGGER.trace(msg);
    }
}
