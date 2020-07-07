package sync;

public interface ISyncData {

    public interface Header {
        int getHeaderLength();
    }

    long getEBookPosFromAudiobookPos(long j);

    long getHiAudiobookPos();

    long getHiEbookPos();

    long getLoAudiobookPos();

    long getLoEbookPos();

    long getMaxAudiobookPosFromEBookRange(long j, long j2);

    long getMinAudiobookPosFromEBookRange(long j, long j2);

    String getSource();

    void init();
}
