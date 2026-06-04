# 序列与迭代器

## Task1
源代码：[SequenceItem](src/main/java/com/example/sequence/SequenceItem.java)

## Task2
源代码：[Sequence](src/main/java/com/example/sequence/Sequence.java)

## Task3
源代码：[SeqIterator](src/main/java/com/example/sequence/SeqIterator.java)

## Task4
源代码：[ArraySequence](src/main/java/com/example/sequence/impl/ArraySequence.java)

关键片段：
- 容量维护
    ```java
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
    ```
- 内部类迭代器（示例）
    ```java
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
    ```

