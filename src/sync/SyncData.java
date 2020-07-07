package sync;

import sync.SyncDataContent;
import sync.SyncIntegration;

public final class SyncData implements ISyncData {
    public final SyncDataContent content;
    private final HasHeader hasHeader;
    /* access modifiers changed from: private */
    public final SyncIntegration.ILogger logger = SyncIntegration.getDelegate().getLogger(getClass());

    interface HasHeader {
    }

    SyncData(HasHeader hasHeader2, SyncDataContent content2) {
        this.hasHeader = hasHeader2;
        this.content = content2;
        this.content.setErrorHandler(new SyncDataContent.IErrorHandler() {
            public void handle(Throwable e) {
                SyncData.this.logger.error(SyncData.this.toString());
            }
        });
    }

    public String getSource() {
        return this.content.getSource();
    }

    public void init() {
        this.content.init();
    }

    public synchronized long getMinAudiobookPosFromEBookRange(long ebookStartPos, long ebookEndPos) {
        return this.content.getMinAudiobookPosFromEBookRange(ebookStartPos, ebookEndPos);
    }

    public synchronized long getMaxAudiobookPosFromEBookRange(long ebookStartPos, long ebookEndPos) {
        return this.content.getMaxAudiobookPosFromEBookRange(ebookStartPos, ebookEndPos);
    }

    public synchronized long getEBookPosFromAudiobookPos(long audioBookPosMillis) {
        return this.content.getEBookPosFromAudiobookPos(audioBookPosMillis);
    }

    public synchronized long getLoEbookPos() {
        return this.content.getLoEbookPos();
    }

    public synchronized long getHiEbookPos() {
        return this.content.getHiEbookPos();
    }

    public synchronized long getLoAudiobookPos() {
        return this.content.getLoAudiobookPos();
    }

    public synchronized long getHiAudiobookPos() {
        return this.content.getHiAudiobookPos();
    }
}
