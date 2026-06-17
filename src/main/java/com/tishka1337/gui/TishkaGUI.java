package com.tishka1337.gui;

import com.tishka1337.decompiler.DecompilerEngine;
import com.tishka1337.analyzer.RazborEngine;
import com.tishka1337.utils.Logger;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.*;

/**
 * Главное окно Tishka1337 Decompiler.
 * Монохромный киберпанк-дизайн.
 */
public class TishkaGUI extends JFrame {

    private JTextField jarPathField;
    private JTextField outputPathField;
    private JComboBox<String> modeCombo;
    private JTextArea logArea;
    private JButton decompileButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JPanel historyPanel;

    private final Preferences prefs = Preferences.userNodeForPackage(TishkaGUI.class);

    public TishkaGUI() {
        setupLookAndFeel();
        setupUI();
        setupHistory();
        setLocationRelativeTo(null);
    }

    private void setupLookAndFeel() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ProgressBar.arc", 6);
            
            // Монохромная палитра
            UIManager.put("Panel.background", new Color(24, 24, 28));
            UIManager.put("TextField.background", new Color(32, 32, 36));
            UIManager.put("TextArea.background", new Color(20, 20, 24));
            UIManager.put("ComboBox.background", new Color(32, 32, 36));
            UIManager.put("Button.background", new Color(45, 45, 50));
            UIManager.put("Button.hoverBackground", new Color(55, 55, 60));
            UIManager.put("ScrollPane.background", new Color(20, 20, 24));
            
