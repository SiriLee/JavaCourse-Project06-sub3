package com.example.sequence.impl;

import com.example.sequence.SeqBiIterator;
import com.example.sequence.SeqIterator;
import com.example.sequence.Sequence;
import com.example.sequence.SequenceItem;

public class ArraySequence implements Sequence {
    private SequenceItem[] items;
    private int size;
    private int capacity;

    public ArraySequence() {
        this.capacity = 10;
        this.items = new SequenceItem[capacity];
        this.size = 0;
    }

    private void ensureCapacity() {
        if (size >= capacity) {
            capacity *= 2;
        } else if (size < capacity / 4) {
            capacity /= 2;
        } else {
            return;
        }
        SequenceItem[] newItems = new SequenceItem[capacity];
        System.arraycopy(items, 0, newItems, 0, size);
        items = newItems;
    }

    @Override
    public void add(SequenceItem item) {
        ensureCapacity();
        items[size++] = item;
    }

    @Override
    public SequenceItem get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return items[index];
    }

    @Override
    public void remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        System.arraycopy(items, index + 1, items, index, size - index - 1);
        items[--size] = null; // Help GC
        ensureCapacity();
    }

    @Override
    public boolean contains(SequenceItem item) {
        for (int i = 0; i < size; i++) {
            if (items[i].equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public SeqIterator iterator() {
        return new ForwardIterator();
    }

    @Override
    public SeqIterator reverseIterator() {
        return new ReverseIterator();
    }

    @Override
    public SeqBiIterator biIterator() {
        return new BidirectionalIterator();
    }

    @Override
    public SequenceItem[] toArray() {
        SequenceItem[] array = new SequenceItem[size];
        System.arraycopy(items, 0, array, 0, size);
        return array;
    }

    // ---------- inner iterator classes ----------

    private class ForwardIterator implements SeqIterator {
        private int cursor = 0;
        private int lastRet = -1;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public SequenceItem next() {
            if (!hasNext()) {
                throw new IndexOutOfBoundsException("No more elements");
            }
            lastRet = cursor;
            return items[cursor++];
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException("next() has not been called");
            }
            ArraySequence.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
        }
    }

    private class ReverseIterator implements SeqIterator {
        private int cursor = size - 1;
        private int lastRet = -1;

        @Override
        public boolean hasNext() {
            return cursor >= 0;
        }

        @Override
        public SequenceItem next() {
            if (!hasNext()) {
                throw new IndexOutOfBoundsException("No more elements");
            }
            lastRet = cursor;
            return items[cursor--];
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException("next() has not been called");
            }
            ArraySequence.this.remove(lastRet);
            cursor = lastRet - 1;
            lastRet = -1;
        }
    }

    private class BidirectionalIterator implements SeqBiIterator {
        private int cursor = 0;
        private int lastRet = -1;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public SequenceItem next() {
            if (!hasNext()) {
                throw new IndexOutOfBoundsException("No more elements");
            }
            lastRet = cursor;
            return items[cursor++];
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public SequenceItem previous() {
            if (!hasPrevious()) {
                throw new IndexOutOfBoundsException("No previous elements");
            }
            lastRet = cursor - 1;
            return items[--cursor];
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException("next() or previous() has not been called");
            }
            ArraySequence.this.remove(lastRet);
            if (lastRet < cursor) {
                cursor--;
            }
            lastRet = -1;
        }
    }

}
