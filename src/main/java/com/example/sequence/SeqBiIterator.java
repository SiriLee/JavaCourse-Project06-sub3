package com.example.sequence;

public interface SeqBiIterator extends SeqIterator {
    boolean hasPrevious();

    SequenceItem previous();
}
