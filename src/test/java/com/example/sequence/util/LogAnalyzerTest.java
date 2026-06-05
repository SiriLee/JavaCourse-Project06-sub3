package com.example.sequence.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sequence.SeqBiIterator;
import com.example.sequence.SeqIterator;
import com.example.sequence.SequenceItem;
import com.example.sequence.impl.ArraySequence;
import com.example.sequence.impl.LinkedSequence;

import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link LogAnalyzer} 的单元测试类。
 * <p>
 * 所有数据访问均通过迭代器进行，不使用 {@code get(int)}。
 * </p>
 */
public class LogAnalyzerTest {

    private static final Logger LOGGER = Logger.getLogger(LogAnalyzerTest.class.getName());

    private ArraySequence arraySeq;
    private LinkedSequence linkedSeq;

    // 测试数据 —— 为两个序列各自创建独立的 SequenceItem，避免共享引用干扰修改测试
    private static final String[] RAW_LOGS = {
            "INFO|100|User login",
            "ERROR|101|Null pointer in ModuleA",
            "WARN|102|Slow response",
            "ERROR|103|Another error",
            "BAD_FORMAT",
            "ERROR|104|keyword found here"
    };

    @BeforeEach
    public void setUp() {
        // 重置跨测试共享的非法日志计数
        LogAnalyzer.resetIllegalLogCount();

        arraySeq = new ArraySequence();
        linkedSeq = new LinkedSequence();

        // 为两个序列分别创建独立的 SequenceItem（相同内容，不同对象）
        for (String raw : RAW_LOGS) {
            arraySeq.add(new SequenceItem(raw));
            linkedSeq.add(new SequenceItem(raw));
        }
    }

    @AfterEach
    public void tearDown() {
        LogAnalyzer.resetIllegalLogCount();
    }

    // ==================== countByLevel 测试 ====================

    @Test
    public void testCountByLevel_Error() {
        LogAnalyzer analyzer = new LogAnalyzer();

        // 使用 ArraySequence 的迭代器
        int arrayErrorCount = analyzer.countByLevel(arraySeq.iterator(), "ERROR");
        LOGGER.info("[ArraySequence] ERROR count: " + arrayErrorCount);
        assertEquals(3, arrayErrorCount,
                "合法 ERROR 日志应有 3 条（索引 1, 3, 5），非法 'BAD_FORMAT' 不计入");

        // 使用 LinkedSequence 的迭代器
        int linkedErrorCount = analyzer.countByLevel(linkedSeq.iterator(), "ERROR");
        LOGGER.info("[LinkedSequence] ERROR count: " + linkedErrorCount);
        assertEquals(3, linkedErrorCount,
                "合法 ERROR 日志应有 3 条（索引 1, 3, 5），非法 'BAD_FORMAT' 不计入");

        // 验证非法日志累计计数（两个序列各含一条 "BAD_FORMAT"，共 2 条非法）
        int invalidCount = LogAnalyzer.getInvalidLogCount();
        LOGGER.info("Total illegal log count: " + invalidCount);
        assertEquals(2, invalidCount, "两个序列各有一条 'BAD_FORMAT'");
    }

    @Test
    public void testCountByLevel_Info() {
        LogAnalyzer analyzer = new LogAnalyzer();

        int count = analyzer.countByLevel(arraySeq.iterator(), "INFO");
        LOGGER.info("[ArraySequence] INFO count: " + count);
        assertEquals(1, count, "只有一条 INFO 日志");
    }

    @Test
    public void testCountByLevel_NullLevel() {
        LogAnalyzer analyzer = new LogAnalyzer();

        int count = analyzer.countByLevel(arraySeq.iterator(), null);
        LOGGER.info("countByLevel with null level: " + count);
        assertEquals(0, count, "level 为 null 时应返回 0");
    }

    // ==================== findLastByKeyword 测试 ====================

    @Test
    public void testFindLastByKeyword() {
        LogAnalyzer analyzer = new LogAnalyzer();

        // ArraySequence 双向迭代器
        SeqBiIterator arrayIt = arraySeq.biIterator();
        SequenceItem arrayResult = analyzer.findLastByKeyword(arrayIt, "keyword");
        assertNotNull(arrayResult, "应找到包含 'keyword' 的日志项");
        LOGGER.info("[ArraySequence] findLastByKeyword('keyword'): " + arrayResult.getData());
        assertTrue(arrayResult.getData().contains("keyword"));

        // LinkedSequence 双向迭代器
        SeqBiIterator linkedIt = linkedSeq.biIterator();
        SequenceItem linkedResult = analyzer.findLastByKeyword(linkedIt, "keyword");
        assertNotNull(linkedResult, "应找到包含 'keyword' 的日志项");
        LOGGER.info("[LinkedSequence] findLastByKeyword('keyword'): " + linkedResult.getData());
        assertEquals("ERROR|104|keyword found here", linkedResult.getData(),
                "应返回最后一条匹配的日志");
    }

    @Test
    public void testFindLastByKeyword_NotFound() {
        LogAnalyzer analyzer = new LogAnalyzer();

        SeqBiIterator it = arraySeq.biIterator();
        SequenceItem result = analyzer.findLastByKeyword(it, "NonExistentKeyword");
        LOGGER.info("findLastByKeyword('NonExistentKeyword'): " + result);
        assertEquals(null, result, "不存在的关键字应返回 null");
    }

