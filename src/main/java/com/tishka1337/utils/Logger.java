package com.tishka1337.utils;

/**
 * Логгер для отладки.
 */
public class Logger {
    public static void info(String msg) {
        System.out.println("[Tishka1337] " + msg);
    }

    public static void error(String msg) {
        System.err.println("[Tishka1337 ERROR] " + msg);
    }
}
