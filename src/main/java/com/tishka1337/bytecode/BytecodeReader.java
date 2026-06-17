package com.tishka1337.bytecode;

import org.objectweb.asm.*;

/**
 * Читает байткод классов через ASM.
 */
public class BytecodeReader {
    public void parseClass(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        // TODO: обход методов, полей, аннотаций
    }
}
