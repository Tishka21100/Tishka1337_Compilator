package com.tishka1337.security;

import java.util.Base64;
import java.util.regex.*;

/**
 * Расшифровщик обфусцированных строк.
 * Находит и декодирует Base64, XOR, и другие методы обфускации.
 */
public class StringDecryptor {

    /**
     * Ищет и расшифровывает обфусцированные строки в коде.
     */
    public static String decryptStrings(String javaCode) {
        StringBuilder result = new StringBuilder();
        String[] lines = javaCode.split("\n");
        boolean foundAny = false;

        for (String line : lines) {
            String decrypted = line;

            // Пытаемся найти Base64 строки
            Matcher b64 = Pattern.compile("\"([A-Za-z0-9+/=]{20,})\"").matcher(line);
            while (b64.find()) {
                String candidate = b64.group(1);
                try {
                    byte[] decoded = Base64.getDecoder().decode(candidate);
                    String decodedStr = new String(decoded, "UTF-8");
                    // Проверяем, что это читаемый текст (не бинарные данные)
                    if (isReadable(decodedStr) && !candidate.equals(decodedStr)) {
                        decrypted = decrypted.replace("\"" + candidate + "\"",
                            "\"" + decodedStr + "\" /* [Tishka1337 decoded] */");
                        foundAny = true;
                    }
                } catch (Exception ignored) {}
            }

            // Ищем XOR с константой
            Matcher xor = Pattern.compile("\\((\\w+)\\s*\\^\\s*(0x[0-9a-fA-F]+|\\d+)\\)").matcher(line);
            while (xor.find()) {
                decrypted = decrypted.replace(xor.group(), xor.group() + " /* [Tishka1337: XOR detected] */");
                foundAny = true;
            }

            result.append(decrypted).append("\n");
        }

        return result.toString();
    }

    /**
     * Проверяет, является ли строка читаемым текстом.
     */
    private static boolean isReadable(String str) {
        if (str == null || str.isEmpty()) return false;
        int printable = 0;
        for (char c : str.toCharArray()) {
            if (c >= 32 && c < 127) printable++;
        }
        return (double) printable / str.length() > 0.8;
    }
}