            UIManager.put("TextField.foreground", new Color(200, 200, 210));
            UIManager.put("TextArea.foreground", new Color(180, 180, 190));
            UIManager.put("Label.foreground", new Color(160, 160, 170));
            UIManager.put("ComboBox.foreground", new Color(200, 200, 210));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        setTitle("Tishka1337 Decompiler by InkTank1337");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setMinimumSize(new Dimension(700, 500));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(24, 24, 28));

        // Верх — GlitchPanel
        GlitchPanel glitchPanel = new GlitchPanel();
        glitchPanel.setPreferredSize(new Dimension(900, 150));
        mainPanel.add(glitchPanel, BorderLayout.NORTH);

        // Центр — настройки + лог
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBackground(new Color(24, 24, 28));

        // Панель настроек
        JPanel settingsPanel = createSettingsPanel();
        centerPanel.add(settingsPanel, BorderLayout.NORTH);

        // Лог
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(20, 20, 24));
        logArea.setForeground(new Color(160, 160, 170));
        logArea.setCaretColor(new Color(160, 160, 170));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 45)));
        logScroll.setPreferredSize(new Dimension(500, 200));
        centerPanel.add(logScroll, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Низ — прогресс + статус + история
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBackground(new Color(24, 24, 28));
        bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(100, 100, 110));
        progressBar.setBackground(new Color(32, 32, 36));
        progressBar.setPreferredSize(new Dimension(500, 20));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(120, 120, 130));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(24, 24, 28));
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(statusLabel, BorderLayout.EAST);

        // Панель истории
        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.X_AXIS));
        historyPanel.setBackground(new Color(24, 24, 28));
        JLabel historyLabel = new JLabel("Recent: ");
        historyLabel.setFont(new Font("Consolas", Font.PLAIN, 10));
        historyLabel.setForeground(new Color(100, 100, 110));
        historyPanel.add(historyLabel);

        bottomPanel.add(statusPanel, BorderLayout.CENTER);
        bottomPanel.add(historyPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(28, 28, 32));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 40, 45)),
            new EmptyBorder(10, 10, 10, 10)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Jar path
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel jarLabel = new JLabel("Jar File:");
        jarLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        panel.add(jarLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        jarPathField = new JTextField(prefs.get("lastJar", ""));
        jarPathField.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(jarPathField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseJar = new JButton("...");
        browseJar.setFont(new Font("Consolas", Font.BOLD, 12));
        browseJar.addActionListener(e -> browseJar());
        panel.add(browseJar, gbc);

        // Output path
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel outLabel = new JLabel("Output:");
        outLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        panel.add(outLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        outputPathField = new JTextField(prefs.get("lastOutput", "./output"));
        outputPathField.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(outputPathField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseOut = new JButton("...");
        browseOut.setFont(new Font("Consolas", Font.BOLD, 12));
        browseOut.addActionListener(e -> browseOutput());
        panel.add(browseOut, gbc);

        // Mode
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel modeLabel = new JLabel("Mode:");
        modeLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        panel.add(modeLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        modeCombo = new JComboBox<>(new String[]{
            "Ready — Full Gradle project",
            "NoReady — Sources only",
            "Razbor — Pure flat + analysis"
        });
        modeCombo.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(modeCombo, gbc);

        // Decompile button
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0;
        decompileButton = new JButton("DECOMPILE");
        decompileButton.setFont(new Font("Consolas", Font.BOLD, 14));
        decompileButton.setBackground(new Color(50, 50, 55));
        decompileButton.setForeground(new Color(180, 180, 190));
        decompileButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 75)),
            new EmptyBorder(8, 25, 8, 25)
        ));
        decompileButton.addActionListener(e -> startDecompile());
        decompileButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                decompileButton.setBackground(new Color(65, 65, 70));
                decompileButton.setForeground(new Color(220, 220, 230));
            }
            public void mouseExited(MouseEvent e) {
                decompileButton.setBackground(new Color(50, 50, 55));
                decompileButton.setForeground(new Color(180, 180, 190));
            }
        });
        panel.add(decompileButton, gbc);

        return panel;
    }

    private void browseJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".jar"); }
            public String getDescription() { return "JAR files (*.jar)"; }
        });
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startDecompile() {
        String jarPath = jarPathField.getText().trim();
        String outputPath = outputPathField.getText().trim();
        int mode = modeCombo.getSelectedIndex();

        if (jarPath.isEmpty() || !new File(jarPath).exists()) {
            JOptionPane.showMessageDialog(this, "Invalid jar file!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Сохраняем в историю
        prefs.put("lastJar", jarPath);
        prefs.put("lastOutput", outputPath);
        addToHistory(jarPath);

        decompileButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Decompiling...");
        logArea.setText("");

        new Thread(() -> {
            try {
                switch (mode) {
                    case 0: // Ready
                        DecompilerEngine de = new DecompilerEngine();
                        de.decompile(jarPath, outputPath, false, false, true, true);
                        break;
                    case 1: // NoReady
                        DecompilerEngine de2 = new DecompilerEngine();
                        de2.decompile(jarPath, outputPath, false, false, false, false);
                        break;
                    case 2: // Razbor
                        RazborEngine razbor = new RazborEngine(new File(jarPath), new File(outputPath));
                        razbor.execute();
                        break;
                }
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Done: " + outputPath);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    decompileButton.setEnabled(true);
                    logArea.append("\n✓ Decompilation complete!\n");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    progressBar.setIndeterminate(false);
                    decompileButton.setEnabled(true);
                    logArea.append("\n✗ Error: " + ex.getMessage() + "\n");
                });
            }
        }).start();
    }

    private void setupHistory() {
        String history = prefs.get("history", "");
        if (!history.isEmpty()) {
            String[] items = history.split("\\|");
            for (int i = items.length - 1; i >= Math.max(0, items.length - 3); i--) {
                addToHistoryPanel(items[i]);
            }
        }
    }

    private void addToHistory(String path) {
        String history = prefs.get("history", "");
        history = history + "|" + path;
        // Оставляем только последние 5
        String[] items = history.split("\\|");
        if (items.length > 5) {
            StringBuilder sb = new StringBuilder();
            for (int i = items.length - 5; i < items.length; i++) {
                if (sb.length() > 0) sb.append("|");
                sb.append(items[i]);
            }
            history = sb.toString();
        }
        prefs.put("history", history);
        addToHistoryPanel(path);
    }

    private void addToHistoryPanel(String path) {
        File f = new File(path);
        JButton btn = new JButton(f.getName());
        btn.setFont(new Font("Consolas", Font.PLAIN, 10));
        btn.setBackground(new Color(35, 35, 38));
        btn.setForeground(new Color(140, 140, 150));
        btn.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55)));
        btn.setToolTipText(path);
        btn.addActionListener(e -> jarPathField.setText(path));
        historyPanel.add(btn);
        historyPanel.revalidate();
    }

    // Перехватываем вывод Logger'а в logArea
    static {
        // Перенаправляем System.out в logArea (будет сделано при создании GUI)
    }
}