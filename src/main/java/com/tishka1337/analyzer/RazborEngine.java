package com.tishka1337.analyzer;

import com.tishka1337.security.StealerDetector;
import com.tishka1337.security.StringDecryptor;
import com.tishka1337.utils.Logger;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

/**
 * RAZBOR ENGINE — ЧИСТЫЙ РАЗБОР БЕЗ BUILD.GRADLE!
 * Плоская структура как у Vineflower.
 * Все ресурсы извлекаются 1-в-1.
 * Деобфускация по mapping-файлам.
 * Полная статистика модулей, миксинов, событий.
 */
public class RazborEngine {

    private final File jarFile;
    private final File outputDir;
    private final Map<String, String> methodMappings = new HashMap<>();
    private final Map<String, String> fieldMappings = new HashMap<>();

    public RazborEngine(File jarFile, File outputDir) {
        this.jarFile = jarFile;
        this.outputDir = outputDir;
    }

    public void execute() throws Exception {
        outputDir.mkdirs();

        // Фаза 1: Извлекаем ВСЕ не-class файлы (плоская структура)
        extractAllResources();

        // Фаза 2: Декомпилируем классы
        decompileClasses();

        // Фаза 3: Загружаем маппинги и деобфусцируем
        loadMappingsAndDeobfuscate();

        // Фаза 4: Глубокий анализ
        printRazborReport();
    }

    private void extractAllResources() throws Exception {
        Logger.info("Extracting all resources...");
        int count = 0;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (name.endsWith(".class")) continue;

                File target = new File(outputDir, name);
                target.getParentFile().mkdirs();
                try (InputStream is = jar.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(target)) {
                    is.transferTo(fos);
                }
                count++;
            }
        }
        Logger.info("  Extracted " + count + " resource files");
    }

    private void decompileClasses() throws Exception {
        Logger.info("Decompiling classes...");

        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
        options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
        options.put(IFernflowerPreferences.DECOMPILE_INNER, "1");
        options.put(IFernflowerPreferences.LOG_LEVEL, "warn");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "1");
        options.put(IFernflowerPreferences.LITERALS_AS_IS, "1");
        options.put(IFernflowerPreferences.INDENT_STRING, "    ");

        IResultSaver saver = new IResultSaver() {
            @Override public void saveFolder(String path) {}
            @Override public void copyFile(String source, String path, String entryName) {}
            @Override public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
                File javaFile = new File(outputDir, entryName);
                javaFile.getParentFile().mkdirs();
                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(javaFile), "UTF-8");
                     PrintWriter pw = new PrintWriter(osw)) {
                    pw.print(StringDecryptor.decryptStrings(content));
                } catch (Exception e) {
                    Logger.error("  Failed: " + entryName);
                }
            }
            @Override public void createArchive(String path, String archiveName, Manifest manifest) {}
            @Override public void saveDirEntry(String path, String archiveName, String entryName) {}
            @Override public void copyEntry(String source, String path, String archiveName, String entryName) {}
            @Override public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
                saveClassFile(path, qualifiedName, entryName, content, null);
            }
            @Override public void closeArchive(String path, String archiveName) {}
        };

        IFernflowerLogger fernLogger = new IFernflowerLogger() {
            @Override public void writeMessage(String message, Severity severity) {
                if (severity == Severity.ERROR) Logger.error(message);
                else if (severity == Severity.WARN) Logger.info("[WARN] " + message);
            }
            @Override public void writeMessage(String message, Severity severity, Throwable t) {
                writeMessage(message + ": " + t.getMessage(), severity);
            }
        };

        Fernflower fernflower = new Fernflower(saver, options, fernLogger);
        File tempDir = new File(outputDir, ".temp_classes");
        tempDir.mkdirs();
        int classCount = 0;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    File tempClass = new File(tempDir, entry.getName());
                    tempClass.getParentFile().mkdirs();
                    try (InputStream is = jar.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(tempClass)) {
                        is.transferTo(fos);
                    }
                    classCount++;
                }
            }
            fernflower.addSource(tempDir);
            Logger.info("  Decompiling " + classCount + " classes...");
            fernflower.decompileContext();
            deleteRecursive(tempDir);
        }
    }

    private void loadMappingsAndDeobfuscate() {
        Logger.info("Loading mappings and deobfuscating...");
        
        // Ищем mapping-файлы
        File[] jsonFiles = outputDir.listFiles((d, n) -> 
            n.endsWith(".json") && (n.contains("mixin") || n.contains("mapping") || n.contains("refmap")));
        
        if (jsonFiles != null) {
            for (File jsonFile : jsonFiles) {
                try {
                    String content = Files.readString(jsonFile.toPath());
                    Pattern p = Pattern.compile("\"([^\"]+)\":\\s*\"L([^;]+);([^\"]+)\"");
                    Matcher m = p.matcher(content);
                    while (m.find()) {
                        String memberSig = m.group(3);
                        String readable = m.group(1);
                        if (memberSig.contains("(")) {
                            methodMappings.put(memberSig.replaceAll("\\(.*", ""), readable);
                        } else if (memberSig.contains(":")) {
                            fieldMappings.put(memberSig.replaceAll(":.*", ""), readable);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        Logger.info("  Method mappings: " + methodMappings.size());
        Logger.info("  Field mappings: " + fieldMappings.size());

        // Применяем ко всем .java файлам
        int replacedCount = 0;
        File[] javaFiles = outputDir.listFiles((d, n) -> n.endsWith(".java"));
        if (javaFiles != null) {
            replacedCount = deobfuscateFiles(outputDir);
        }
        Logger.info("  Deobfuscated " + replacedCount + " references");
    }

    private int deobfuscateFiles(File dir) {
        int total = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isDirectory()) {
                total += deobfuscateFiles(file);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = Files.readString(file.toPath());
                    String original = content;
                    for (Map.Entry<String, String> e : methodMappings.entrySet()) {
                        content = content.replaceAll("\\b" + e.getKey() + "\\b", e.getValue());
                    }
                    for (Map.Entry<String, String> e : fieldMappings.entrySet()) {
                        content = content.replaceAll("\\b" + e.getKey() + "\\b", e.getValue());
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

    private void printRazborReport() {
        Logger.info("");
        Logger.info("╔══════════════════════════════════════════╗");
        Logger.info("║        TISHKA1337 RAZBOR REPORT         ║");
        Logger.info("╠══════════════════════════════════════════╣");

        // Статистика
        RazborAnalyzer.analyze(outputDir);

        // Сканирование угроз
        Logger.info("");
        Logger.info("Scanning for threats...");
        List<StealerDetector.Threat> threats = StealerDetector.scanDirectory(outputDir);
        StealerDetector.printReport(threats);
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) for (File f : files) deleteRecursive(f);
        }
        file.delete();
    }
}