package com.tishka1337.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.Random;

/**
 * Панель с анимированными переливающимися цифрами 1337.
 * Толстый шрифт, глянцевый эффект, случайные вспышки.
 */
public class GlitchPanel extends JPanel {

    private final Timer timer;
    private float hue = 0f;
    private float glitchOffset = 0f;
    private final Random random = new Random();
    private double time = 0;

    public GlitchPanel() {
        setBackground(new Color(20, 20, 22));
        setPreferredSize(new Dimension(500, 150));
        
        timer = new Timer(30, e -> {
            time += 0.03;
            hue += 0.002f;
            if (hue > 1f) hue = 0f;
            glitchOffset = (random.nextFloat() - 0.5f) * 3f;
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Тёмный фон с градиентом
        GradientPaint bgGrad = new GradientPaint(0, 0, new Color(18, 18, 20), 0, h, new Color(28, 28, 32));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, w, h);

        // Сетка на фоне (киберпанк-стиль)
        g2.setColor(new Color(40, 40, 45, 60));
        for (int x = 0; x < w; x += 30) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 30) {
            g2.drawLine(0, y, w, y);
        }

        // Переливающийся градиент для текста
        float h1 = (float) ((Math.sin(time * 0.7) + 1) / 2);
        float h2 = (float) ((Math.sin(time * 0.7 + 1.5) + 1) / 2);
        
        Color c1 = Color.getHSBColor(h1 * 0.08f + 0.55f, 0.3f, 0.85f);
        Color c2 = Color.getHSBColor(h2 * 0.08f + 0.58f, 0.2f, 0.95f);

        // Основной текст "1337"
        Font font = new Font("Consolas", Font.BOLD, 72);
        g2.setFont(font);
        
        FontMetrics fm = g2.getFontMetrics();
        String text = "1337";
        int textWidth = fm.stringWidth(text);
        int textX = (w - textWidth) / 2 + (int) glitchOffset;
        int textY = h / 2 + fm.getAscent() / 2 - 10;

        // Тень
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(text, textX + 3, textY + 3);

        // Основной текст с градиентом
        GradientPaint textGrad = new GradientPaint(textX, textY - 30, c1, textX, textY + 10, c2);
        g2.setPaint(textGrad);
        g2.drawString(text, textX, textY);

        // Глянцевый блик сверху
        g2.setColor(new Color(255, 255, 255, 20));
        g2.drawString(text, textX - 1, textY - 1);

        // Случайные глитч-линии
        if (random.nextFloat() < 0.1f) {
            g2.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 60));
            int glitchY = random.nextInt(h);
            g2.fillRect(0, glitchY, w, random.nextInt(3) + 1);
        }

        // Подпись "Tishka1337"
        Font smallFont = new Font("Consolas", Font.PLAIN, 14);
        g2.setFont(smallFont);
        fm = g2.getFontMetrics();
        String subtitle = "by InkTank1337";
        int subWidth = fm.stringWidth(subtitle);
        g2.setColor(new Color(140, 140, 150));
        g2.drawString(subtitle, (w - subWidth) / 2, textY + 30);

        // Версия
        String version = "v1.0.0";
        int verWidth = fm.stringWidth(version);
        g2.setColor(new Color(100, 100, 110));
        g2.drawString(version, (w - verWidth) / 2, textY + 50);

        g2.dispose();
    }
}