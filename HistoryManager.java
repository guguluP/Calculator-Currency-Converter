package Project.ui;

import Project.GlassCalculator;
import Project.engine.CalculatorEngine;
import Project.model.UITheme;
import java.awt.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Manages calculation and conversion history.
 * Handles persistence via preferences and optional database storage.
 */
public class HistoryManager {
    private final GlassCalculator calculator;
    private final CalculatorEngine engine;

    public HistoryManager(GlassCalculator calculator, CalculatorEngine engine) {
        this.calculator = calculator;
        this.engine = engine;
    }

    public void loadHistoryFromPrefs() {
        Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);

        // Load Calculation History
        int nCalc = p.getInt("histCalcN", 0);
        List<String> calcHistory = engine.getCalcHistory();
        calcHistory.clear();
        for (int i = 0; i < nCalc; i++) {
            String e = p.get("histCalc_" + i, null);
            if (e != null) calcHistory.add(e);
        }

        // Load Conversion History
        int nConv = p.getInt("histConvN", 0);
        List<String> convHistory = engine.getConvHistory();
        convHistory.clear();
        for (int i = 0; i < nConv; i++) {
            String e = p.get("histConv_" + i, null);
            if (e != null) convHistory.add(e);
        }
    }

    public void saveHistoryToPrefs() {
        Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);

        // Save Calculation History
        List<String> calcHistory = engine.getCalcHistory();
        p.putInt("histCalcN", calcHistory.size());
        for (int i = 0; i < calcHistory.size(); i++) {
            p.put("histCalc_" + i, calcHistory.get(i));
        }

        // Save Conversion History
        List<String> convHistory = engine.getConvHistory();
        p.putInt("histConvN", convHistory.size());
        for (int i = 0; i < convHistory.size(); i++) {
            p.put("histConv_" + i, convHistory.get(i));
        }

        try {
            p.flush();
        } catch (Exception ignored) {
        }
    }

    public void openHistoryPanel() {
        JDialog dlg = new JDialog(calculator, "History", true);
        dlg.setSize(600, 700);
        dlg.setLocationRelativeTo(calculator);
        dlg.getContentPane().setBackground(UITheme.BG_DEEP());

        JLabel title = new JLabel("History", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setBorder(new EmptyBorder(16, 0, 12, 0));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UITheme.BG_DEEP());
        tabbedPane.setForeground(UITheme.TEXT_PRIMARY());
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Calculations Tab
        tabbedPane.addTab("Calculations",
            createHistoryTab(engine.getCalcHistory(), "No calculations yet.", "Calculation History"));

        // Conversions Tab
        tabbedPane.addTab("Conversions",
            createHistoryTab(engine.getConvHistory(), "No conversions yet.", "Conversion History"));

        // Storage Info
        JLabel storageInfo = new JLabel("💾 All entries are saved in:  Preferences  +  Database",
                                       SwingConstants.CENTER);
        storageInfo.setFont(UITheme.FONT_STATUS);
        storageInfo.setForeground(UITheme.TEXT_DIM());
        storageInfo.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        btnPanel.setBackground(UITheme.BG_DEEP());

        JButton clearCalcBtn = styledDialogBtn("Clear Calculations", new Color(255, 100, 100));
        JButton clearConvBtn = styledDialogBtn("Clear Conversions", new Color(255, 100, 100));
        JButton clearAllBtn = styledDialogBtn("Clear All", UITheme.ACCENT_RED);
        JButton closeBtn = styledDialogBtn("Close", UITheme.ACCENT_CYAN);

        clearCalcBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dlg, "Clear all Calculation history?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                engine.getCalcHistory().clear();
                saveHistoryToPrefs();
                calculator.clearHistory(); // Clear from database too
                refreshHistoryTabs(tabbedPane);
            }
        });

        clearConvBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dlg, "Clear all Conversion history?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                engine.getConvHistory().clear();
                saveHistoryToPrefs();
                refreshHistoryTabs(tabbedPane);
            }
        });

        clearAllBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dlg, "Clear ALL history (both tabs)?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                engine.getCalcHistory().clear();
                engine.getConvHistory().clear();
                saveHistoryToPrefs();
                calculator.clearHistory(); // Clear from database too
                refreshHistoryTabs(tabbedPane);
            }
        });

        closeBtn.addActionListener(e -> dlg.dispose());

        btnPanel.add(clearCalcBtn);
        btnPanel.add(clearConvBtn);
        btnPanel.add(clearAllBtn);
        btnPanel.add(closeBtn);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG_DEEP());
        content.add(title, BorderLayout.NORTH);
        content.add(tabbedPane, BorderLayout.CENTER);
        content.add(storageInfo, BorderLayout.SOUTH);

        dlg.add(content, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel createHistoryTab(List<String> list, String emptyText, String tabName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        area.setBackground(UITheme.BG_SURFACE());
        area.setForeground(UITheme.TEXT_PRIMARY());
        area.setBorder(new EmptyBorder(16, 20, 16, 20));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(list.isEmpty() ? emptyText : String.join("\n\n", list));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.BG_DEEP());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void refreshHistoryTabs(JTabbedPane tabbedPane) {
        // Refresh both tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            JPanel tab = (JPanel) tabbedPane.getComponentAt(i);
            JScrollPane scroll = (JScrollPane) tab.getComponent(0);
            JTextArea area = (JTextArea) scroll.getViewport().getView();

            List<String> list = (i == 0) ? engine.getCalcHistory() : engine.getConvHistory();
            String empty = (i == 0) ? "No calculations yet." : "No conversions yet.";

            area.setText(list.isEmpty() ? empty : String.join("\n\n", list));
        }
    }

    private JButton styledDialogBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 28, 12, 28));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}