package com.tishka1337.decompiler;

import com.tishka1337.security.StealerDetector;
import com.tishka1337.security.StringDecryptor;
import com.tishka1337.utils.Logger;
import com.tishka1337.utils.ProjectRebuilder;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class DecompilerEngine {

    public void decompile(String jarPath, String outputDir, boolean rename, boolean pretty, boolean rebuild, boolean scan) {
        Logger.info("Decompiling: " + jarPath);

        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            Logger.error("Jar file not found: " + jarPath);
            return;
        }

        File outDir = new File(outputDir);
        // Для Razbor — java-файлы кладём прямо в outDir, для Ready — в src/main/java
        File javaOutput = rebuild ? new File(outDir, "src/main/java") : outDir;
        javaOutput.mkdirs();

        try {
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
                    File javaFile = new File(javaOutput, entryName);
                    javaFile.getParentFile().mkdirs();
                    try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(javaFile), "UTF-8");
                         PrintWriter pw = new PrintWriter(osw)) {
                        String decoded = StringDecryptor.decryptStrings(content);
                        pw.print(decoded);
                    } catch (Exception e) {
                        Logger.error("  Failed: " + entryName + " - " + e.getMessage());
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
            File tempDir = new File(outDir, ".temp_classes");
            tempDir.mkdirs();
            int classCount = 0;

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory()) continue;
                    
                    if (name.endsWith(".class")) {
                        File tempClass = new File(tempDir, name);
                        tempClass.getParentFile().mkdirs();
                        try (InputStream is = jar.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(tempClass)) {
                            is.transferTo(fos);
                        }
                        classCount++;
                    } else {
                        // Для Razbor: кладём в outDir (плоская структура как у Vineflower)
                        // Для Ready: кладём в src/main/resources
                        File target;
                        if (rebuild) {
                            target = new File(outDir, "src/main/resources/" + name);
                        } else {
                            target = new File(outDir, name);
                        }
                        target.getParentFile().mkdirs();
                        try (InputStream is = jar.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(target)) {
                            is.transferTo(fos);
                        }
                    }
                }
                
                fernflower.addSource(tempDir);
                Logger.info("Decompiling " + classCount + " classes...");
                fernflower.decompileContext();
                deleteRecursive(tempDir);
            }

            // Только для Ready: генерируем build.gradle и gradlew
            if (rebuild) {
                Logger.info("");
                Logger.info("Rebuilding project structure...");
                ProjectRebuilder rebuilder = new ProjectRebuilder(jarFile, outDir);
                rebuilder.rebuild();
            }

            // Сканирование на угрозы (для всех режимов)
            if (scan) {
                Logger.info("");
                Logger.info("Scanning for threats...");
                List<StealerDetector.Threat> threats = StealerDetector.scanDirectory(outDir);
                StealerDetector.printReport(threats);
            }

            Logger.info("");
            Logger.info("Done! Output: " + outDir.getAbsolutePath());

        } catch (Exception e) {
            Logger.error("Decompilation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) for (File f : files) deleteRecursive(f);
        }
        file.delete();
    }
}