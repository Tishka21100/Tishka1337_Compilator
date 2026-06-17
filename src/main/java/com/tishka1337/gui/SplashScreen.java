package com.tishka1337.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;

/**
 * Сплэш-скрин с анимированными толстыми 1337.
 * Затухает и исчезает через 2.5 секунды.
 */
public class SplashScreen extends JWindow {

    private float opacity = 1.0f;
    private double time = 0;
    private final Timer animationTimer;
    private final Timer fadeTimer;
    private Runnable onComplete;

    public SplashScreen(Runnable onComplete) {
        this.onComplete = onComplete;
        
        setSize(500, 300);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                // Тёмный фон
                g2.setColor(new Color(14, 14, 18));
                g2.fillRect(0, 0, w, h);

                // Сетка
                g2.setColor(new Color(30, 30, 36, 40));
                for (int x = 0; x < w; x += 25) g2.drawLine(x, 0, x, h);
                for (int y = 0; y < h; y += 25) g2.drawLine(0, y, w, y);

                // Переливающиеся 1337
                float hue1 = (float) ((Math.sin(time * 0.8) + 1) / 2);
                float hue2 = (float) ((Math.sin(time * 0.8 + 2.0) + 1) / 2);
                
                Color c1 = Color.getHSBColor(hue1 * 0.06f + 0.56f, 0.25f, 0.9f);
                Color c2 = Color.getHSBColor(hue2 * 0.06f + 0.58f, 0.2f, 0.95f);

                Font font = new Font("Consolas", Font.BOLD, 80);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                String text = "1337";
                int textWidth = fm.stringWidth(text);
                int textX = (w - textWidth) / 2;
                int textY = h / 2 + fm.getAscent() / 2 - 5;

                // Тень
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawString(text, textX + 3, textY + 3);

                // Основной текст
                GradientPaint textGrad = new GradientPaint(textX, textY - 30, c1, textX, textY + 10, c2);
                g2.setPaint(textGrad);
                g2.drawString(text, textX, textY);

                // Блик
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawString(text, textX - 1, textY - 1);

                // Подпись
                Font smallFont = new Font("Consolas", Font.PLAIN, 13);
                g2.setFont(smallFont);
                fm = g2.getFontMetrics();
                String subtitle = "Tishka1337 Decompiler";
                int subWidth = fm.stringWidth(subtitle);
                g2.setColor(new Color(130, 130, 145));
                g2.drawString(subtitle, (w - subWidth) / 2, textY + 35);

                String author = "by InkTank1337";
                int authWidth = fm.stringWidth(author);
                g2.setColor(new Color(100, 100, 115));
                g2.drawString(author, (w - authWidth) / 2, textY + 55);

                // Глитч-линии
                if (Math.random() < 0.15) {
                    g2.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 50));
                    int glitchY = (int) (Math.random() * h);
                    g2.fillRect(0, glitchY, w, 2);
                }

                // Прогресс-бар снизу
                g2.setColor(new Color(50, 50, 55));
                g2.fillRoundRect(100, h - 25, 300, 4, 2, 2);
                g2.setColor(c1);
                g2.fillRoundRect(100, h - 25, (int) (300 * (time / 2.5)), 4, 2, 2);

                g2.dispose();
            }
        };
        panel.setBackground(new Color(14, 14, 18));
        setContentPane(panel);

        // Анимация
        animationTimer = new Timer(30, e -> {
            time += 0.03;
            repaint();
        });
        animationTimer.start();

        // Затухание и закрытие
        fadeTimer = new Timer(50, new ActionListener() {
            int tick = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                tick++;
                if (tick > 20) { // через 1 секунду начинаем затухать
                    opacity -= 0.05f;
                    if (opacity <= 0) {
                        opacity = 0;
                        fadeTimer.stop();
                        animationTimer.stop();
                        setVisible(false);
                        dispose();
                        if (onComplete != null) onComplete.run();
                    }
                    setOpacity(Math.max(0, opacity));
                }
            }
        });
        
        // Запускаем затухание через 2.5 секунды
        Timer delayTimer = new Timer(2500, e -> {
            ((Timer) e.getSource()).stop();
            fadeTimer.start();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
}