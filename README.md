# 序列与迭代器

## Task1
源代码：[SequenceItem](src/main/java/com/example/sequence/SequenceItem.java)

## Task2
源代码：[Sequence](src/main/java/com/example/sequence/Sequence.java)

## Task3
源代码：[SeqIterator](src/main/java/com/example/sequence/SeqIterator.java)

## Task4
源代码：[SeqBiIterator](src/main/java/com/example/sequence/SeqBiIterator.java)

## Task5
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

## Task6
源代码：[LinkedSequence](src/main/java/com/example/sequence/impl/LinkedSequence.java)

关键片段：
- 内部类迭代器（示例）
    ```java
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
    ```

## Task7
源代码：[SequenceUtils](src/main/java/com/example/sequence/util/SequenceUtils.java)

## Task8
源代码：[IteratorTest](src/test/java/com/example/sequence/util/IteratorTest.java)

- 说明：
    > 本程序采用自动化测试，而非输出内容到控制台后比对结果。

- 测试方法：
    > 需要配置Maven环境
    ```cmd
    mvn test -Dtest=sequence.util.IteratorTest
    ```
- 运行结果：
    ```cmd
    [INFO] Results:
    [INFO] 
    [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
    [INFO] 
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    ```

## Task9

### 问题一

**答：使用 `Iterator` 遍历效率更高。**

原因分析 ——  `LinkedSequence.get(int index)` 的源码：

```java
public SequenceItem get(int index) {
    Node current = head;
    for (int i = 0; i < index; i++) {
        current = current.next;  // 每次调用都从头开始走
    }
    return current.item;
}
```

- **`for` + `get(i)` 的方式**：每次 `get(i)` 都要从链表头结点 `head` 出发，一步步走到第 `i` 个位置。遍历 n 个元素为**O(n²)** 时间复杂度。
- **`Iterator` 的方式**：迭代器内部维护一个 `current` 指针，每次 `next()` 只需执行 `current = current.next` 一步，即 **O(n)** 时间复杂度。

### 问题二

**1. 无法同时进行多次独立遍历**
如果容器只有一个游标，则同一时刻只能有一段代码在遍历。一旦需要嵌套遍历，就会互相踩踏进度。

**2. 多线程的"共享危机"**
如果把 `next()` 放在 `Sequence` 上，游标是容器共享的状态。线程 A 读到一半时线程 B 也调了 `next()`，等线程 A 再回来继续读，读到的就是 B 已经跳过去的位置，造成数据混乱。

`Iterator` 模式通过 **"每次调用 `iterator()` 返回一个全新的迭代器对象"** 解决了这个问题——每个迭代器都有自己独立的游标，各线程互不干扰。容器本身只负责存数据，不负责读到哪儿。

**3. 支持多种遍历策略**
同一个 `Sequence` 可以同时返回正向迭代器、逆向迭代器、双向迭代器。如果只能有一个 `next()` 挂在容器上，就无法同时支持这些策略。

---

### 问题三

**答：应该新增一种 `Iterator` 的实现类**

**1. 开闭原则**
`Sequence` 接口和其实现类应该**对扩展开放，对修改关闭**。新增一个内部迭代器类 `EvenIndexIterator` 是一种"扩展"，不需要改动 `Sequence` 的任何现有方法签名，也不会影响已有功能。

**2. 不污染容器接口**
如果把"只访问偶数下标"硬编码到 `Sequence` 的方法里（比如加一个 `getEvenIndexIterator()`），那未来每多一种遍历需求，就要往 `Sequence` 加一个方法，接口会不断膨胀。

**3. 这正是迭代器模式的经典应用场景**
迭代器模式的核心价值就是将“遍历算法”从容器中分离独立。容器可以各自用内部类实现最适合自己的偶数下标迭代器，但使用方只看到 `SeqIterator` 接口，不关心底层差异。

## Task10

Vibe-coding记录：
- [claude-conversation](docs/claude-conversation-2026-06-05-ae1bee7c.md)

源代码：
- [LogAnalyzer](src/main/java/com/example/sequence/util/LogAnalyzer.java)
- [LogAnalyzerTest](src/test/java/com/example/sequence/util/LogAnalyzerTest.java)

