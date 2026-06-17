package com.tishka1337;

import com.tishka1337.decompiler.DecompilerEngine;
import com.tishka1337.analyzer.*;
import com.tishka1337.gui.SplashScreen;
import com.tishka1337.gui.TishkaGUI;
import com.tishka1337.utils.Logger;
import javax.swing.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        // Если нет аргументов — SplashScreen → GUI
        if (args.length == 0) {
            SwingUtilities.invokeLater(() -> {
                SplashScreen splash = new SplashScreen(() -> {
                    // После закрытия сплэша открываем GUI
                    SwingUtilities.invokeLater(() -> {
                        TishkaGUI gui = new TishkaGUI();
                        gui.setVisible(true);
                    });
                });
                splash.setVisible(true);
            });
            return;
        }

        // CLI режим
        String jarPath = args[0];
        String outputDir = "./output";
        boolean rename = false, pretty = false, rebuild = true, scan = true, razbor = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-o": case "--output": outputDir = args[++i]; break;
                case "-r": case "--rename": rename = true; break;
                case "-p": case "--pretty": pretty = true; break;
                case "--no-rebuild": rebuild = false; break;
                case "--no-scan": scan = false; break;
                case "-z": case "--razbor": razbor = true; break;
            }
        }

        try {
            if (razbor) {
                RazborEngine razborEngine = new RazborEngine(new File(jarPath), new File(outputDir));
                razborEngine.execute();
            } else {
                DecompilerEngine engine = new DecompilerEngine();
                engine.decompile(jarPath, outputDir, rename, pretty, rebuild, scan);
            }
        } catch (Exception e) {
            Logger.error("Failed: " + e.getMessage());
        }
    }
}