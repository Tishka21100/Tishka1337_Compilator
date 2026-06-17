package com.tishka1337.analyzer;

import com.tishka1337.utils.Logger;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class RazborAnalyzer {

    public static void analyze(File projectDir) {
        Logger.info("");
        Logger.info("╔══════════════════════════════════════════╗");
        Logger.info("║     TISHKA1337 RAZBOR MODE              ║");
        Logger.info("╚══════════════════════════════════════════╝");

        File javaDir = new File(projectDir, "src/main/java");
        File resourcesDir = new File(projectDir, "src/main/resources");

        // === ФАЗА 1: Сбор статистики ===
        Stats stats = new Stats();
        scanDirectory(javaDir, javaDir, stats);

        // === ФАЗА 2: Загрузка маппингов ===
        Map<String, String> classMappings = new HashMap<>();
        Map<String, String> methodMappings = new HashMap<>();
        Map<String, String> fieldMappings = new HashMap<>();
        loadMappings(resourcesDir, classMappings, methodMappings, fieldMappings);

        // === ФАЗА 3: Деобфускация всех .java файлов ===
        if (!classMappings.isEmpty() || !methodMappings.isEmpty()) {
            Logger.info("");
            Logger.info("Deobfuscating with mappings...");
            int replacedCount = deobfuscateAll(javaDir, classMappings, methodMappings, fieldMappings);
            Logger.info("Replaced " + replacedCount + " obfuscated references!");
        }

        // === ФАЗА 4: Вывод Razbor-бокса ===
        printBox(stats, resourcesDir);
    }

    private static void loadMappings(File resourcesDir, 
                                      Map<String, String> classMappings,
                                      Map<String, String> methodMappings,
                                      Map<String, String> fieldMappings) {
        // Ищем polyak.mixins.json или любой mapping файл
        File[] jsonFiles = resourcesDir.listFiles((d, n) -> n.endsWith(".json") && (n.contains("mixin") || n.contains("mapping")));
        if (jsonFiles == null) return;

        for (File jsonFile : jsonFiles) {
            try {
                String content = Files.readString(jsonFile.toPath());
                
                // Паттерн: "readableName": "Lpackage/class;method()signature"
                Pattern p = Pattern.compile("\"([^\"]+)\":\\s*\"L([^;]+);([^\"]+)\"");
                Matcher m = p.matcher(content);
                
                while (m.find()) {
                    String readable = m.group(1);
                    String className = m.group(2);
                    String memberSig = m.group(3);
                    
                    // Определяем метод или поле
                    if (memberSig.contains("(")) {
                        // Это метод: method_12345()V
                        String methodName = memberSig.replaceAll("\\(.*", "");
                        String obfuscated = className + ";" + methodName;
                        methodMappings.put(obfuscated, readable);
                    } else if (memberSig.contains(":")) {
                        // Это поле: field_12345:LType;
                        String fieldName = memberSig.replaceAll(":.*", "");
                        String obfuscated = className + ";" + fieldName;
                        fieldMappings.put(obfuscated, readable);
                    }
                }

                // Также ищем class_XXXX → readable
                Pattern classP = Pattern.compile("\"([^\"]+)\":\\s*\"Lnet/minecraft/([^\"]+)\"");
                Matcher classM = classP.matcher(content);
                while (classM.find()) {
                    String readable = classM.group(1);
                    String mcClass = classM.group(2);
                    String classNum = mcClass.replaceAll(".*class_(\\d+).*", "class_");
                    if (!classNum.equals(mcClass)) {
                        classMappings.put(classNum, readable);
                    }
                }
                
                Logger.info("  Loaded " + methodMappings.size() + " method mappings");
                Logger.info("  Loaded " + fieldMappings.size() + " field mappings");
            } catch (Exception e) {
                Logger.error("Failed to parse mappings: " + e.getMessage());
            }
        }
    }

    private static int deobfuscateAll(File javaDir, 
                                       Map<String, String> classMappings,
                                       Map<String, String> methodMappings,
                                       Map<String, String> fieldMappings) {
        int total = 0;
        File[] files = javaDir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                total += deobfuscateAll(file, classMappings, methodMappings, fieldMappings);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = Files.readString(file.toPath());
                    String original = content;

                    // Заменяем method_XXXX на читаемые имена
                    for (Map.Entry<String, String> entry : methodMappings.entrySet()) {
                        String[] parts = entry.getKey().split(";");
                        if (parts.length == 2) {
                            String className = parts[0].substring(parts[0].lastIndexOf('/') + 1);
                            String methodName = parts[1];
                            // Заменяем method_12345 на readableName
                            content = content.replaceAll("\\b" + methodName + "\\b", entry.getValue());
                        }
                    }

                    // Заменяем field_XXXX на читаемые имена
                    for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                        String[] parts = entry.getKey().split(";");
                        if (parts.length == 2) {
                            String fieldName = parts[1];
                            content = content.replaceAll("\\b" + fieldName + "\\b", entry.getValue());
                        }
                    }

                    if (!content.equals(original)) {
                        Files.writeString(file.toPath(), content);
                        total++;
                    }
                } catch (Exception ignored) {}
            }
        }
        return total;
    }

    // ... (остальные методы: scanDirectory, detectCategory, extractClassName, printBox — как раньше)
    private static void scanDirectory(File baseDir, File dir, Stats stats) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(baseDir, file, stats);
            } else if (file.getName().endsWith(".java")) {
                stats.totalClasses++;
                try {
                    String content = Files.readString(file.toPath());
                    if (content.contains("extends Module")) {
                        stats.totalModules++;
                        String category = detectCategory(file.getAbsolutePath());
                        String name = extractClassName(content);
                        stats.modulesByCategory
                            .computeIfAbsent(category, k -> new ArrayList<>())
                            .add(name != null ? name : file.getName().replace(".java", ""));
                    }
                    if (content.contains("@Mixin")) stats.totalMixins++;
                    if (content.contains("extends Event") && !content.contains("class Event ")) stats.totalEvents++;
                    if (content.contains("implements ICommand") || content.contains("extends Command")) stats.totalCommands++;
                } catch (Exception ignored) {}
            }
        }
    }

    private static String detectCategory(String path) {
        String lower = path.toLowerCase().replace("\\", "/");
        if (lower.contains("/combat/")) return "Combat";
        if (lower.contains("/movement/")) return "Movement";
        if (lower.contains("/render/")) return "Render";
        if (lower.contains("/player/")) return "Player";
        if (lower.contains("/misc/")) return "Misc";
        return "Other";
    }

    private static String extractClassName(String content) {
        Matcher m = Pattern.compile("class\\s+(\\w+)\\s+extends\\s+Module").matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private static void printBox(Stats stats, File resourcesDir) {
        String modName = "Unknown", modVersion = "?";
        File fabricJson = new File(resourcesDir, "fabric.mod.json");
        if (fabricJson.exists()) {
            try {
                String json = Files.readString(fabricJson.toPath());
                modName = extractJson(json, "name");
                modVersion = extractJson(json, "version");
            } catch (Exception ignored) {}
        }

        Logger.info("╠══════════════════════════════════════════╣");
        Logger.info(String.format("║  Mod: %-22s v%-8s ║", modName, modVersion));
        Logger.info("║  Minecraft: 1.21.4 (Fabric)             ║");
        Logger.info("╠══════════════════════════════════════════╣");
        Logger.info(String.format("║  MODULES: %-3d                           ║", stats.totalModules));
        
        String[] order = {"Combat", "Movement", "Render", "Player", "Misc", "Other"};
        for (String cat : order) {
            List<String> names = stats.modulesByCategory.getOrDefault(cat, Collections.emptyList());
            if (!names.isEmpty()) {
                String sample = String.join(", ", names.subList(0, Math.min(3, names.size())));
                if (names.size() > 3) sample += ", ...";
                Logger.info(String.format("║    %-10s: %-2d  %-22s ║", cat, names.size(), sample));
            }
        }
        
        Logger.info("╠══════════════════════════════════════════╣");
        Logger.info(String.format("║  Mixins: %-3d   Events: %-3d   Cmds: %-3d   ║", 
            stats.totalMixins, stats.totalEvents, stats.totalCommands));
        Logger.info(String.format("║  Total classes: %-3d                     ║", stats.totalClasses));
        
        // Ресурсы
        File[] allFiles = resourcesDir.listFiles((d, n) -> !n.equals("META-INF"));
        if (allFiles != null && allFiles.length > 0) {
            Logger.info("╠══════════════════════════════════════════╣");
            Logger.info("║  Resources:                             ║");
            for (File f : allFiles) {
                if (f.getName().endsWith(".json") || f.getName().endsWith(".accesswidener")) {
                    Logger.info(String.format("║    + %-34s ║", f.getName()));
                }
            }
        }
        Logger.info("╚══════════════════════════════════════════╝");
    }

    private static String extractJson(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return "?";
        start = json.indexOf("\"", start + key.length() + 2) + 1;
        int end = json.indexOf("\"", start);
        return end == -1 ? "?" : json.substring(start, end);
    }

    static class Stats {
        int totalClasses = 0, totalModules = 0, totalMixins = 0, totalEvents = 0, totalCommands = 0;
        Map<String, List<String>> modulesByCategory = new LinkedHashMap<>();
    }
}