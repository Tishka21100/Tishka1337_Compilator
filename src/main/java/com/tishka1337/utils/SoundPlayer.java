package com.tishka1337.utils;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

/**
 * Проигрывает звук при успешном завершении декомпиляции.
 * Генерирует звук программно (без внешних файлов).
 */
public class SoundPlayer {

    private static final int SAMPLE_RATE = 44100;

    /**
     * Играет киберпанк-звук успеха (три восходящих тона).
     */
    public static void playSuccessSound() {
        new Thread(() -> {
            try {
                // Три тона: низкий → средний → высокий
                float[] frequencies = {523.25f, 659.25f, 783.99f}; // C5, E5, G5
                float duration = 0.12f;
                
                for (float freq : frequencies) {
                    playTone(freq, duration);
                    Thread.sleep(60);
                }
            } catch (Exception e) {
                // Звук не критичен
            }
        }).start();
    }

    private static void playTone(float frequency, float duration) throws Exception {
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] buffer = new byte[numSamples * 2];
        
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / SAMPLE_RATE;
            // Затухание (fade out)
            double envelope = 1.0 - (double) i / numSamples;
            short sample = (short) (Math.sin(angle) * 32767 * 0.6 * envelope);
            buffer[i * 2] = (byte) (sample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        AudioInputStream ais = new AudioInputStream(bais, format, numSamples);
        
        Clip clip = AudioSystem.getClip();
        clip.open(ais);
        clip.start();
        
        // Ждём окончания
        Thread.sleep((long) (duration * 1000) + 50);
        clip.close();
    }
}