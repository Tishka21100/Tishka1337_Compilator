package com.tishka1337.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class ProjectRebuilder {

    private final File jarFile;
    private final File outputDir;
    private final File javaDir;
    private final File resourcesDir;

    private String mcVersion = "1.21.4";
    private String yarnVersion = "1.21.4+build.1";
    private String loaderVersion = "0.16.9";
    private String fabricApiVersion = "0.104.0+1.21.4";
    private String modId = "unknownmod";
    private String modName = "Unknown Mod";
    private String modVersion = "1.0.0";
    private String mainClass = null;

    private boolean isFabric = false;
    private boolean isForge = false;
    private boolean hasMixins = false;
    private String mixinConfig = null;

    public ProjectRebuilder(File jarFile, File outputDir) {
        this.jarFile = jarFile;
        this.outputDir = outputDir;
        this.javaDir = new File(outputDir, "src/main/java");
        this.resourcesDir = new File(outputDir, "src/main/resources");
    }

    public void rebuild() throws Exception {
        Logger.info("Rebuilding project from: " + jarFile.getName());
        javaDir.mkdirs();
        resourcesDir.mkdirs();

        Set<String> resourceExtensions = Set.of(
            "json", "png", "jpg", "jpeg", "gif", "bmp",
            "fxml", "css", "xml", "properties",
            "cfg", "conf", "txt", "md",
            "mcmeta", "info", "lang", "accesswidener"
        );

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (name.endsWith(".class")) continue;

                String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
                if (resourceExtensions.contains(ext) || name.startsWith("META-INF/") || name.equals("pack.mcmeta") || name.endsWith(".accesswidener")) {
                    File target = new File(resourcesDir, name);
                    target.getParentFile().mkdirs();
                    try (InputStream is = jar.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(target)) {
                        is.transferTo(fos);
                    }
                }
            }
        }

        File fabricModJson = new File(resourcesDir, "fabric.mod.json");
        if (fabricModJson.exists()) {
            isFabric = true;
            parseFabricModJson(fabricModJson);
        }

        File[] mixinConfigs = resourcesDir.listFiles((dir, name) -> 
            name.endsWith(".json") && (name.contains("mixin") || name.contains("mixins")));
        if (mixinConfigs != null && mixinConfigs.length > 0) {
            hasMixins = true;
            mixinConfig = mixinConfigs[0].getName();
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (name.endsWith(".class")) continue;

                String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
                if (resourceExtensions.contains(ext) || name.startsWith("META-INF/")) {
                    File target = new File(resourcesDir, name);
                    if (!target.exists()) {
                        target.getParentFile().mkdirs();
                        try (InputStream is = jar.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(target)) {
                            is.transferTo(fos);
                        }
                    }
                }
            }
        }

        if (isFabric) {
            generateFabricBuildGradle();
        } else {
            generateForgeBuildGradle();
        }
        generateGradlew();
        generateGradleProperties();
        generateSettingsGradle();

        Logger.info("Project rebuilt: " + (isFabric ? "Fabric" : "Forge") + " " + mcVersion);
        Logger.info("Output: " + outputDir.getAbsolutePath());
    }

    private void parseFabricModJson(File jsonFile) {
        try {
            String content = Files.readString(jsonFile.toPath());
            modId = extractJsonValue(content, "id");
            modName = extractJsonValue(content, "name");
            modVersion = extractJsonValue(content, "version");
            
            if (content.contains("\"entrypoints\"")) {
                int entryStart = content.indexOf("\"main\"");
                if (entryStart != -1) {
                    int valStart = content.indexOf("[", entryStart) + 1;
                    int valEnd = content.indexOf("]", valStart);
                    String mainEntry = content.substring(valStart, valEnd)
                        .replace("\"", "").replace(",", "").trim();
                    mainClass = mainEntry;
                }
            }
            
            if (content.contains("\"mixins\"")) {
                hasMixins = true;
                int mixStart = content.indexOf("\"mixins\"");
                int arrStart = content.indexOf("[", mixStart) + 1;
                int arrEnd = content.indexOf("]", arrStart);
                String mixEntry = content.substring(arrStart, arrEnd)
                    .replace("\"", "").replace(",", "").trim();
                mixinConfig = mixEntry;
            }

            Logger.info("  Fabric mod: " + modName + " v" + modVersion + " (id: " + modId + ")");
            if (mainClass != null) Logger.info("  Main class: " + mainClass);
        } catch (Exception e) {
            Logger.error("Failed to parse fabric.mod.json: " + e.getMessage());
        }
    }

    private String extractJsonValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return null;
        start = json.indexOf("\"", start + key.length() + 2) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private void generateFabricBuildGradle() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by Tishka1337 by InkTank1337\n");
        sb.append("// Fabric ").append(mcVersion).append(" project\n\n");
        sb.append("plugins {\n");
        sb.append("    id 'fabric-loom' version '1.7-SNAPSHOT'\n");
        sb.append("    id 'maven-publish'\n");
        sb.append("}\n\n");
        sb.append("version = '").append(modVersion != null ? modVersion : "1.0.0").append("'\n");
        sb.append("group = '").append(modId).append("'\n\n");
        sb.append("repositories {\n");
        sb.append("    mavenCentral()\n");
        sb.append("    maven { url = 'https://jitpack.io' }\n");
        sb.append("}\n\n");
        sb.append("dependencies {\n");
        sb.append("    minecraft 'com.mojang:minecraft:").append(mcVersion).append("'\n");
        sb.append("    mappings 'net.fabricmc:yarn:").append(yarnVersion).append(":v2'\n");
        sb.append("    modImplementation 'net.fabricmc:fabric-loader:").append(loaderVersion).append("'\n");
        sb.append("    modImplementation 'net.fabricmc.fabric-api:fabric-api:").append(fabricApiVersion).append("'\n");
        sb.append("}\n\n");
        sb.append("processResources {\n");
        sb.append("    inputs.property 'version', project.version\n");
        sb.append("    filesMatching('fabric.mod.json') {\n");
        sb.append("        expand 'version': project.version\n");
        sb.append("    }\n");
        sb.append("}\n");

        Files.writeString(new File(outputDir, "build.gradle").toPath(), sb.toString());
        Logger.info("  Generated: build.gradle (Fabric)");
    }

    private void generateForgeBuildGradle() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("buildscript {\n");
        sb.append("    repositories {\n");
        sb.append("        maven { url = 'https://maven.minecraftforge.net/' }\n");
        sb.append("        mavenCentral()\n");
        sb.append("    }\n");
        sb.append("    dependencies {\n");
        sb.append("        classpath 'net.minecraftforge.gradle:ForgeGradle:5.1.+'\n");
        if (hasMixins) sb.append("        classpath 'org.spongepowered:mixingradle:0.7-+'\n");
        sb.append("    }\n");
        sb.append("}\n\n");
        sb.append("apply plugin: 'net.minecraftforge.gradle'\n");
        if (hasMixins) sb.append("apply plugin: 'org.spongepowered.mixin'\n");
        sb.append("\n");
        sb.append("version = '").append(modVersion).append("'\n");
        sb.append("group = 'com.example'\n\n");
        sb.append("minecraft {\n");
        sb.append("    mappings channel: 'official', version: '").append(mcVersion).append("'\n");
        sb.append("}\n\n");
        sb.append("dependencies {\n");
        sb.append("    minecraft 'net.minecraftforge:forge:").append(mcVersion).append("-").append("51.0.0").append("'\n");
        sb.append("}\n");

        Files.writeString(new File(outputDir, "build.gradle").toPath(), sb.toString());
        Logger.info("  Generated: build.gradle (Forge)");
    }

    /**
     * Генерирует скрипты Gradle Wrapper (gradlew, gradlew.bat, wrapper jar).
     */
        private void generateGradlew() throws IOException {
        String gradlew = "#!/bin/sh\n" +
            "# Generated by Tishka1337 by InkTank1337\n" +
            "export GRADLE_USER_HOME=\"$HOME/.gradle\"\n" +
            "exec gradle \"$@\"\n";
        Files.writeString(new File(outputDir, "gradlew").toPath(), gradlew);
        new File(outputDir, "gradlew").setExecutable(true);

        String gradlewBat = "@echo off\r\n" +
            ":: Generated by Tishka1337 by InkTank1337\r\n" +
            "set GRADLE_USER_HOME=%USERPROFILE%\\.gradle\r\n" +
            "gradle %*\r\n";
        Files.writeString(new File(outputDir, "gradlew.bat").toPath(), gradlewBat);

        File wrapperDir = new File(outputDir, "gradle/wrapper");
        wrapperDir.mkdirs();
        String wrapperProps = "distributionBase=GRADLE_USER_HOME\n" +
            "distributionPath=wrapper/dists\n" +
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10-bin.zip\n" +
            "zipStoreBase=GRADLE_USER_HOME\n" +
            "zipStorePath=wrapper/dists\n";
        Files.writeString(new File(wrapperDir, "gradle-wrapper.properties").toPath(), wrapperProps);

        Logger.info("  Generated: gradlew, gradlew.bat, gradle-wrapper.properties");
    }

    private void generateGradleProperties() throws IOException {
        String props = "# Generated by Tishka1337 by InkTank1337\n";
        props += "org.gradle.jvmargs=-Xmx3G\n";
        props += "org.gradle.daemon=false\n";
        Files.writeString(new File(outputDir, "gradle.properties").toPath(), props);
    }

    private void generateSettingsGradle() throws IOException {
        String settings = "rootProject.name = '" + modId + "'\n";
        Files.writeString(new File(outputDir, "settings.gradle").toPath(), settings);
    }
}