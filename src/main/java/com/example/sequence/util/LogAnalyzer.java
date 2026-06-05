package com.example.sequence.util;

import com.example.sequence.SeqIterator;
import com.example.sequence.SeqBiIterator;
import com.example.sequence.SequenceItem;
import com.example.sequence.impl.ArraySequence;

import java.util.logging.Logger;

/**
 * 日志分析器 —— 依赖序列容器和迭代器进行日志数据的统计与查询。
 * <p>
 * 所有数据访问只能通过迭代器（{@link SeqIterator} / {@link SeqBiIterator}）进行，
 * 禁止使用 {@code get(int)} 等基于下标的方式。
 * </p>
 *
 * <h2>设计讨论与改进方向</h2>
 *
 * <h3>1. 扩展需求：按小时统计 ERROR 数量</h3>
 * <p>
 * 若未来需要实现 {@code countByLevelPerHour(SeqIterator it, String level)}，
 * 返回 {@code Map<Integer, Integer>}（hour → count），现有设计几乎无需修改核心结构：
 * </p>
 * <ul>
 *   <li><b>可复用部分：</b>{@link #parseLog(String)} 已解析出 {@link LogRecord#timestamp}，
 *       只需在聚合时额外提取小时数（{@code (timestamp / 3600000) % 24} 或通过
 *       {@code java.time.Instant} 转换），其余解析与校验逻辑完全不变。</li>
 *   <li><b>迭代器优势：</b>新方法仍然只需单向遍历一次 —— {@code while(it.hasNext())} →
 *       解析 → 校验 → 聚合到 {@code HashMap}。不需要随机访问，不需要修改原序列，
 *       完美契合迭代器模式。</li>
 *   <li><b>不改动原序列：</b>迭代器是只读视图（即使 {@code remove()} 存在，我们也不调用），
 *       统计分析对原数据零副作用。未来任何统计维度（按天、按关键字、按时间窗口）都可以
 *       通过相同的“迭代 + {@code parseLog} + 聚合”模式实现，无需触碰 {@code Sequence} 接口。</li>
 *   <li><b>潜在瓶颈：</b>{@code parseLog} 对每条日志执行正则匹配（{@code timestampStr.matches("\\d+")}），
 *       数百万条日志场景下可优化为字符逐位判断（{@code charAt + isDigit} 循环），避免重复编译
 *       {@code Pattern}。</li>
 * </ul>
 *
 * <h3>2. 大规模场景：{@code filterByKeyword} 的懒加载 / 流式改进</h3>
 * <p>
 * 当前 {@link #filterByKeyword(SeqIterator, String)} 返回一个完全物化的 {@link ArraySequence}，
 * 所有匹配项一次性加载到内存。当日志量达到百万级且匹配率较高时，存在 OOM 风险。
 * 改进方案 —— 返回一个<b>自定义延迟计算视图</b>（Lazy View），核心思路：
 * </p>
 * <ol>
 *   <li><b>定义 {@code FilteredLogSequence implements Sequence}</b>：
 *     <ul>
 *       <li>字段：持有原始 {@code Sequence} 引用 + {@code keyword}。</li>
 *       <li>不预计算、不存储结果列表。</li>
 *       <li>不支持 {@code add / remove}（只读视图），调用时抛出
 *           {@code UnsupportedOperationException}。</li>
 *       <li>{@code size()} 需要遍历计数（开销大），可在首次调用后缓存结果。</li>
 *       <li>{@code get(int)} 需要跳过不匹配项定位到第 N 个匹配项，
 *           同样可配合简单的索引跳跃表缓存。</li>
 *     </ul>
 *   </li>
 *   <li><b>核心 —— 自定义迭代器 {@code FilteredLogIterator implements SeqIterator}</b>：
 *     <ul>
 *       <li>持有原始序列的独立迭代器（每次调用 {@code iterator()} 时重新获取）。</li>
 *       <li>{@code hasNext()} 中内部循环：不断调用内部迭代器的 {@code next()}，
 *           跳过解析失败或不含关键字的项，直到找到下一个匹配项（或耗尽），
 *           将匹配项缓存在 {@code nextReady} 字段中。</li>
 *       <li>{@code next()} 返回已缓存的 {@code nextReady}，并清空缓存触发下一轮查找。</li>
 *       <li>这种“预取 + 过滤”模式让调用者仍然看到普通的迭代器接口，
 *           但实际只在每次调用 {@code next()} 时才推进计算。</li>
 *     </ul>
 *   </li>
 *   <li><b>内存对比：</b>
 *     <ul>
 *       <li>原方案：O(M) 内存，M = 匹配项数量（可能百万级）。</li>
 *       <li>新方案：O(1) 内存，每次只持有一个预取项引用。</li>
 *     </ul>
 *   </li>
 *   <li><b>兼容性：</b>返回类型仍为 {@code Sequence}（或 {@code FilteredLogSequence}），
 *       调用方代码无需修改（仍可调用 {@code iterator() / size() / isEmpty()}）。</li>
 *   <li><b>代价：</b>多次遍历需要多次重新获取迭代器（每次都是全量扫描）；
 *       若需反复访问，可在首次遍历时惰性缓存匹配项索引，后续直接跳转。</li>
 * </ol>
 *
 * <h3>3. 测试类说明</h3>
 * <p>
 * 完整的单元测试位于 {@code LogAnalyzerTest}（同包下），覆盖：
 * {@code countByLevel}（含 null 参数）、{@code findLastByKeyword}
 * （含未命中场景）、{@code findLastByLevel}、{@code isSameLogSequence}
 * （含修改后比较、自身引用）、{@code filterByKeyword}（含 null 迭代器/关键字、
 * 大小写敏感性），以及非法日志累计计数验证。
 * </p>
 *
 * @see com.example.sequence.SeqIterator
 * @see com.example.sequence.SeqBiIterator
 * @see com.example.sequence.impl.ArraySequence
 * @see com.example.sequence.impl.LinkedSequence
 */
