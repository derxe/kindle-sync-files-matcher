package sync;

import sync.SyncIntegration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ThreadSafeReadAlongSyncDataProvider {
    private final SyncIntegration.ILogger logger = SyncIntegration.getDelegate().getLogger(getClass());

    public synchronized ISyncData create(File syncFile) {
        ISyncData syncData;
        if (syncFile == null) {
            throw new IllegalArgumentException("The sync file must not be null");
        }
        try {
            syncData = new SimpleSyncDataBuilder(new SyncFileWrapper(syncFile)).create();
            syncData.init();
        } catch (Exception e) {
            this.logger.error("cannot create a SimpleSyncDataBuilder", e);
            syncData = null;
        }
        return syncData;
    }

    private static final class SyncFileWrapper implements IFile {
        private final File syncFile;

        public SyncFileWrapper(File syncFile2) {
            this.syncFile = syncFile2;
        }

        public String getPath() {
            return this.syncFile.getPath();
        }

        public InputStream open() throws IOException {
            return new FileInputStream(this.syncFile);
        }
    }
}
