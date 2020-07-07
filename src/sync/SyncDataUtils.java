package sync;

public final class SyncDataUtils {

    public enum RangeMembership {
        BEFORE,
        WITHIN,
        AFTER,
        INVALID
    }

    public static int getMaxEBookPositionWithSyncedAudio(ISyncData syncData, int maxDurationAudioPosition) {
        int mappedHiEBookPosition;
        if (syncData == null) {
            throw new IllegalArgumentException("syncData is null!");
        }
        int hiEBookPosition = (int) syncData.getHiEbookPos();
        if (maxDurationAudioPosition <= 0 || (mappedHiEBookPosition = (int) syncData.getEBookPosFromAudiobookPos((long) maxDurationAudioPosition)) < 0) {
            return hiEBookPosition;
        }
        return Math.min(mappedHiEBookPosition, hiEBookPosition);
    }

    public static int getEBookPositionFromAudioPosition(ISyncData syncData, int audioPosition, int maxDurationAudioPosition) {
        if (syncData == null) {
            throw new IllegalArgumentException("syncData is null!");
        } else if (audioPosition <= 0 || audioPosition != maxDurationAudioPosition) {
            return (int) syncData.getEBookPosFromAudiobookPos((long) audioPosition);
        } else {
            return getMaxEBookPositionWithSyncedAudio(syncData, maxDurationAudioPosition);
        }
    }

    public static RangeMembership getRangeMembership(ISyncData syncData, int audioPosition) {
        if (syncData == null) {
            throw new IllegalArgumentException("syncData is null!");
        }
        long loAbookPos = syncData.getLoAudiobookPos();
        long hiAbookPos = syncData.getHiAudiobookPos();
        if (loAbookPos < 0) {
            throw new IllegalArgumentException("syncData low audiobook position is invalid: " + loAbookPos);
        } else if (hiAbookPos < 0) {
            throw new IllegalArgumentException("syncData hi audiobook position is invalid: " + hiAbookPos);
        } else if (((long) audioPosition) < loAbookPos) {
            return RangeMembership.BEFORE;
        } else {
            if (((long) audioPosition) <= hiAbookPos) {
                return RangeMembership.WITHIN;
            }
            return RangeMembership.AFTER;
        }
    }

    public static int getMinAudiobookPosFromEBookRange(ISyncData syncData, int ebookStartPos, int ebookEndPos) {
        if (syncData != null) {
            return (int) syncData.getMinAudiobookPosFromEBookRange((long) ebookStartPos, (long) ebookEndPos);
        }
        throw new IllegalArgumentException("syncData is null!");
    }

    public static int getMaxAudiobookPosFromEBookRange(ISyncData syncData, int ebookStartPos, int ebookEndPos) {
        if (syncData != null) {
            return (int) syncData.getMaxAudiobookPosFromEBookRange((long) ebookStartPos, (long) ebookEndPos);
        }
        throw new IllegalArgumentException("syncData is null!");
    }
}