public class LogAnalyzer {

    /** 日志记录器 */
    private static final Logger LOGGER = Logger.getLogger(LogAnalyzer.class.getName());

    /** 累计统计到的非法日志条数（跨多次调用共享） */
    private static int totalIllegalLogCount = 0;

    // ==================== 公开方法（空骨架） ====================

    /**
     * 统计指定级别（INFO / WARN / ERROR）的日志条数。
     * <p>
     * 忽略格式不合法的日志；每忽略一条，通过 {@link #LOGGER} 输出警告，
     * 并累加到 {@link #totalIllegalLogCount} 中。
     * </p>
     *
     * @param it    日志迭代器（仅前向）
     * @param level 目标级别，大小写不敏感（"INFO" / "WARN" / "ERROR"）
     * @return 匹配该级别的合法日志条数
     */
    public int countByLevel(SeqIterator it, String level) {
        if (level == null) {
            return 0;
        }
        String targetLevel = level.toUpperCase();
        int count = 0;

        while (it.hasNext()) {
            SequenceItem item = it.next();
            if (item == null) {
                logIllegalEntry(null);
                continue;
            }
            LogRecord record = parseLog(item.getData());
            if (record == null) {
                // parseLog 内部已记录 WARNING
                continue;
            }
            if (record.level.equals(targetLevel)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从后往前查找包含指定关键字的日志。
     * <p>
     * 先将迭代器移动至末尾，再利用 {@link SeqBiIterator#previous()} 从后向前扫描，
     * 返回第一个匹配的日志项。关键字匹配针对解析后的 {@code message} 字段，
     * 区分大小写。格式不合法的日志会被跳过（{@link #parseLog(String)} 内部记录 WARNING）。
     * </p>
     *
     * @param it      双向迭代器（可处于任意位置，方法内部会先移动到末尾）
     * @param keyword 关键字（区分大小写）
     * @return 第一个匹配的原始 {@link SequenceItem}；若未找到则返回 {@code null}
     */
    public SequenceItem findLastByKeyword(SeqBiIterator it, String keyword) {
        if (it == null || keyword == null) {
            return null;
        }

        // 先将迭代器移动到序列末尾
        while (it.hasNext()) {
            it.next();
        }

        // 从后往前遍历
        while (it.hasPrevious()) {
            SequenceItem item = it.previous();
            if (item == null) {
                continue;
            }
            LogRecord record = parseLog(item.getData());
            if (record == null) {
                continue; // 格式不合法，跳过
            }
            if (record.message.contains(keyword)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 从后往前查找指定日志级别的日志。
     * <p>
     * 先将迭代器移动至末尾，再利用 {@link SeqBiIterator#previous()} 从后向前扫描，
     * 返回第一个匹配的日志项。级别匹配规则与 {@link #countByLevel} 一致
     * （大小写不敏感，仅接受 INFO / WARN / ERROR）。
     * 格式不合法的日志会被跳过（{@link #parseLog(String)} 内部记录 WARNING）。
     * </p>
     *
     * @param it    双向迭代器（可处于任意位置，方法内部会先移动到末尾）
     * @param level 目标级别，大小写不敏感
     * @return 第一个匹配的原始 {@link SequenceItem}；若未找到则返回 {@code null}
     */
    public SequenceItem findLastByLevel(SeqBiIterator it, String level) {
        if (it == null || level == null) {
            return null;
        }
        String targetLevel = level.toUpperCase();

        // 先将迭代器移动到序列末尾
        while (it.hasNext()) {
            it.next();
        }

        // 从后往前遍历
        while (it.hasPrevious()) {
            SequenceItem item = it.previous();
            if (item == null) {
                continue;
            }
            LogRecord record = parseLog(item.getData());
            if (record == null) {
                continue; // 格式不合法，跳过
            }
            if (record.level.equals(targetLevel)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 比较两个日志迭代器是否完全相同（内容与顺序均一致）。
     * <p>
     * 同时步进两个迭代器，逐项比较 {@link SequenceItem#getData()} 的字符串相等性。
     * 只有当两者同时耗尽、且每一项的原始数据都相等时才返回 {@code true}。
     * </p>
     * <p>
     * <b>注意：</b>迭代器是一次性的 —— 本方法会消费传入的迭代器。
     * 若调用者需要对同一序列反复比较，应每次都通过
     * {@code Sequence.iterator()} 重新获取新的迭代器实例。
     * </p>
     *
     * @param it1 第一个日志迭代器
     * @param it2 第二个日志迭代器
     * @return 两者内容与顺序完全一致时返回 {@code true}
     */
    public boolean isSameLogSequence(SeqIterator it1, SeqIterator it2) {
        // 两个引用相同（包括同为 null），快速返回
        if (it1 == it2) {
            return true;
        }
        // 一个为 null 另一个不为 null
        if (it1 == null || it2 == null) {
            return false;
        }

        // 同步步进两个迭代器
        while (it1.hasNext() && it2.hasNext()) {
            SequenceItem item1 = it1.next();
            SequenceItem item2 = it2.next();

            String data1 = (item1 != null) ? item1.getData() : null;
            String data2 = (item2 != null) ? item2.getData() : null;

            // 使用 java.util.Objects.equals 安全处理 null 情况
            if (!java.util.Objects.equals(data1, data2)) {
                return false;
            }
        }

        // 双方必须同时耗尽 —— 若还有一方剩余元素，则长度不同
        return !it1.hasNext() && !it2.hasNext();
    }

    /**
     * 过滤出包含指定关键字的日志项，保持原顺序。
     * <p>
     * 遍历迭代器，对每条日志调用 {@link #parseLog(String)} 进行解析。
     * 解析成功且 {@code message} 包含关键字（区分大小写）的项，
     * 将其原始 {@link SequenceItem} 原样添加到新的 {@link ArraySequence} 中。
     * 解析失败的条目会被忽略（{@code parseLog} 内部自动记录 WARNING 并累加计数）。
     * </p>
     * <p>
     * <b>注意：</b>返回序列中的 {@link SequenceItem} 与原序列共享同一对象引用，
     * 这是安全的 —— 修改其 {@code data} 不会破坏原序列的结构。
     * </p>
     *
     * @param it      日志迭代器（仅前向）
     * @param keyword 关键字（区分大小写）；为 {@code null} 时返回空序列
     * @return 新创建的 {@link ArraySequence}，仅包含匹配项，保持原顺序
     */
    public ArraySequence filterByKeyword(SeqIterator it, String keyword) {
        ArraySequence result = new ArraySequence();

        if (it == null || keyword == null) {
            return result;
        }

        while (it.hasNext()) {
            SequenceItem item = it.next();
            if (item == null) {
                continue;
            }
            LogRecord record = parseLog(item.getData());
            if (record == null) {
                continue; // 格式不合法，parseLog 内部已记录 WARNING
            }
            if (record.message.contains(keyword)) {
                result.add(item); // 复用原对象引用
            }
        }

        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 返回累计的非法日志计数。
     *
     * @return 累计非法日志条数
     */
    public static int getInvalidLogCount() {
        return totalIllegalLogCount;
    }

    /**
     * 重置累计的非法日志计数。
     */
    public static void resetIllegalLogCount() {
        totalIllegalLogCount = 0;
    }

    /**
     * 记录一条非法日志（格式不合法）的警告。
     *
     * @param rawData 原始日志数据（可能为 null）
     */
    private static void logIllegalEntry(String rawData) {
        totalIllegalLogCount++;
        LOGGER.warning("Illegal log entry (total: " + totalIllegalLogCount + "): " + rawData);
    }

    // ==================== 日志解析 ====================

    /** 合法的日志级别 */
    private static final java.util.Set<String> VALID_LEVELS =
            java.util.Set.of("INFO", "WARN", "ERROR");

    /**
     * 解析一行日志数据为 {@link LogRecord}。
     * <p>
     * 期望格式：{@code LEVEL|timestamp|message}，三个部分以 {@code |} 分隔。
     * 不符合格式、level 非法、或 timestamp 非纯数字时返回 {@code null}，
     * 并通过 {@link #LOGGER} 输出 WARNING 记录原始内容。
     * </p>
     *
     * @param data 原始日志字符串，可能为 {@code null}
     * @return 解析成功的 {@link LogRecord}；失败返回 {@code null}
     */
    private static LogRecord parseLog(String data) {
        if (data == null || data.isEmpty()) {
            logIllegalEntry(data);
            return null;
        }

        String[] parts = data.split("\\|", -1);
        if (parts.length != 3) {
            logIllegalEntry(data);
            return null;
        }

        String level = parts[0].toUpperCase();
        if (!VALID_LEVELS.contains(level)) {
            logIllegalEntry(data);
            return null;
        }

        String timestampStr = parts[1];
        if (!timestampStr.matches("\\d+")) {
            logIllegalEntry(data);
            return null;
        }

        long timestamp = Long.parseLong(timestampStr);
        String message = parts[2];

        return new LogRecord(level, timestamp, message);
    }

    // ==================== 内部类 ====================

    /**
     * 解析后的日志记录，包含 level、timestamp、message 三个字段。
     */
    public static class LogRecord {
        /** 日志级别：INFO / WARN / ERROR */
        public final String level;
        /** 时间戳（毫秒） */
        public final long timestamp;
        /** 日志消息正文 */
        public final String message;

        public LogRecord(String level, long timestamp, String message) {
            this.level = level;
            this.timestamp = timestamp;
            this.message = message;
        }

        @Override
        public String toString() {
            return level + "|" + timestamp + "|" + message;
        }
    }
}
