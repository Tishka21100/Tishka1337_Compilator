package com.tishka1337.utils;

import java.util.jar.*;

/**
 * Утилита для работы с jar-файлами.
 */
public class JarHandler {
    public void openJar(String jarPath) {
        try (JarFile jar = new JarFile(jarPath)) {
            // TODO: чтение entries
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
