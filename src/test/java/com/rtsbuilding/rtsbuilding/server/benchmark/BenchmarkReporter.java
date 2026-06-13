package com.rtsbuilding.rtsbuilding.server.benchmark;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基准测试结果收集器 / Benchmark Result Collector
 *
 * <p>替代 {@link System#out} 直接输出，将所有 benchmark 结果收集到内存中，
 * 在 JVM 退出时写入 {@code benchmark-results.txt}。
 * 首次运行时仅输出结果；从第二次起自动对比上次结果并附加性能变化报告。
 * 每次运行前会自动将旧结果归档到 {@code benchmark-results-history/} 目录。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * BenchmarkReporter.record("[Module] metric: %d ns/op", value);
 * }</pre>
 */
public final class BenchmarkReporter {

    private static final String OUTPUT_FILE = "benchmark-results.txt";
    private static final Path OUTPUT_PATH = Paths.get(OUTPUT_FILE);
    private static final Path HISTORY_DIR = Paths.get("benchmark-results-history");
    private static final DateTimeFormatter HISTORY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Object LOCK = new Object();
    private static final List<String> RESULTS = new ArrayList<>();
    private static volatile boolean hookRegistered = false;

    /**
     * 解析指标行的正则：
     * [Category] description: <result>
     * 捕获 category、description、第一个数值。
     */
    private static final Pattern METRIC_PATTERN =
            Pattern.compile("(?:^\\s+)?\\[(\\w+)\\]\\s+(.+?):\\s+(?:avg\\s+)?([\\d,.]+)");

    private BenchmarkReporter() {
    }

    /**
     * 记录一条 benchmark 结果（格式同 {@link String#format}）。
     * 同时输出到 stdout 和内存队列，JVM 退出时自动写入文件。
     */
    public static void record(String format, Object... args) {
        String line = String.format(format, args);
        synchronized (LOCK) {
            RESULTS.add(line);
        }
        // Also print to stdout for real-time viewing
        System.out.println(line);
        ensureShutdownHook();
    }

    private static void ensureShutdownHook() {
        if (!hookRegistered) {
            synchronized (LOCK) {
                if (!hookRegistered) {
                    Runtime.getRuntime().addShutdownHook(new Thread(BenchmarkReporter::flush));
                    hookRegistered = true;
                }
            }
        }
    }

