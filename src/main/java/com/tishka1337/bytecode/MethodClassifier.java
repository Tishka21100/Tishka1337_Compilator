package com.tishka1337.bytecode;

/**
 * Классификатор методов: определяет назначение метода (KillAura, Scaffold, ESP...).
 */
public class MethodClassifier {
    public enum MethodType {
        KILL_AURA, SCAFFOLD, ESP, VELOCITY, UNKNOWN
    }

    public MethodType classify(String className, String methodName) {
        // TODO: ML-классификация или анализ вызовов
        return MethodType.UNKNOWN;
    }
}
