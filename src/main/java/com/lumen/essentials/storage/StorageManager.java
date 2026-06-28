package com.lumen.essentials.storage;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.violations.Violation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Asynchronous, batched logger for violations, investigation actions and ore logs.
 *
 * <p>Log lines are queued from the main thread (cheap, lock-free) and flushed to
 * disk on a single async task, so file I/O never blocks the server tick. The queue
 * is drained on shutdown to avoid losing buffered entries.
 */
public final class StorageManager {

    private final LumenEssentials plugin;
    private final ConcurrentLinkedQueue<LogLine> queue = new ConcurrentLinkedQueue<>();
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final File logDir;
    private int flushTaskId = -1;

    private static final class LogLine {
        final String file;
        final String text;

        LogLine(String file, String text) {
            this.file = file;
            this.text = text;
        }
    }

    public StorageManager(LumenEssentials plugin) {
        this.plugin = plugin;
        this.logDir = new File(plugin.getDataFolder(), "logs");
    }

    public void start() {
        if (!logDir.exists() && !logDir.mkdirs()) {
            plugin.getLogger().warning("Could not create log directory: " + logDir);
        }
        // Flush every 5 seconds (100 ticks) off the main thread.
        this.flushTaskId = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::flush, 100L, 100L)
                .getTaskId();
    }

    public void shutdown() {
        if (flushTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }
        flush();
    }

    private String stamp() {
        return timestampFormat.format(new Date());
    }

    /** Queues a formatted violation log line. */
    public void logViolation(Violation v) {
        String line = String.format(
                "[%s] %s check=%s category=%s vl=%.2f conf=%.2f ping=%d tps=%.1f "
                        + "world=%s x=%.1f y=%.1f z=%.1f server=%s plugin=%s%s",
                stamp(), v.player(), v.checkName(), v.category(), v.violationLevel(),
                v.confidence(), v.ping(), v.tps(), v.world(), v.x(), v.y(), v.z(),
                v.serverVersion(), v.pluginVersion(),
                v.debugInfo().isEmpty() ? "" : " debug=[" + v.debugInfo() + "]");
        queue.add(new LogLine("violations.log", line));
    }

    /** Queues a free-form line into {@code investigation.log}. */
    public void logInvestigation(String message) {
        queue.add(new LogLine("investigation.log", "[" + stamp() + "] " + message));
    }

    /** Queues a free-form line into {@code orelog.log}. */
    public void logOre(String message) {
        queue.add(new LogLine("orelog.log", "[" + stamp() + "] " + message));
    }

    /** Drains the queue and appends each line to its target file. */
    public void flush() {
        if (queue.isEmpty()) {
            return;
        }
        List<LogLine> batch = new ArrayList<>();
        LogLine line;
        while ((line = queue.poll()) != null) {
            batch.add(line);
        }
        // Group writes by file to minimize open/close churn.
        for (LogLine entry : batch) {
            File target = new File(logDir, entry.file);
            try (BufferedWriter writer = Files.newBufferedWriter(target.toPath(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(entry.text);
                writer.newLine();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to write log " + entry.file, ex);
            }
        }
    }
}