    /**
     * 将收集到的所有结果写入 benchmark-results.txt。
     * 由 shutdown hook 自动调用，也可手动调用（例如在 Gradle task 中）。
     *
     * <p>流程：
     * <ol>
     *   <li>读取旧 baseline 解析指标</li>
     *   <li>归档旧 baseline 到 history 目录</li>
     *   <li>写入新结果</li>
     *   <li>如有旧 baseline，追加性能变化对比报告</li>
     * </ol>
     */
    public static void flush() {
        synchronized (LOCK) {
            if (RESULTS.isEmpty()) {
                return;
            }
            try {
                // 1. Parse old baseline if exists
                Map<String, Double> oldMetrics = parseResultsFile(OUTPUT_PATH);

                // 2. Archive old baseline to history before overwriting
                archiveBaseline();

                // 3. Write new results
                List<String> reportLines = buildReportLines();
                Files.write(OUTPUT_PATH, reportLines,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // 4. Generate and append diff if old baseline exists
                if (!oldMetrics.isEmpty()) {
                    Map<String, Double> newMetrics = parseMetrics(RESULTS);
                    List<String> diffLines = buildDiffReport(oldMetrics, newMetrics);
                    Files.write(OUTPUT_PATH, diffLines, StandardOpenOption.APPEND);
                    System.out.println("[BenchmarkReporter] 性能变化对比已附加到 " + OUTPUT_PATH.toAbsolutePath());
                }

                System.out.println("[BenchmarkReporter] 结果已写入: " + OUTPUT_PATH.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[BenchmarkReporter] 写入失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 构建完整的报告行列表（头部 + 分类 + 指标 + 尾部）。
     */
    private static List<String> buildReportLines() {
        List<String> lines = new ArrayList<>();
        lines.add("╔══════════════════════════════════════════════════════╗");
        lines.add("║  RTS Building — 极限性能测试报告                       ║");
        lines.add("║  Extreme Performance Benchmark Report                ║");
        lines.add("╚══════════════════════════════════════════════════════╝");
        lines.add("生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        lines.add("");

        // Add section separators by detecting category changes
        String currentCategory = "";
        for (String result : RESULTS) {
            // Extract category from "[RtsXXX]" prefix
            String category = extractCategory(result);
            if (!category.equals(currentCategory)) {
                if (!currentCategory.isEmpty()) {
                    lines.add("");
                }
                lines.add("── " + category + " ──────────────────────");
                currentCategory = category;
            }
            lines.add("  " + result);
        }

        lines.add("");
        lines.add("── 测试完成 ──────────────────────────");
        lines.add("共计 " + RESULTS.size() + " 项基准测试记录");
        return lines;
    }

    /**
     * 将旧的 benchmark-results.txt 归档到 benchmark-results-history/ 目录。
     */
    private static void archiveBaseline() throws IOException {
        if (!Files.exists(OUTPUT_PATH)) {
            return;
        }
        Files.createDirectories(HISTORY_DIR);
        String timestamp = LocalDateTime.now().format(HISTORY_FMT);
        Path historyFile = HISTORY_DIR.resolve("benchmark-results-" + timestamp + ".txt");
        Files.copy(OUTPUT_PATH, historyFile, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[BenchmarkReporter] 旧结果已归档: " + historyFile.toAbsolutePath());
    }

    /**
     * 解析 benchmark 结果文件，提取所有指标的 key → value 映射。
     */
    private static Map<String, Double> parseResultsFile(Path file) {
        if (!Files.exists(file)) {
            return Collections.emptyMap();
        }
        try {
            List<String> lines = Files.readAllLines(file);
            return parseMetrics(lines);
        } catch (IOException e) {
            System.err.println("[BenchmarkReporter] 无法读取基准文件: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 解析若干行文本，提取指标的 key（[Category] description）→ 数值 的映射。
     *
     * <p>key 示例：{@code [RtsAggregateStorage] drainPendingChanges × 10,000}</p>
     */
    static Map<String, Double> parseMetrics(List<String> lines) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher m = METRIC_PATTERN.matcher(line);
            if (m.find()) {
                String category = m.group(1);
                String description = m.group(2).trim();
                String valueStr = m.group(3).replace(",", "");
                String key = "[" + category + "] " + description;
                try {
                    double value = Double.parseDouble(valueStr);
                    metrics.put(key, value);
                } catch (NumberFormatException ignored) {
                    // skip unparseable lines
                }
            }
        }
        return metrics;
    }

    /**
     * 构建旧 vs 新性能变化对比报告。
     *
     * <p>对于每一项指标：
     * <ul>
     *   <li>🟢 绿色 = 性能提升（数值下降）</li>
     *   <li>🔴 红色 = 性能下降（数值上升）</li>
     *   <li>⚪ 灰色 = 无变化</li>
     *   <li>🆕 新增指标</li>
     *   <li>🗑️ 已移除指标</li>
     * </ul>
     * </p>
     */
    private static List<String> buildDiffReport(Map<String, Double> oldMetrics,
                                                  Map<String, Double> newMetrics) {
        List<String> diff = new ArrayList<>();
        diff.add("");
        diff.add("╔══════════════════════════════════════════════════════╗");
        diff.add("║  性能变化对比 Performance Delta                        ║");
        diff.add("║  🟢 = 提升 (数值↓)  |  🔴 = 下降 (数值↑)               ║");
        diff.add("║  ⚪ = 无变化  |  🆕 = 新增  |  🗑 = 已移除             ║");
        diff.add("╚══════════════════════════════════════════════════════╝");
        diff.add("");

        // ── 收集所有指标变化 ──
        int improved = 0, regressed = 0, unchanged = 0, added = 0, removed = 0;
        List<String> changedEntries = new ArrayList<>();
        List<String> addedEntries = new ArrayList<>();
        List<String> removedEntries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : newMetrics.entrySet()) {
            String key = entry.getKey();
            double newVal = entry.getValue();
            Double oldVal = oldMetrics.get(key);

            if (oldVal == null) {
                addedEntries.add(formatDiffLine("\uD83C\uDD95", key, oldVal, newVal));
                added++;
            } else {
                double diffVal = newVal - oldVal;
                double pct = oldVal != 0 ? (diffVal / oldVal) * 100.0 : 0;
                String icon;
                if (diffVal < 0) {
                    icon = "\uD83D\uDFE2";
                    improved++;
                } else if (diffVal > 0) {
                    icon = "\uD83D\uDD34";
                    regressed++;
                } else {
                    icon = "\u26AA";
                    unchanged++;
                }
                changedEntries.add(formatDiffLine(icon, key, oldVal, newVal, diffVal, pct));
            }
        }

        // Find removed metrics
        for (String key : oldMetrics.keySet()) {
            if (!newMetrics.containsKey(key)) {
                removedEntries.add(formatDiffLine("\uD83D\uDDD1", key, oldMetrics.get(key), null));
                removed++;
            }
        }

        // ── 写入对比报告 ──
        if (!changedEntries.isEmpty()) {
            diff.add("── 变化指标 ──────────────────────");
            diff.addAll(changedEntries);
            diff.add("");
        }
        if (!addedEntries.isEmpty()) {
            diff.add("── 新增指标 ──────────────────────");
            diff.addAll(addedEntries);
            diff.add("");
        }
        if (!removedEntries.isEmpty()) {
            diff.add("── 移除指标 ──────────────────────");
            diff.addAll(removedEntries);
            diff.add("");
        }

        diff.add("── 汇总 ──────────────────────────");
        diff.add(String.format("  🟢 提升: %d 项  |  🔴 下降: %d 项  |  ⚪ 无变化: %d 项",
                improved, regressed, unchanged));
        diff.add(String.format("  🆕 新增: %d 项  |  🗑 移除: %d 项", added, removed));
        diff.add("");

        return diff;
    }

    /**
     * 格式化一条变化指标的对比行。
     */
    private static String formatDiffLine(String icon, String key,
                                          Double oldVal, Double newVal,
                                          double diff, double pct) {
        String oldStr = oldVal != null ? formatValue(oldVal) : "—";
        String newStr = newVal != null ? formatValue(newVal) : "—";
        String sign = diff < 0 ? "" : (diff > 0 ? "+" : "");
        String arrow = diff < 0 ? "↓" : (diff > 0 ? "↑" : "—");
        return String.format("  %s %-60s  %s → %s  (%s%.1f%%)  %s",
                icon, key, oldStr, newStr, sign, pct, arrow);
    }

    /**
     * 格式化一条新增/移除指标的对比行。
     */
    private static String formatDiffLine(String icon, String key,
                                          Double oldVal, Double newVal) {
        String oldStr = oldVal != null ? formatValue(oldVal) : "—";
        String newStr = newVal != null ? formatValue(newVal) : "—";
        if (icon.equals("\uD83C\uDD95")) {
            return String.format("  %s %-60s  新增: %s ns/op", icon, key, newStr);
        } else {
            return String.format("  %s %-60s  已移除 (上次: %s ns/op)", icon, key, oldStr);
        }
    }

    /**
     * 格式化数值：整数用千分位，小数保留一位。
     */
    private static String formatValue(double val) {
        if (val == (long) val) {
            return String.format("%,.0f", val);
        }
        return String.format("%,.1f", val);
    }

    /**
     * 从一行结果中提取分类名（例如 "[RtsPageCache]" → "RtsPageCache"）。
     */
    private static String extractCategory(String line) {
        if (line == null || line.isEmpty()) {
            return "Unknown";
        }
        int start = line.indexOf('[');
        if (start < 0) return "General";
        int end = line.indexOf(']', start);
        if (end < 0) return "General";
        String tag = line.substring(start + 1, end);
        // Remove trailing benchmark suffix if present
        if (tag.endsWith("Benchmark")) {
            tag = tag.substring(0, tag.length() - "Benchmark".length());
        }
        return tag;
    }
}
