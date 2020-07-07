# What it does / or it should do ...

It tires to read .snyc from kindle/auidible and match it to the book text. 

## Some quick info about the code

**before I forget everything**

* **snyc** "library"was obtained by decompiling the kindle app. I am not 100% if it works correctly 
because if I remember correctly I managed to find some mistakes inside. 



## What data is written in sync file

As far as I remember the sync file only saves the timestamps at the beginning of the words. 
The time is written in ms (whole number) from the beginning of the audio file. If you download audio
file from audible (.aax) the timestamps will match quite nicely. 

The words in sync file are written as number of character each timestamp represents. 

For example from sync file we just get this:

* 12345 81323
* 12348 83531
* 12353 89452
* 12360 94323

Meaning that character at position 12345 has the timestamp of "81323 -> 81 seconds and 323 milliseconds"
So the text for the following charcter position could look something like this: 

"in some things "

Matched:

* 12345 81323: 3 "in "
* 12348 83531: 5 "some "
* 12353 89452: 7 "things "
* 12360 94323


The problem as you can see was that you needed a character perfect matching of the text in order for the text to allign properly. 
Calibre's ebook to text gave decent enough results in order for the problem to work. Some small differences were corrected by 
*tryFixing* function inside SnycWord class. 
