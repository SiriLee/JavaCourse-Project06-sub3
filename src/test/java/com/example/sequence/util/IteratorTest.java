package com.example.sequence.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sequence.SequenceItem;
import com.example.sequence.impl.ArraySequence;
import com.example.sequence.impl.LinkedSequence;

public class IteratorTest {
    private ArraySequence arraySequence;
    private LinkedSequence linkedSequence;
    private final String[] testData = {"A", "B", "C", "D", "E"};

    public IteratorTest() {
        arraySequence = new ArraySequence();
        linkedSequence = new LinkedSequence();
        for (String s : testData) {
            SequenceItem item = new SequenceItem(s);
            arraySequence.add(item);
            linkedSequence.add(item);
        }
    }

    @Test
    public void testCountContains() {
        int countA = SequenceUtils.countContains(arraySequence.iterator(), "A");
        int countB = SequenceUtils.countContains(linkedSequence.iterator(), "B");
        int countX = SequenceUtils.countContains(arraySequence.iterator(), "X");
        assertEquals(1, countA);
        assertEquals(1, countB);
        assertEquals(0, countX);
    }

    @Test
    public void testFindLast() {
        int indexC = SequenceUtils.findLast(arraySequence.biIterator(), "C");
        int indexD = SequenceUtils.findLast(linkedSequence.biIterator(), "D");
        int indexX = SequenceUtils.findLast(arraySequence.biIterator(), "X");
        assertEquals(2, indexC);
        assertEquals(3, indexD);
        assertEquals(-1, indexX);
    }

    @Test
    public void testCompare() {
        assertTrue(SequenceUtils.compare(arraySequence.iterator(), linkedSequence.iterator()));
    }
}
