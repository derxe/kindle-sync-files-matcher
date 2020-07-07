Files inside this folder:

* *.calibre.txt whole book that was obtained by calibre book to txt alforithm
* *.csvsync each word synced acording to the sync file
* *.fixed file that has the same spaces between words as the ".sync". expect it to have. The problem here was that calibre produced txt file did not exactly match the sync file. For some words had to many spaces in front of them (for example at the end of the chapter) and so it didn't match the sync file. Snyc file said "nex 5 characters are timed as such" but  the calibre had 6 characters for word "" so this had to be fixed. Compare .txt and .fixed file to see the differance.   
* start.txt start of the book so that the matcher knows where to start matching. At the beginning of the books we have some "noise" that we want to skip
* *.sync sync file downloaded from kindle app 
