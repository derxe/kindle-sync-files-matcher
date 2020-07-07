package decrypt;

import sync.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Decrypt {

    public static void printSyncDataInfo(SyncData data) {
        System.out.println(data.getSource());
        System.out.println("Lo ebook pos: " + data.getLoEbookPos());
        System.out.println("Hi ebook pos: " + data.getHiEbookPos());

        System.out.println("Lo abook pos: " + data.getLoAudiobookPos());
        System.out.println("Hi abook pos: " + data.getHiAudiobookPos() + " 27276000");
    }

    // Ebook position is in character count from beginning
    // abook position is in ms from the beginning of audio file including the "welcome to the audible ... "
    public static void main(String[] args) throws FileNotFoundException {
        SyncIntegration.setDelegate(new MyHushpuppySyncIntegration());

        //System.out.println(Arrays.toString(args));
        String bookPath = "/home/anze/Documents/Sola1920S/ONJ/sync_files_and_books/Wenn_die_Zeit_gekommen_ist";
        if(args.length > 0) {
            //System.out.println("Reading from args " + args[0]);
            bookPath = args[0];
        }
        File bookDirectory = new File(bookPath);

        // load sync and txt file
        File syncFile = Utils.getFileWithEnding(bookDirectory, ".sync");
        File txtFile = Utils.getFileWithEnding(bookDirectory, "calibre.txt");
//        File txtFile = Utils.getFileWithEnding(bookDirectory, ".txt.fixed");
        if(syncFile == null || txtFile == null) return;
        System.out.println("Sync file: " + syncFile);
        System.out.println("Txt  file: " + txtFile);
        System.out.println();


        ThreadSafeReadAlongSyncDataProvider syncDataProvider = new ThreadSafeReadAlongSyncDataProvider();
        SyncData data = (SyncData) syncDataProvider.create(syncFile);

        //printDiffs(data, 30);
        //SyncDataContent dataContent = data.content;
        printSyncDataInfo(data);

        BookText bookTxt = new BookText(Utils.readFile(txtFile));
        bookTxt.removeNewLines();


        System.out.println("Txt length:" + bookTxt.txt.length());

        String bookStartText = Utils.readFile(bookDirectory, "start.txt").replace("\n", "");
        System.out.println("Start of the book text '" + bookStartText + "'");
        if(bookStartText.equals("")) {
            System.err.println("Unable to get the start of the book from 'start.txt' file");
            System.out.println("Some of the first diffs so you can locate the start of the book:");
            printDiffs(data, 40);
            System.exit(1);
        }
        int textStartPosition = bookTxt.txt.indexOf(bookStartText);
        if(textStartPosition < 0) {
            System.err.println("Unable to find this start of the book");
            System.exit(0);
        }
        System.out.printf("Start position %d, Text:'%s'\n", textStartPosition, bookTxt.txt.substring(textStartPosition, textStartPosition + 20));

        ArrayList<SyncWord> syncWords = new ArrayList<>();

        long audioPos = data.getLoAudiobookPos();
        long audioPosOld = audioPos;
        long undefinedAudioPos = -1;

        long ebookPosOld = data.getLoEbookPos();
        int textPosition = textStartPosition; // text position relative to the txtBook it has "nothing" to do with eBookPos

        /*System.out.println(data.getEBookPosFromAudiobookPos(48088));
        System.out.println(data.getEBookPosFromAudiobookPos(48089));
        System.out.println(data.getEBookPosFromAudiobookPos(48090));

        System.exit(0);
        */

        while(textPosition < bookTxt.txt.length() && audioPos < data.getHiAudiobookPos() /* && audioPos < 58669*/) {
            long ebookPos = data.getEBookPosFromAudiobookPos(audioPos);

            if(ebookPos == -1) {
                if(undefinedAudioPos == -1) {
                    undefinedAudioPos = audioPos;
                }
                audioPos++;
                continue;
            } else {
                if(undefinedAudioPos != -1) {
//                    System.out.println("#################################### -1 from:" + undefinedAudioPos + " to:" + (audioPos-1));
                    undefinedAudioPos = -1;
                }
            }

            //System.out.printf("a:%d e:%d\n", audioPos, ebookpos);
            if(ebookPosOld != ebookPos) {
                // diff: how many characters have we read
                int diff = (int) (ebookPos-ebookPosOld);

                SyncWord syncWord = new SyncWord();
                syncWord.audioStart = audioPosOld;
                syncWord.audioEnd = audioPos;
                syncWord.ebookWordStart = ebookPosOld;
                syncWord.ebookWordEnd = ebookPos;
                syncWord.wordStart = textPosition;
                syncWord.wordEnd = textPosition+diff;
                syncWord.cutWordFromText(bookTxt);
                syncWords.add(syncWord);

//                if(diff > 30)
//                    System.out.printf("%10d %10d, %d, %s\n", ebookPos, audioPos, diff, syncWord);

                int MAX_WRONG_WORDS = 20;
                int BACK_CHECK_LENGTH = 25; // basically if more then 8 words from the last 12 words are wrong we need to fix that
                int numberOfWrongWords = 0;
                for(int i=Math.max(0, syncWords.size()-BACK_CHECK_LENGTH); i<syncWords.size(); i++) {
                    if(!syncWords.get(i).wordOk) numberOfWrongWords ++;
                }

                if(numberOfWrongWords >= MAX_WRONG_WORDS) {
                    int start = Math.max(0, syncWords.size()-BACK_CHECK_LENGTH);
//                    for(int i=start; i<syncWords.size(); i++) {
//                        System.out.println("," + syncWords.get(i).word + "," + (syncWords.get(i).wordOk? "" : "###########"));
//                    }
//                    System.out.printf("Too many wrong words detected(%d) going back to fix it\n", numberOfWrongWords);

//                    if(ebookPos > 125600) {
//                        System.out.println("NOW!!");
//                        BookText.printDebug = true;
//                    }
                    int lastWordOkIndex = bookTxt.tryFixing(syncWords, start, syncWords.size()-1, MAX_WRONG_WORDS);
                    if(lastWordOkIndex == -1) {
                        System.err.println("Unable to fix the word shift. Aborting");
                        System.exit(1);
                    }
                    SyncWord lastWordOk = syncWords.get(lastWordOkIndex);

                    // remove all the wrong words and try to match them again
                    // set the audio position to the values that were before the errors begun
                    audioPos = lastWordOk.audioEnd;
                    audioPosOld = lastWordOk.audioStart;
                    textPosition = lastWordOk.wordStart;
                    ebookPosOld = data.getEBookPosFromAudiobookPos(audioPos) - lastWordOk.word.length();

                    syncWords.subList(lastWordOkIndex, syncWords.size()).clear();
                    continue;
                }

                textPosition += diff;
                audioPosOld = audioPos;
            }

            ebookPosOld = ebookPos;
            audioPos ++;
        }

//        System.out.println("Text position: " + textPosition);
//        System.out.println("Audio position: " + audioPos);
//        for(int i=Math.max(0, syncWords.size()-50); i<syncWords.size(); i++) {
//            System.out.println("," + syncWords.get(i).word + "," + (syncWords.get(i).wordOk? "" : "###########"));
//        }

        bookTxt.txt = bookTxt.getTxtShifted();
        bookTxt.addBackNewLines();
        Utils.writeFile(txtFile.getPath()+".fixed", bookTxt.txt);


        StringBuilder out = new StringBuilder();
        for(SyncWord sw : syncWords) {
            out.append(sw).append("\n");
        }
        Utils.writeFile(txtFile.getPath() + ".csvsync", out.toString());

        System.out.println("Ova");
    }

    private static void printDiffs(SyncData data, int numDiffs) {
        long audioPos = data.getLoAudiobookPos();
        long audioPosOld = data.getLoAudiobookPos();

        long ebookPosOld = data.getLoEbookPos();

        int diffsFound = 0;
        while(diffsFound < numDiffs) {
            long ebookPos = data.getEBookPosFromAudiobookPos(audioPos);

            if(ebookPos != -1 && ebookPosOld != ebookPos) {
                // diff: how many characters have we read
                int diff = (int) (ebookPos-ebookPosOld);
                System.out.print(diff + " ");

                diffsFound ++;
                ebookPosOld = ebookPos;
            }

            audioPos ++;
        }
    }


}

