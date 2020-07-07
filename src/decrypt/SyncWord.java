package decrypt;

class SyncWord {

    long audioStart;
    long audioEnd;

    // word start/end relative to the provided text
    int wordStart;
    int wordEnd;

    // word start/end values read from the sync file.
    // they are just here mostly for debugging
    public long ebookWordStart;
    public long ebookWordEnd;

    String word;
    boolean wordOk;

    public void cutWordFromText(BookText bookText) {
        word = bookText.cutWordFromText(this);
        wordOk = isWordOk(word);
    }

    private static String getms(long mslong) {
        return String.format("%05d.%03d", mslong/1000, mslong%1000);
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s", getms(audioStart), getms(audioEnd), word);
//        return String.format("%s,%s,%d,%d,%s", getms(audioStart), getms(audioEnd), ebookWordStart, ebookWordEnd, word);
    }


    static boolean isWordOk(String a) {
        if(a.length() == 0) return true;

        char lastChar = a.charAt(a.length()-1);
        return lastChar == ' ';
    }

    public SyncWord clone() {
        SyncWord sw = new SyncWord();
        sw.wordEnd = wordEnd;
        sw.wordStart = wordStart;
        sw.audioEnd = audioEnd;
        sw.audioStart = audioStart;
        sw.word = word + "";
        sw.wordOk = wordOk;

        return sw;
    }

}