修复：
- [LinkedSequence — LinkedBiIterator 越尾判空修复](src/main/java/com/example/sequence/impl/LinkedSequence.java#L196-L213)

### 设计概述

基于已完成的 `Sequence` 与迭代器体系，构建了一个日志分析模块。所有分析操作**只通过迭代器访问数据**，同一套代码可同时处理 `ArraySequence` 与 `LinkedSequence`。

日志格式：`LEVEL|timestamp|message`（`LEVEL` = INFO / WARN / ERROR，`timestamp` = 纯数字，`message` = 任意不含竖线的文本）。

### 类结构

| 类 | 位置 | 说明 |
|---|---|---|
| `LogAnalyzer` | `com.example.sequence.util` | 核心分析器，提供统计、查找、比较、过滤功能 |
| `LogAnalyzer.LogRecord` | 内部静态类 | 解析后的日志记录（level / timestamp / message） |
| `LogAnalyzerTest` | 测试包 `com.example.sequence.util` | 14 个单元测试用例 |

### LogAnalyzer 方法一览

| 方法 | 迭代器 | 功能 |
|---|---|---|
| `countByLevel(it, level)` | `SeqIterator` | 统计指定级别日志条数，非法格式忽略并计入累计 |
| `findLastByKeyword(it, keyword)` | `SeqBiIterator` | 从后往前查找 message 含关键字的日志（区分大小写） |
| `findLastByLevel(it, level)` | `SeqBiIterator` | 从后往前查找指定级别的日志（大小写不敏感） |
| `isSameLogSequence(it1, it2)` | `SeqIterator` ×2 | 比较两个迭代器数据流完全一致（内容 + 顺序） |
| `filterByKeyword(it, keyword)` | `SeqIterator` | 筛选 message 含关键字的日志到新 `ArraySequence` |

### 私有辅助

| 方法/字段 | 说明 |
|---|---|
| `parseLog(String data)` | 解析 `LEVEL\|timestamp\|message` 为 `LogRecord`，格式不合法返回 `null` 并记录 WARNING |
| `logIllegalEntry(String rawData)` | 累加非法计数 + `LOGGER.warning()` |
| `getInvalidLogCount()` / `resetIllegalLogCount()` | 查询 / 重置累计非法日志数 |

### 测试方法

```cmd
mvn test -Dtest=sequence.util.LogAnalyzerTest
```

运行结果：

```cmd
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试覆盖场景：

- `countByLevel`：ArraySequence / LinkedSequence 各自统计 ERROR=3 / INFO=1；null 参数防护；非法日志累计计数验证
- `findLastByKeyword`：两个序列分别从后往前按关键字查找；关键字不存在返回 null
- `findLastByLevel`：从后往前按级别查找，命中最后一条匹配项
- `isSameLogSequence`：内容相同 → true；修改一条后 → false；同一引用快速路径
- `filterByKeyword`：大小写敏感匹配；null 迭代器/关键字返回空序列；遍历结果验证

### 关键设计要点

**1. 纯迭代器访问** — 所有方法内部只通过 `hasNext()` / `next()` / `hasPrevious()` / `previous()` 访问数据，不使用 `get(i)`。这使得同一套分析逻辑对 `ArraySequence` 和 `LinkedSequence` 均适用。

**2. 非法日志追踪** — `parseLog` 在解析失败时自动调用 `logIllegalEntry`，累加静态计数器并通过 `Logger.warning()` 输出原始内容，避免排查时丢失现场信息。非法计数跨调用累计，可随时查询或重置。

**3. 双向查找自动就位** — `findLastByKeyword` 和 `findLastByLevel` 内部先将迭代器移动到末尾（`while(it.hasNext()) it.next()`），再通过 `previous()` 从后往前扫描，不依赖调用方传入时的迭代器位置。

**4. Bug 修复 — LinkedBiIterator 越尾判空** — 原实现中 `hasPrevious()` 判断条件为 `current != null && current.prev != null`，当 `next()` 耗尽链表后 `current == null`，导致 `hasPrevious()` 始终返回 `false`，双向回退完全失效。修复后在 `current == null` 时退化为 `tail != null` 判断，`previous()` 中同步处理回退到 `tail` 的逻辑。

**5. 未来扩展方向** — 类 Javadoc 中讨论了按小时聚合统计（复用 `parseLog` + `timestamp`，零改动）、以及大规模场景下 `filterByKeyword` 的懒加载视图方案（自定义 `FilteredLogSequence implements Sequence`，O(1) 内存）。
