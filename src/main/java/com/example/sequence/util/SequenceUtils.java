package com.example.sequence.util;

import com.example.sequence.Sequence;
import com.example.sequence.SequenceItem;
import com.example.sequence.SeqIterator;
import com.example.sequence.SeqBiIterator;

public class SequenceUtils {
    public static int countContains(SeqIterator it, String target) {
        int count = 0;
        while (it.hasNext()) {
            SequenceItem item = it.next();
            if (item.getData().equals(target)) {
                count++;
            }
        }
        return count;
    }

    public static int findLast(SeqBiIterator it, String target) {
        int index = -1;
        int lastMatch = -1;
        while (it.hasNext()) {
            SequenceItem item = it.next();
            index++;
            if (item.getData().equals(target)) {
                lastMatch = index;
            }
        }
        return lastMatch;
    }

    public static boolean compare(SeqIterator it1, SeqIterator it2) {
        while (it1.hasNext() && it2.hasNext()) {
            SequenceItem item1 = it1.next();
            SequenceItem item2 = it2.next();
            if (!item1.getData().equals(item2.getData())) {
                return false;
            }
        }
        return !it1.hasNext() && !it2.hasNext();
    }
}
