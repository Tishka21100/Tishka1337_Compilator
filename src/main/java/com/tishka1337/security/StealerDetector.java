package com.tishka1337.security;

import com.tishka1337.utils.Logger;
import java.util.*;
import java.util.regex.*;

/**
 * Детектор стилеров, бэкдоров и подозрительного кода.
 * Анализирует декомпилированный Java-код и ищет паттерны угроз.
 */
public class StealerDetector {

    public static class Threat {
        public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }

        public final String fileName;
        public final Severity severity;
        public final String category;
        public final String description;
        public final String evidence;

        public Threat(String fileName, Severity severity, String category, String description, String evidence) {
            this.fileName = fileName;
            this.severity = severity;
            this.category = category;
            this.description = description;
            this.evidence = evidence;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s | %s: %s | Evidence: %s",
                severity, category, fileName, description,
                evidence.length() > 80 ? evidence.substring(0, 80) + "..." : evidence);
        }
    }

    // Паттерны угроз
    private static final List<ThreatPattern> PATTERNS = new ArrayList<>();
    static {
        // === СТИЛЕРЫ ТОКЕНОВ ===
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.CRITICAL, "Token Stealer",
            "Чтение лаунчер-аккаунтов Minecraft",
            Pattern.compile("launcher_accounts\\.json", Pattern.CASE_INSENSITIVE)
        ));
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.CRITICAL, "Token Stealer",
            "Чтение .lunarclient/settings",
            Pattern.compile("\\.lunarclient.*settings", Pattern.CASE_INSENSITIVE)
        ));
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.CRITICAL, "Token Stealer",
            "Чтение файлов Discord",
            Pattern.compile("discord.*Local Storage.*https_", Pattern.CASE_INSENSITIVE)
        ));

        // === DISCORD WEBHOOKS ===
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.CRITICAL, "Discord Webhook",
            "Отправка данных на Discord вебхук",
            Pattern.compile("https?://(?:discord(?:app)?\\.com/api/webhooks/|canary\\.discord\\.com/api/webhooks/)\\d+/[\\w\\-]+", Pattern.CASE_INSENSITIVE)
        ));
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.HIGH, "Discord Webhook (Encoded)",
            "Base64-закодированный Discord вебхук",
            Pattern.compile("Base64\\.getDecoder\\(\\)\\.decode\\(", Pattern.CASE_INSENSITIVE)
        ));

        // === СЕТЕВЫЕ УГРОЗЫ ===
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.HIGH, "C2 Communication",
            "Отправка данных на внешний сервер (HTTP POST)",
            Pattern.compile("HttpURLConnection|HttpClient\\.newHttpClient|OkHttpClient|Unirest", Pattern.CASE_INSENSITIVE)
        ));
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.MEDIUM, "Suspicious Connection",
            "Подключение к неизвестному IP/домену",
            Pattern.compile("(Socket|DatagramSocket)\\s*\\(\\s*\"[^\"]*\"\\s*,\\s*\\d+", Pattern.CASE_INSENSITIVE)
        ));

        // === ОБФУСКАЦИЯ ===
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.MEDIUM, "String Obfuscation",
            "Использование обфускации строк",
            Pattern.compile("(xor|XOR|encrypt|decrypt|cipher)\\s*\\(", Pattern.CASE_INSENSITIVE)
        ));
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.LOW, "Reflection Abuse",
            "Использование рефлексии (доступ к приватным полям)",
            Pattern.compile("setAccessible\\s*\\(\\s*true\\s*\\)", Pattern.CASE_INSENSITIVE)
        ));

        // === ВРЕДОНОСНЫЕ ДЕЙСТВИЯ ===
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.CRITICAL, "File Deletion",
            "Удаление файлов игрока",
            Pattern.compile("File\\.delete\\(|Files\\.delete\\(|Runtime\\.getRuntime\\(\\)\\.exec\\(\"rm|exec\\(\"del", Pattern.CASE_INSENSITIVE)
        ));
        PATTERNS.add(new ThreatPattern(
            Threat.Severity.HIGH, "Startup Persistence",
            "Добавление в автозагрузку Windows",
            Pattern.compile("HKEY_CURRENT_USER\\\\\\\\Software\\\\\\\\Microsoft\\\\\\\\Windows\\\\\\\\CurrentVersion\\\\\\\\Run", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * Сканирует все декомпилированные файлы на угрозы.
     */
    public static List<Threat> scanDirectory(java.io.File outputDir) {
        List<Threat> threats = new ArrayList<>();
        scanRecursive(outputDir, outputDir, threats);
        return threats;
    }

    private static void scanRecursive(java.io.File baseDir, java.io.File currentDir, List<Threat> threats) {
        java.io.File[] files = currentDir.listFiles();
        if (files == null) return;
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                scanRecursive(baseDir, file, threats);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    String relativePath = baseDir.toPath().relativize(file.toPath()).toString();
                    for (ThreatPattern pattern : PATTERNS) {
                        Matcher m = pattern.pattern.matcher(content);
                        while (m.find()) {
                            // Захватываем контекст вокруг совпадения
                            int start = Math.max(0, m.start() - 40);
                            int end = Math.min(content.length(), m.end() + 40);
                            String evidence = content.substring(start, end).replace('\n', ' ').replace('\r', ' ');
                            threats.add(new Threat(relativePath, pattern.severity, pattern.category, pattern.description, evidence));
                        }
                    }
                } catch (Exception e) {
                    Logger.error("Failed to scan: " + file.getPath() + " - " + e.getMessage());
                }
            }
        }
    }

    /**
     * Выводит отчёт об угрозах в консоль.
     */
    public static void printReport(List<Threat> threats) {
        if (threats.isEmpty()) {
            Logger.info("[SECURITY] Угроз не обнаружено.");
            return;
        }

        Logger.info("========================================");
        Logger.info("  ОБНАРУЖЕНЫ УГРОЗЫ: " + threats.size());
        Logger.info("========================================");

        // Группируем по серьёзности
        Map<Threat.Severity, List<Threat>> grouped = new TreeMap<>();
        for (Threat t : threats) {
            grouped.computeIfAbsent(t.severity, k -> new ArrayList<>()).add(t);
        }

        for (Threat.Severity sev : Threat.Severity.values()) {
            List<Threat> list = grouped.get(sev);
            if (list == null || list.isEmpty()) continue;

            String color = switch (sev) {
                case CRITICAL -> "!!!";
                case HIGH -> "!!";
                case MEDIUM -> "!";
                case LOW -> ".";
            };
            Logger.info(String.format("\n%s %s (%d):", color, sev, list.size()));
            for (Threat t : list) {
                Logger.info("  " + t.toString());
            }
        }
        Logger.info("========================================");
    }

    private static class ThreatPattern {
        final Threat.Severity severity;
        final String category;
        final String description;
        final Pattern pattern;

        ThreatPattern(Threat.Severity severity, String category, String description, Pattern pattern) {
            this.severity = severity;
            this.category = category;
            this.description = description;
            this.pattern = pattern;
        }
    }
}