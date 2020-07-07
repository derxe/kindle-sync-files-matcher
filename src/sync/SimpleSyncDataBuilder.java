package sync;

import sync.ISyncData;
import sync.SyncData;
import java.io.IOException;
import javax.crypto.Cipher;

public final class SimpleSyncDataBuilder {
    private AES aes = new AES();
    private int blockSize = 64000;
    private Cipher cipher;
    private int numSearchesBeforeBinarySearch = 3;
    private IFile syncFile;
    private boolean trace = true;
    private boolean useHeader;

    public SimpleSyncDataBuilder(IFile syncFile2) {
        this.syncFile = syncFile2;
    }

    public ISyncData create() throws IOException, SyncDataHeaderException {
        final ISyncData.Header header;
        int syncDataOffset;
        SyncDataContent content;
        if (this.syncFile == null) {
            throw new NullPointerException("You must set the 'syncFile' attribute by calling syncFile(File)");
        }
        if (this.useHeader) {
            header = new SyncDataHeader(this.syncFile);
            syncDataOffset = header.getHeaderLength();
        } else {
            header = null;
            syncDataOffset = 0;
        }
        int numSearches = this.numSearchesBeforeBinarySearch;
        if (this.cipher != null) {
            content = new SyncDataContent(this.syncFile, syncDataOffset, this.blockSize, numSearches, this.cipher);
        } else {
            content = new SyncDataContent(this.syncFile, syncDataOffset, this.blockSize, numSearches);
        }
        content.setTrace(this.trace);
        return new SyncData(new SyncData.HasHeader() {
        }, content);
    }
}
