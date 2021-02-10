# Tunnista

This software is published just to provide the precise description of how the classification methods of simple scoring, sum of relative frequencies and the product of relative frequencies were implemented.

The first argument to the program includes the training texts (one per line) followed by tab and the language code for the line. The second argument is similarly formatted test file. By modifying the code, you can make it use one of the three classifiers and print out different measures of the performance of this classifier. It will also automatically go over ranges of different n-gram models and penalty modifiers. The results have to be verified manually.

In order to compile and run the software, you need the guava library (23.0 was used here, but other versions should work too).
