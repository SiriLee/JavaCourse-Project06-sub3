package com.example.sequence.impl;

import com.example.sequence.Sequence;
import com.example.sequence.SequenceItem;
import com.example.sequence.SeqIterator;
import com.example.sequence.SeqBiIterator;

public class LinkedSequence implements Sequence {
    private Node head;
    private Node tail;
    private int size;

    private static class Node {
        SequenceItem item;
        Node next;
        Node prev;

        Node(SequenceItem item) {
            this.item = item;
        }
    }

    public LinkedSequence() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public void add(SequenceItem item) {
        Node newNode = new Node(item);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
    }

    @Override
    public SequenceItem get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.item;
    }

    @Override
    public void remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        if (current.prev != null) {
            current.prev.next = current.next;
        } else {
            head = current.next; // Removing the head
        }
        if (current.next != null) {
            current.next.prev = current.prev;
        } else {
            tail = current.prev; // Removing the tail
        }
        size--;
    }

    @Override
    public boolean contains(SequenceItem item) {
        Node current = head;
        while (current != null) {
            if (current.item.equals(item)) {
                return true;
            }
            current = current.next;
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
        return new LinkedIterator();
    }

    @Override
    public SeqIterator reverseIterator() {
        return new LinkedReverseIterator();
    }

    @Override
    public SeqBiIterator biIterator() {
        return new LinkedBiIterator();
    }

    @Override
    public SequenceItem[] toArray() {
        SequenceItem[] array = new SequenceItem[size];
        Node current = head;
        int index = 0;
        while (current != null) {
            array[index++] = current.item;
            current = current.next;
        }
        return array;
    }

    // ---------- inner iterator classes ----------

    private class LinkedIterator implements SeqIterator {
        private Node current = head;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public SequenceItem next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements");
            }
            SequenceItem item = current.item;
            current = current.next;
            return item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported in LinkedIterator");
        }
    }

    private class LinkedReverseIterator implements SeqIterator {
        private Node current = tail;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public SequenceItem next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements");
            }
            SequenceItem item = current.item;
            current = current.prev;
            return item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported in LinkedReverseIterator");
        }
    }

    private class LinkedBiIterator implements SeqBiIterator {
        private Node current = head;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public SequenceItem next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements");
            }
            SequenceItem item = current.item;
            current = current.next;
            return item;
        }

        @Override
        public boolean hasPrevious() {
            if (current == null) {
                // 已越过尾部（next 耗尽所有元素），若链表非空则可回退
                return tail != null;
            }
            return current.prev != null;
        }

        @Override
        public SequenceItem previous() {
            if (!hasPrevious()) {
                throw new IllegalStateException("No previous elements");
            }
            if (current == null) {
                // 从越过尾部的位置回退到最后一个元素
                current = tail;
            } else {
                current = current.prev;
            }
            return current.item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported in LinkedBiIterator");
        }
    }

}
