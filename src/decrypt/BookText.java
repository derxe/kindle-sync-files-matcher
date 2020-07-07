package decrypt;

import java.util.ArrayList;

class BookText {

    static class TxtShift {
        int position; // where the shift should be applied
        int shift; // how much to shift

        public TxtShift(int start, int shift){
            this.position = start;
            this.shift = shift;
        }

        @Override
        public String toString() {
            return String.format("position:%d, shift:%d", position, shift);
        }
    }

    String txt = "";

    ArrayList<Integer> newLines = new ArrayList<>();
    public ArrayList<TxtShift> txtShifts = new ArrayList<>();

    public BookText(String txt) {
        this.txt = txt;
    }

    /**
     * Remove all the new lines from the book txt and svae their positions.
     */
    public void removeNewLines() {
        int index = 0;
        int prevIndex = 0;
        StringBuilder sbTxt = new StringBuilder();
        while((index = txt.indexOf("\n", index)) >= 0) {
            newLines.add(index);
            sbTxt.append(txt, prevIndex, index);
            index ++;
            prevIndex = index;
        }
        sbTxt.append(txt, prevIndex, txt.length());

        this.txt = sbTxt.toString();
    }

    /**
     * Insert back all the new lines that were removed with removeNewLines function
     */
    public void addBackNewLines() {
        StringBuilder sbTxt = new StringBuilder();
        try {
            int prevSpace = 0;
            int prevShift = 0;
            int nFilled = 0;
            for (Integer space : newLines) {
                int shift = getShift(space - nFilled);
                //System.out.println(txt.length());
                if(space - nFilled - shift > txt.length()) {
                    txt = sbTxt.toString();
                    System.err.println((space - nFilled - shift) + ">=" + txt.length());
                    return;
                }
                sbTxt.append(txt, prevSpace - nFilled - prevShift, space - nFilled - shift).append("\n");
                nFilled += 1;
                prevSpace = space + 1;
                prevShift = shift;
            }
            sbTxt.append(txt, prevSpace - nFilled - prevShift, txt.length());
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        txt = sbTxt.toString();
    }

    /**
     * Apply all the shifts to the text and return corrected text.
     * @return
     */
    public String getTxtShifted() {
        StringBuilder txtFixed = new StringBuilder();
        int lastPosition = 0;
        for(TxtShift txtShift : txtShifts) {
            int position = txtShift.position + getShift(txtShift.position);
            int shift = txtShift.shift;
            String cutText1 = txt.substring(position-20, position);
            String cutText2 = txt.substring(position, position+20);

            if(shift > 0) {
                if (lastPosition <= position-shift) {
                    txtFixed.append(txt, lastPosition, position - shift);
                    lastPosition = position;
                } else {
                    return txtFixed.toString();
                }
            } else if (shift < 0){
                String filler = Utils.repeat(-shift, "#");
                if(lastPosition <= position) {
                    txtFixed.append(txt, lastPosition, position);
                    lastPosition = position;
                }
                txtFixed.append(filler);
            } else {
                throw new IllegalStateException("Shift = " + txtShift.shift);
            }


            System.out.println("Shift: " + txtShift + " '" + cutText1 + "'" + cutText2 + "'");
        }
        txtFixed.append(txt, lastPosition, txt.length());

        return txtFixed.toString();
    }

    /**
     * return how much do we have to shift so that the position is in the right place.
     */
    public int getShift(int position) {
        int shift = 0;
        // just sum up all the shifts before position
        for (TxtShift txtShift : txtShifts) {
            if (position < txtShift.position) break;
            shift += txtShift.shift;
        }
        return shift;
    }

    public String cutWordFromText(SyncWord syncWord) {
        return cutWordFromText(syncWord.wordStart, syncWord.wordEnd);
    }

    public String cutWordFromText(int start, int end) {
        int shiftStart = getShift(start);
        int shiftEnd = getShift(end);

        int beginIndex = shiftStart + start;
        int endIndex = shiftEnd + end;

        if(beginIndex > endIndex) return "";
        try {
            return txt.substring(beginIndex, endIndex);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.printf("shiftStart: %d, shiftEnd: %d, start:%d, end:%d, substring(%d,%d)", shiftStart, shiftEnd, start, end, beginIndex, endIndex);
        }
        System.exit(2);
        return "";
    }

    static boolean printDebug = false;

    /**
     * Try inserting shift to the words so that they line properly line up.
     * When the shift is found it is automatically added to the txtShifts array.
     *
     * @param syncWords all the syncWords list until now
     * @param start, the start and the end of the words from syncWords list taht wer are fixing
     * @param end: INCLUSIVE
     * @param maxWordsWrong when fixing how many words can be wrong in order for this to be a good fix
     * @return returns lastOkWordIndex. Form this word on the errors begun
     */
    public int tryFixing(ArrayList<SyncWord> syncWords, int start, int end, int maxWordsWrong) {

        int bestScore = -1;
        TxtShift bestTxtShift = null;
        int bestLastWordOkIndex = -1;

        for(int lastWordOkIndex=start; lastWordOkIndex <= end; lastWordOkIndex++) {
            SyncWord lastWordOk = syncWords.get(lastWordOkIndex);
            if(printDebug) {
                System.out.printf("Trying word on index: %d: %s\n", lastWordOkIndex, lastWordOk.word);

                for(int i=Math.max(start, lastWordOkIndex-3); i<=Math.min(lastWordOkIndex+3, end); i++) {
                    System.out.printf(",%s, %s%s\n", syncWords.get(i).word, (i==lastWordOkIndex? "XX":""), (syncWords.get(i).wordOk? "":"#####"));
                }
            }

            // try different spacings from -2 too 5;
            for (int shift = -3; shift < 4; shift++) {
                // TODO find the best position to put shift in. Now it's just inserted at the word end.
                TxtShift txtShift = new TxtShift(syncWords.get(lastWordOkIndex).wordEnd - 1, shift);
                txtShifts.add(txtShift);

                if(printDebug) System.out.println("Testing shift: " + shift);
                int wordsOk = 0;
                int wordsWorng = 0;
                for (int i = start; i <= end; i++) {
                    SyncWord syncWord = syncWords.get(i);
                    String newWord = cutWordFromText(syncWord);
                    if (newWord.length() == 0) continue;
                    boolean newWordOk = SyncWord.isWordOk(newWord);
                    if(newWordOk) wordsOk++;
                    if(!newWordOk) wordsWorng++;
                    if(printDebug) System.out.printf(",%s, %s\n", newWord, (newWordOk? "":"#####"));
                }

                int score = wordsWorng >= maxWordsWrong? -1 : wordsOk;

                if(printDebug) System.out.println("Final score: " + score);
                if(printDebug) System.out.println();

                txtShifts.remove(txtShift);

                if (score > bestScore) {
                    bestScore = score;
                    bestTxtShift = txtShift;
                    bestLastWordOkIndex = lastWordOkIndex;
                }
            }
        }

        String word = bestLastWordOkIndex == -1? "null" : syncWords.get(bestLastWordOkIndex).word;
        System.out.printf("saving fix with score:%d, index:%d, word:%s, fix: %s\n", bestScore, bestLastWordOkIndex, word, bestTxtShift);
        txtShifts.add(bestTxtShift);
        return bestLastWordOkIndex;
    }

}

