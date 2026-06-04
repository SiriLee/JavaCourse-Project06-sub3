package com.example.sequence;

public interface SeqIterator {
    boolean hasNext();

    SequenceItem next();

    void remove();
}
