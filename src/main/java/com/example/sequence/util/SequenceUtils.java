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

    public static SeqBiIterator findLast(SeqBiIterator it, String target) {
        while (it.hasNext()) {
            it.next();
        }
        while (it.hasPrevious()) {
            SequenceItem item = it.previous();
            if (item.getData().equals(target)) {
                return it; // Return the iterator at the found item
            }
        }
        return null; // Return null if the item is not found
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
