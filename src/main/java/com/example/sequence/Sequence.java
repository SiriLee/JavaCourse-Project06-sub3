package com.example.sequence;

public interface Sequence {
    void add(SequenceItem item);

    SequenceItem get(int index);

    void remove(int index);

    boolean contains(SequenceItem item);

    int size();

    boolean isEmpty();

    SeqIterator iterator();

    SeqIterator reverseIterator();

    SeqIterator biIterator();

    SequenceItem[] toArray();

    @Override
    boolean equals(Object obj);

    @Override
    String toString();
}
