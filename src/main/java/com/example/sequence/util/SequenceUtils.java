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

    public static boolean compare(Sequence seq1, Sequence seq2) {
        if (seq1.size() != seq2.size()) {
            return false;
        }
        SeqIterator it1 = seq1.iterator();
        SeqIterator it2 = seq2.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            if (!it1.next().equals(it2.next())) {
                return false;
            }
        }
        if (it1.hasNext() || it2.hasNext()) {
            return false; // One of the sequences has extra items
        }
        return true;
    }
}