    // ==================== findLastByLevel 测试 ====================

    @Test
    public void testFindLastByLevel() {
        LogAnalyzer analyzer = new LogAnalyzer();

        SeqBiIterator it = linkedSeq.biIterator();
        SequenceItem result = analyzer.findLastByLevel(it, "ERROR");
        assertNotNull(result, "应找到最后一条 ERROR 日志");
        LOGGER.info("[LinkedSequence] findLastByLevel('ERROR'): " + result.getData());
        assertEquals("ERROR|104|keyword found here", result.getData(),
                "应返回序列中最后出现的 ERROR 日志");
    }

    // ==================== isSameLogSequence 测试 ====================

    @Test
    public void testIsSameLogSequence_Identical() {
        LogAnalyzer analyzer = new LogAnalyzer();

        boolean same = analyzer.isSameLogSequence(
                arraySeq.iterator(), linkedSeq.iterator());
        LOGGER.info("isSameLogSequence (identical): " + same);
        assertTrue(same, "两个序列内容完全相同，应返回 true");
    }

    @Test
    public void testIsSameLogSequence_AfterModification() {
        LogAnalyzer analyzer = new LogAnalyzer();

        // 先确认初始相同
        assertTrue(analyzer.isSameLogSequence(
                arraySeq.iterator(), linkedSeq.iterator()));

        // 修改 linkedSeq 中的一条数据 —— 仅通过迭代器操作，不使用 get(i)
        SeqIterator modifyIt = linkedSeq.iterator();
        while (modifyIt.hasNext()) {
            SequenceItem item = modifyIt.next();
            if (item.getData().contains("Another error")) {
                item.setData("ERROR|103|Modified data — different");
                LOGGER.info("Modified item via iterator: " + item.getData());
                break;
            }
        }

        // 修改后再比较（重新获取迭代器）
        boolean sameAfterModification = analyzer.isSameLogSequence(
                arraySeq.iterator(), linkedSeq.iterator());
        LOGGER.info("isSameLogSequence (after modification): " + sameAfterModification);
        assertFalse(sameAfterModification, "修改后内容不同，应返回 false");
    }

    @Test
    public void testIsSameLogSequence_Self() {
        LogAnalyzer analyzer = new LogAnalyzer();

        // 同一迭代器引用 —— 快速路径
        SeqIterator it = arraySeq.iterator();
        boolean same = analyzer.isSameLogSequence(it, it);
        LOGGER.info("isSameLogSequence (self): " + same);
        assertTrue(same, "同一引用应返回 true");
    }

    // ==================== filterByKeyword 测试 ====================

    @Test
    public void testFilterByKeyword_LowercaseError() {
        LogAnalyzer analyzer = new LogAnalyzer();

        // 筛选 message 中包含 "error"（小写）的日志
        ArraySequence filtered = analyzer.filterByKeyword(
                linkedSeq.iterator(), "error");
        LOGGER.info("filterByKeyword('error') — result size: " + filtered.size());

        // 验证结果：仅 "Another error" 包含小写 "error"，非法 BAD_FORMAT 被跳过
        assertEquals(1, filtered.size(),
                "只有 'ERROR|103|Another error' 的 message 包含小写 'error'");

        // 通过迭代器遍历结果，不使用 get(i)
        SeqIterator resultIt = filtered.iterator();
        while (resultIt.hasNext()) {
            SequenceItem item = resultIt.next();
            LOGGER.info("  filtered item: " + item.getData());
            assertTrue(item.getData().contains("error"),
                    "过滤结果应包含 'error' 关键字");
        }
    }

    @Test
    public void testFilterByKeyword_MultipleMatches() {
        LogAnalyzer analyzer = new LogAnalyzer();

        // 筛选 message 中包含 "error" 的日志 —— 这里 "error" 小写，仅 'Another error' 匹配
        ArraySequence filtered = analyzer.filterByKeyword(
                arraySeq.iterator(), "error");

        LOGGER.info("filterByKeyword('error') size: " + filtered.size());

        // 用迭代器验证每一项
        SeqIterator it = filtered.iterator();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(1, count, "应只有 1 条 message 包含小写 'error'");
    }

    @Test
    public void testFilterByKeyword_NotFound() {
        LogAnalyzer analyzer = new LogAnalyzer();

        ArraySequence filtered = analyzer.filterByKeyword(
                arraySeq.iterator(), "XYZ123_NonExistent");
        LOGGER.info("filterByKeyword('XYZ123_NonExistent') size: " + filtered.size());
        assertEquals(0, filtered.size(), "无匹配时应返回空序列");
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterByKeyword_NullIterator() {
        LogAnalyzer analyzer = new LogAnalyzer();

        ArraySequence filtered = analyzer.filterByKeyword(null, "error");
        LOGGER.info("filterByKeyword(null, 'error') size: " + filtered.size());
        assertEquals(0, filtered.size(), "null 迭代器应返回空序列");
    }

    @Test
    public void testFilterByKeyword_NullKeyword() {
        LogAnalyzer analyzer = new LogAnalyzer();

        ArraySequence filtered = analyzer.filterByKeyword(arraySeq.iterator(), null);
        LOGGER.info("filterByKeyword(it, null) size: " + filtered.size());
        assertEquals(0, filtered.size(), "null 关键字应返回空序列");
    }
}
