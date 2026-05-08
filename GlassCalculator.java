package Project;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.prefs.*;

import Project.model.UITheme;
import Project.model.ExpressionParser;
import Project.service.CurrencyService;
import Project.service.DBManager;
import Project.ui.GlassButton;
import Project.ui.ScalingDisplay;
import Project.ui.Toast;
import Project.ui.IconManager;
import Project.ui.SetupWizard;
import Project.ui.ButtonFactory;
import Project.ui.KeyboardHandler;
import Project.ui.HistoryManager;
import Project.engine.CalculatorEngine;
import Project.config.AppConfig;

public class GlassCalculator extends JFrame {

    // ═══════════════════════════════════════════════════════════════════
    //  FIELDS
    // ═══════════════════════════════════════════════════════════════════
    private DBManager db;
    private CurrencyService fx;

    private final ScalingDisplay display = new ScalingDisplay();
    private JPanel buttonPanel;
    private JPanel header;
    private JPanel statusBar;
    private CardLayout cardLayout;
    private JPanel mainCardPanel;

    private JLabel statusDB, statusAngle, statusMem, statusStorage;
    private JLabel modeLabel;

    // Currency panel components
    private ScalingDisplay inputFld;
    private JLabel resultLbl;
    private JLabel statusLbl;
    private JComboBox<String> fromBox, toBox;
    private Runnable commitHistory;

    private final String[] CURRENCIES = {
            "USD", "EUR", "INR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "RUB",
            "BRL", "ZAR", "MXN", "SGD", "HKD", "SEK", "NOK", "DKK", "KRW", "TRY"
    };

    private final List<String> favoriteCurrencies = new ArrayList<>();
    private JPanel favoritesPanel;

    private boolean startNewInput = true;
    private boolean radianMode = false;
    private boolean inverseMode = false;
    private double memory = 0.0;
    private double lastOperand = 0.0;
    private String lastOperator = "";
    private boolean repeatPossible = false;

    private final List<String> calcHistory = new ArrayList<>();
    private final List<String> convHistory = new ArrayList<>();

    private enum Mode {BASIC, SCIENTIFIC}

    private Mode currentMode = Mode.BASIC;

    private final Map<String, GlassButton> buttonMap = new HashMap<>();

    private boolean commitFlag;

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    public GlassCalculator() {
        super("GlassCalc");

        // Check for first run setup
        if (AppConfig.isFirstRun() || !AppConfig.isSetupComplete()) {
            SetupWizard wizard = new SetupWizard(this);
            wizard.setVisible(true);
        }

        Preferences prefs = Preferences.userNodeForPackage(GlassCalculator.class);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_DEEP());

        IconManager.setAppIcon(this);
        db = new DBManager(this);
        db.init();
        fx = new CurrencyService();

        header = buildHeader();
        buttonPanel = new JPanel();
        buttonPanel.setBackground(UITheme.BG_DEEP());
        int borderInset = Math.max(8, (int)(10 * getDPIScale()));
        buttonPanel.setBorder(new EmptyBorder(borderInset, borderInset, borderInset, borderInset));
        rebuildButtons();

        statusBar = buildStatusBar();

        JPanel calcCard = new JPanel(new BorderLayout(0, 6));
        calcCard.setBackground(UITheme.BG_DEEP());
        calcCard.add(header, BorderLayout.NORTH);
        calcCard.add(buttonPanel, BorderLayout.CENTER);
        calcCard.add(statusBar, BorderLayout.SOUTH);

        JPanel fxCard = buildCurrencyPanel();

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.add(calcCard, "calc");
        mainCardPanel.add(fxCard, "fx");
        add(mainCardPanel, BorderLayout.CENTER);

        setSize(prefs.getInt("w", 400), prefs.getInt("h", 640));
        setLocation(prefs.getInt("x", 200), prefs.getInt("y", 140));

        loadHistoryFromPrefs();
        hookKeyboard();
        hookDisplayContextMenu();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveHistoryToPrefs();
                db.shutdown();
                fx.shutdown();
                Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);
                p.putInt("w", getWidth());
                p.putInt("h", getHeight());
                p.putInt("x", getX());
                p.putInt("y", getY());
                try {
                    p.flush();
                } catch (Exception ignored) {
                }
                dispose();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  HEADER  (display + mode indicator + hamburger menu button)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(UITheme.BG_DEEP());
        h.setBorder(new EmptyBorder(12, 16, 8, 8));

        modeLabel = new JLabel("Calculator", SwingConstants.CENTER);
        modeLabel.setFont(UITheme.FONT_LABEL);
        modeLabel.setForeground(UITheme.TEXT_DIM());

        GlassButton menuBtn = new GlassButton("≡");
        menuBtn.setFont(new Font("Segoe UI", Font.BOLD, 32));
        menuBtn.setForeground(UITheme.ACCENT_CYAN);
        menuBtn.setPreferredSize(new Dimension(58, 58));
        menuBtn.addActionListener(e -> showMenu(menuBtn));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(modeLabel);
        rightPanel.add(menuBtn);

        h.add(display, BorderLayout.CENTER);
        h.add(rightPanel, BorderLayout.EAST);
        return h;
    }

    // ─────────────────────────────────────────────────────────────────
    //  STATUS BAR
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.BG_SURFACE());
        bar.setBorder(new EmptyBorder(6, 16, 6, 16));

        statusDB = makeStatusLabel("● DB", UITheme.ACCENT_RED);
        statusAngle = makeStatusLabel("DEG", UITheme.TEXT_DIM());
        statusMem = makeStatusLabel("M: 0", UITheme.TEXT_DIM());

        // New DB + Preferences status
        statusStorage = new JLabel("💾 DB + Prefs");
        statusStorage.setFont(UITheme.FONT_STATUS);
        statusStorage.setForeground(UITheme.TEXT_DIM());

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(statusDB);
        statusPanel.add(statusAngle);
        statusPanel.add(statusMem);
        statusPanel.add(new JSeparator(JSeparator.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 16));
                setForeground(UITheme.TEXT_DIM());
            }
        });
        statusPanel.add(statusStorage);

        bar.add(statusPanel, BorderLayout.WEST);
        return bar;
    }

    private JLabel makeStatusLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_STATUS);
        lbl.setForeground(color);
        return lbl;
    }

     // ─────────────────────────────────────────────────────────────────
    //  HAMBURGER MENU POPUP
    // ─────────────────────────────────────────────────────────────────
    private void showMenu(Component anchor) {
        JPopupMenu m = new JPopupMenu();
        m.setBackground(UITheme.BG_ELEVATED());
        m.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GLASS_BORDER(), 1),
                new EmptyBorder(8, 6, 8, 6)));

        addStyledMenuItem(m, currentMode == Mode.BASIC ? "✓ Basic Mode" : "Basic Mode",
                () -> switchMode(Mode.BASIC));
        addStyledMenuItem(m, currentMode == Mode.SCIENTIFIC ? "✓ Scientific Mode" : "Scientific Mode",
                () -> switchMode(Mode.SCIENTIFIC));

        m.addSeparator();
        addStyledMenuItem(m, "Currency Converter", () -> {
            cardLayout.show(mainCardPanel, "fx");
            modeLabel.setText("Currency");
        });
        addStyledMenuItem(m, "History", this::openHistoryPanel);
        m.addSeparator();

        // Theme selection submenu
        JMenu themeMenu = new JMenu("Theme");
        themeMenu.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        themeMenu.setBackground(UITheme.BG_ELEVATED());
        themeMenu.setForeground(UITheme.TEXT_PRIMARY());

        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem("Dark Mode");
        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem("Light Mode");
        JRadioButtonMenuItem systemItem = new JRadioButtonMenuItem("System Default");

        darkItem.setSelected(UITheme.getThemeMode() == UITheme.ThemeMode.DARK);
        lightItem.setSelected(UITheme.getThemeMode() == UITheme.ThemeMode.LIGHT);
        systemItem.setSelected(UITheme.getThemeMode() == UITheme.ThemeMode.SYSTEM);

        darkItem.addActionListener(e -> {
            UITheme.setThemeMode(UITheme.ThemeMode.DARK);
            refreshTheme();
        });
        lightItem.addActionListener(e -> {
            UITheme.setThemeMode(UITheme.ThemeMode.LIGHT);
            refreshTheme();
        });
        systemItem.addActionListener(e -> {
            UITheme.setThemeMode(UITheme.ThemeMode.SYSTEM);
            refreshTheme();
        });

        themeGroup.add(darkItem);
        themeGroup.add(lightItem);
        themeGroup.add(systemItem);

        themeMenu.add(darkItem);
        themeMenu.add(lightItem);
        themeMenu.add(systemItem);
        m.add(themeMenu);

        m.addSeparator();

        // ✅ NEW: Individual reconfiguration options
        addStyledMenuItem(m, "🔧 Reconfigure Database", this::showDatabaseConfigDialog);
        addStyledMenuItem(m, "🔑 Configure API Key", this::showApiConfigDialog);

        addStyledMenuItem(m, "Settings", this::showSettings);
        addStyledMenuItem(m, "About", this::showAbout);
        addStyledMenuItem(m, "Keyboard Shortcuts", this::showKeyboardHelp);

        m.show(anchor, anchor.getWidth() - 240, anchor.getHeight() + 6);
    }

    private void switchMode(Mode mode) {
        if (currentMode != mode) {
            currentMode = mode;
            modeLabel.setText(mode == Mode.BASIC ? "Basic" : "Scientific");
            rebuildButtons();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  DATABASE CONFIG DIALOG
    // ─────────────────────────────────────────────────────────────────
    private void showDatabaseConfigDialog() {
        JDialog dialog = new JDialog(this, "Database Configuration", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DEEP());
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.BG_DEEP());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 15);
        gbc.anchor = GridBagConstraints.WEST;

        // Database Type
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createLabel("Database Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> dbTypeCombo = new JComboBox<>(new String[]{"MySQL", "SQLite", "PostgreSQL"});
        dbTypeCombo.setBackground(UITheme.BG_ELEVATED());
        dbTypeCombo.setForeground(UITheme.TEXT_PRIMARY());
        dbTypeCombo.setPreferredSize(new Dimension(150, 32));
        dbTypeCombo.setSelectedItem(AppConfig.getDatabaseType());
        formPanel.add(dbTypeCombo, gbc);

        // Host
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(createLabel("Host:"), gbc);
        gbc.gridx = 1;
        JTextField dbHostField = createTextField(AppConfig.getDatabaseHost());
        formPanel.add(dbHostField, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(createLabel("Port:"), gbc);
        gbc.gridx = 1;
        JTextField dbPortField = createTextField(String.valueOf(AppConfig.getDatabasePort()));
        formPanel.add(dbPortField, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(createLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField dbUserField = createTextField(AppConfig.getDatabaseUser());
        formPanel.add(dbUserField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(createLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField dbPasswordField = new JPasswordField(AppConfig.getDatabasePassword());
        dbPasswordField.setBackground(UITheme.BG_ELEVATED());
        dbPasswordField.setForeground(UITheme.TEXT_PRIMARY());
        dbPasswordField.setPreferredSize(new Dimension(150, 32));
        dbPasswordField.setBorder(BorderFactory.createLineBorder(UITheme.GLASS_BORDER()));
        formPanel.add(dbPasswordField, gbc);

        // Database Name
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(createLabel("Database Name:"), gbc);
        gbc.gridx = 1;
        JTextField dbNameField = createTextField(AppConfig.getDatabaseName());
        formPanel.add(dbNameField, gbc);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UITheme.BG_DEEP());
        GlassButton saveBtn = new GlassButton("Save");
        saveBtn.setForeground(UITheme.ACCENT_GREEN);
        saveBtn.addActionListener(e -> {
            try {
                String dbType = (String) dbTypeCombo.getSelectedItem();
                String host = dbHostField.getText().trim();
                int port = Integer.parseInt(dbPortField.getText().trim());
                String username = dbUserField.getText().trim();
                String password = new String(dbPasswordField.getPassword());
                String dbName = dbNameField.getText().trim();

                AppConfig.setDatabaseConfig(dbType, host, port, username, password, dbName);
                toast("Database configuration saved ✓");

                // Reinitialize DB
                if (db != null) {
                    db.shutdown();
                }
                db = new DBManager(this);
                db.init();

                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        GlassButton cancelBtn = new GlassButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(UITheme.TEXT_PRIMARY());
        return label;
    }

    private JTextField createTextField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        field.setBackground(UITheme.BG_ELEVATED());
        field.setForeground(UITheme.TEXT_PRIMARY());
        field.setPreferredSize(new Dimension(150, 32));
        field.setBorder(BorderFactory.createLineBorder(UITheme.GLASS_BORDER()));
        return field;
    }

    // ─────────────────────────────────────────────────────────────────
    //  API CONFIG DIALOG
    // ─────────────────────────────────────────────────────────────────
    private void showApiConfigDialog() {
        JDialog dialog = new JDialog(this, "API Configuration", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DEEP());
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextArea descArea = new JTextArea(
            "Enter your Exchange Rate API key:\n\n" +
            "• Get free key from: https://www.exchangerate-api.com\n" +
            "• Leave empty to disable currency conversion"
        );
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descArea.setForeground(UITheme.TEXT_PRIMARY());
        descArea.setOpaque(false);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);

        JPasswordField apiKeyField = new JPasswordField(AppConfig.getApiKey());
        apiKeyField.setBackground(UITheme.BG_ELEVATED());
        apiKeyField.setForeground(UITheme.TEXT_PRIMARY());
        apiKeyField.setPreferredSize(new Dimension(200, 32));
        apiKeyField.setBorder(BorderFactory.createLineBorder(UITheme.GLASS_BORDER()));

        panel.add(descArea, BorderLayout.NORTH);
        panel.add(apiKeyField, BorderLayout.CENTER);

        dialog.add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UITheme.BG_DEEP());
        GlassButton saveBtn = new GlassButton("Save");
        saveBtn.setForeground(UITheme.ACCENT_GREEN);
        saveBtn.addActionListener(e -> {
            String apiKey = new String(apiKeyField.getPassword()).trim();
            AppConfig.setApiKey(apiKey);
            if (apiKey.isEmpty()) {
                fx = new CurrencyService(); // Recreate to disable API
                toast("Currency API disabled");
            } else {
                fx.configureApiKey(apiKey);
                toast("API key updated ✓");
            }
            dialog.dispose();
        });
        GlassButton cancelBtn = new GlassButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addStyledMenuItem(JPopupMenu m, String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        item.setBackground(UITheme.BG_ELEVATED());
        item.setForeground(UITheme.TEXT_PRIMARY());
        item.setBorder(new EmptyBorder(8, 16, 8, 16));
        item.addActionListener(e -> action.run());
        m.add(item);
    }

    // ─────────────────────────────────────────────────────────────────
    //  BUTTON PANEL
    // ─────────────────────────────────────────────────────────────────
    private void rebuildButtons() {
        buttonPanel.removeAll();
        buttonMap.clear();

        if (currentMode == Mode.BASIC) buildBasicButtons();
        else buildScientificButtons();

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void buildBasicButtons() {
        int gap = Math.max(6, (int)(8 * getDPIScale()));
        buttonPanel.setLayout(new GridLayout(5, 4, gap, gap));
        String[][] layout = {{"AC", "C", "%", "÷"}, {"7", "8", "9", "×"}, {"4", "5", "6", "−"}, {"1", "2", "3", "+"}, {"0", ".", "±", "="}};
        for (String[] row : layout) {
            for (String t : row) {
                GlassButton btn = makeBtn(t);
                buttonPanel.add(btn);
                buttonMap.put(t, btn);
            }
        }
    }

    private void buildScientificButtons() {
        int gap = Math.max(4, (int)(6 * getDPIScale()));
        buttonPanel.setLayout(new GridLayout(5, 10, gap, gap));
        String s = inverseMode ? "sin⁻¹" : "sin";
        String co = inverseMode ? "cos⁻¹" : "cos";
        String ta = inverseMode ? "tan⁻¹" : "tan";
        String sh = inverseMode ? "sinh⁻¹" : "sinh";
        String ch = inverseMode ? "cosh⁻¹" : "cosh";
        String th = inverseMode ? "tanh⁻¹" : "tanh";

        String[][] layout = {
                {"(", ")", "mc", "m+", "m-", "mr", "⌫", "AC", "%", "÷"},
                {"2nd", "x²", "x³", "xʸ", "yˣ", "2ˣ", "7", "8", "9", "×"},
                {"1/x", "²√x", "³√x", "ʸ√x", "logy", "log₂", "4", "5", "6", "−"},
                {"x!", s, co, ta, "e", "EE", "1", "2", "3", "+"},
                {"Rand", sh, ch, th, "π", "Rad", "±", "0", ".", "="}
        };

        for (String[] row : layout) {
            for (String t : row) {
                GlassButton btn = makeBtn(t);
                buttonPanel.add(btn);
                buttonMap.put(t, btn);
            }
        }
    }

    private GlassButton makeBtn(String text) {
        GlassButton btn = new GlassButton(text);

        if ("÷×−+=".contains(text)) btn.setForeground(UITheme.ACCENT_AMBER);
        else if ("AC C ⌫".contains(text)) btn.setForeground(UITheme.TEXT_DIM());
        else if (isSciToken(text)) btn.setForeground(UITheme.ACCENT_CYAN);

        // Tooltips for scientific functions
        String tooltip = getTooltip(text);
        if (tooltip != null) btn.setToolTipText(tooltip);

        btn.addActionListener(this::onButtonAction);
        return btn;
    }

    public void highlightButton(String text) {
        GlassButton btn = buttonMap.get(text);
        if (btn != null) {
            btn.triggerRipple();
        }
    }

    private String getTooltip(String t) {
        return switch (t) {
            // Basic Functions
            case "AC" -> "Clear All (Esc)";
            case "C", "⌫" -> "Backspace";
            case "%" -> "Percent (%)";
            case "±", "+/-" -> "Toggle Sign";
            case "=" -> "Calculate (Enter / =)";
            case "÷" -> "Divide (/)";
            case "×" -> "Multiply (*)";
            case "−" -> "Subtract (-)";
            case "+" -> "Add (+)";

            // Memory Functions
            case "mc" -> "Clear Memory";
            case "m+" -> "Add to Memory";
            case "m-" -> "Subtract from Memory";
            case "mr" -> "Recall Memory";

            // Scientific Functions
            case "x²" -> "Square (x²)";
            case "x³" -> "Cube (x³)";
            case "xʸ", "yˣ" -> "Power (x^y)";
            case "2ˣ" -> "2 to the power of x (2^x)";
            case "²√x" -> "Square Root";
            case "³√x" -> "Cube Root";
            case "ʸ√x" -> "y-th Root";
            case "1/x" -> "Reciprocal (1/x)";
            case "logy" -> "Logarithm Base 10";
            case "log₂" -> "Logarithm Base 2";
            case "x!" -> "Factorial (!)";
            case "sin", "sin⁻¹" -> "Sine / Arcsine";
            case "cos", "cos⁻¹" -> "Cosine / Arccosine";
            case "tan", "tan⁻¹" -> "Tangent / Arctangent";
            case "sinh", "sinh⁻¹" -> "Hyperbolic Sine / Arcsinh";
            case "cosh", "cosh⁻¹" -> "Hyperbolic Cosine / Arccosh";
            case "tanh", "tanh⁻¹" -> "Hyperbolic Tangent / Arctanh";
            case "π" -> "Pi (3.14159...)";
            case "e" -> "Euler's Number (2.718...)";
            case "EE" -> "<html>Enter exponent for a value (scientific notation)<br>(or press E)</html>";
            case "Rad" -> "Toggle Degrees / Radians";
            case "2nd" -> "Inverse / Hyperbolic functions";
            case "Rand" -> "Random Number (0 to 1)";
            case "(", ")" -> "Parentheses";

            default -> null;
        };
    }

    private boolean isSciToken(String t) {
        return switch (t) {
            case "2nd", "x\u00B2", "x\u00B3", "x\u02B8", "y\u02E3", "2\u02E3", "1/x",
                 "\u00B2\u221Ax", "\u00B3\u221Ax", "\u02B8\u221Ax", "logy", "log\u2082",
                 "x!", "sin", "sin\u207B\u00B9", "cos", "cos\u207B\u00B9", "tan", "tan\u207B\u00B9",
                 "sinh", "sinh\u207B\u00B9", "cosh", "cosh\u207B\u00B9", "tanh", "tanh\u207B\u00B9",
                 "e", "EE", "\u03C0", "Rad", "(", ")",
                 "mc", "m+", "m-", "mr", "Rand" -> true;
            default -> false;
        };
    }

    // ─────────────────────────────────────────────────────────────────
    //  BUTTON / KEYBOARD ACTIONS
    // ─────────────────────────────────────────────────────────────────
    private void onButtonAction(ActionEvent e) {
        dispatch(((JButton) e.getSource()).getText());
    }

    public void dispatch(String cmd) {
        switch (cmd) {
            case "AC" -> reset();
            case "C", "⌫" -> backspace();
            case "%" -> percent();
            case "±", "+/-" -> negate();
            case "=" -> evaluate();
             case "x²" -> {
                 try {
                     double v = eval(display.getText());
                     display.setText(fmt(v * v));
                     startNewInput = true;
                 } catch (Exception ignored) {
                     display.setText("Error");
                 }
             }
             case "x³" -> {
                 try {
                     double v = eval(display.getText());
                     display.setText(fmt(v * v * v));
                     startNewInput = true;
                 } catch (Exception ignored) {
                     display.setText("Error");
                 }
             }
             case "xʸ", "yˣ" -> insert("^");
             case "2ˣ" -> insert("2^");
             case "1/x" -> {
                 try {
                     double v = eval(display.getText());
                     display.setText(fmt(1.0 / v));
                     startNewInput = true;
                 } catch (Exception ignored) {
                     display.setText("Error");
                 }
             }
             case "²√x" -> {
                 try {
                     double v = eval(display.getText());
                     display.setText(fmt(Math.sqrt(v)));
                     startNewInput = true;
                 } catch (Exception ignored) {
                     display.setText("Error");
                 }
             }
             case "³√x" -> {
                 try {
                     double v = eval(display.getText());
                     display.setText(fmt(Math.cbrt(v)));
                     startNewInput = true;
                 } catch (Exception ignored) {
                     display.setText("Error");
                 }
             }
             case "ʸ√x" -> insert("^(1/");
             case "logy" -> insert("log(");
             case "log₂" -> insert("log2(");
             case "x!" -> factorial();
            case "(" -> insert("(");
            case ")" -> insert(")");
            case "mc" -> {
                memory = 0;
                refreshStatusBar();
                toast("Memory cleared");
            }
             case "m+" -> {
                 try {
                     memory += eval(display.getText());
                     refreshStatusBar();
                     toast("Added to memory");
                 } catch (Exception ignored) {
                     toast("Invalid expression");
                 }
             }
             case "m-" -> {
                 try {
                     memory -= eval(display.getText());
                     refreshStatusBar();
                     toast("Subtracted from memory");
                 } catch (Exception ignored) {
                     toast("Invalid expression");
                 }
             }
            case "mr" -> insert(fmt(memory));
            case "e" -> insert(String.valueOf(Math.E));
            case "EE" -> insert("E");
            case "Rand" -> insert(fmt(Math.random()));
            case "π" -> insert(String.valueOf(Math.PI));
            case "Rad" -> {
                radianMode = !radianMode;
                refreshStatusBar();
                toast(radianMode ? "Radians" : "Degrees");
            }
            case "2nd" -> {
                inverseMode = !inverseMode;
                rebuildButtons();
            }
            case "sin", "sin⁻¹" -> insert(inverseMode ? "asin(" : "sin(");
            case "cos", "cos⁻¹" -> insert(inverseMode ? "acos(" : "cos(");
            case "tan", "tan⁻¹" -> insert(inverseMode ? "atan(" : "tan(");
            case "sinh", "sinh⁻¹" -> insert(inverseMode ? "asinh(" : "sinh(");
            case "cosh", "cosh⁻¹" -> insert(inverseMode ? "acosh(" : "cosh(");
            case "tanh", "tanh⁻¹" -> insert(inverseMode ? "atanh(" : "tanh(");
            default -> {
                if (cmd.matches("[0-9.]") || "÷×−+^".contains(cmd))
                    insert(cmd);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  INPUT HELPERS
    // ─────────────────────────────────────────────────────────────────
    public void insert(String text) {
        String cur = display.getText();
        int caret = display.getCaretPosition();

        if (startNewInput) {
            display.setText(text);
            display.setCaretPosition(Math.min(text.length(), display.getText().length()));
            startNewInput = false;
            return;
        }

        // If inserting an operator, replace the last operator before caret
        if (text.length() == 1 && "÷×−+".contains(text) && caret > 0) {
            String before = cur.substring(0, caret);
            int lastOpIndex = -1;
            for (int i = before.length() - 1; i >= 0; i--) {
                char ch = before.charAt(i);
                if ("÷×−+".indexOf(ch) >= 0) {
                    lastOpIndex = i;
                    break;
                }
            }
            if (lastOpIndex >= 0) {
                // Replace the operator at lastOpIndex; preserve everything else
                String n = cur.substring(0, lastOpIndex) + text + cur.substring(lastOpIndex + 1);
                display.setText(n);
                display.setCaretPosition(lastOpIndex + 1);
                return;
            }
        }

        String next = cur.substring(0, caret) + text + cur.substring(caret);
        display.setText(next);
        display.setCaretPosition(Math.min(caret + text.length(), next.length()));
        startNewInput = false;
    }

    private void backspace() {
        String t = display.getText();
        if (t.isEmpty() || t.equals("0") || t.equals("Error")) {
            reset();
            return;
        }
        int p = display.getCaretPosition();
        if (p > 0) {
            String n = t.substring(0, p - 1) + t.substring(p);
            display.setText(n.isEmpty() ? "0" : n);
            display.setCaretPosition(Math.max(0, p - 1));
        }
    }

    private void percent() {
        try {
            double v = eval(display.getText());
            display.setText(fmt(v / 100));
            startNewInput = true;
        } catch (Exception ignored) {
        }
    }

    private void negate() {
        try {
            double v = eval(display.getText());
            display.setText(fmt(-v));
        } catch (Exception ignored) {
        }
    }

    private void factorial() {
        try {
            double n = Double.parseDouble(display.getText().trim());
            if (n < 0 || n > 170 || n != (long) n) {
                display.setText("Error");
                return;
            }
            BigInteger r = BigInteger.ONE;
            for (long i = 2; i <= (long) n; i++) {
                r = r.multiply(BigInteger.valueOf(i));
            }
            display.setText(r.toString());
            startNewInput = true;
        } catch (Exception e) {
            display.setText("Error");
        }
    }

    private boolean isValidExpression(String expr) {
        if (expr.length() > 1000) return false; // Prevent DoS
        // Must include Unicode button operators ×, ÷, − alongside ASCII ones;
        // without them every on-screen-button expression is rejected before parsing.
        return expr.matches("[0-9a-zA-Z.+\\-*/^()√πe×÷−,\\s]+");
    }

    private void evaluate() {
        String cur = display.getText().trim();
        if (cur.isEmpty()) return;
        if (!isValidExpression(cur)) {
            display.setText("Invalid Input");
            return;
        }
        try {
            double result = eval(cur);
            String fmtResult = fmt(result);
            display.setText(fmtResult);
            
            String entry = cur + " = " + fmtResult;
            addCalculationToHistory(entry);           // ← Changed
            db.saveAsync(cur, fmtResult, "calc");
            // Extract for repeat operator (pressing = again)
            extractLastOp(cur);
            repeatPossible = true;
            startNewInput = true;
        } catch (Exception ex) {
            display.setText("Error");
            repeatPossible = false;
        }
    }

    private void extractLastOp(String expr) {
        String c = expr.replace("×", "*").replace("÷", "/").replace("−", "-");
        lastOperator = "";
        lastOperand = 0;
        int lastOpIndex = -1;
        for (int i = c.length() - 1; i >= 0; i--) {
            char ch = c.charAt(i);
            if ("+-*/^".indexOf(ch) >= 0) {
                lastOpIndex = i;
                break;
            }
        }
        if (lastOpIndex >= 0) {
            try {
                lastOperand = Double.parseDouble(c.substring(lastOpIndex + 1));
                lastOperator = String.valueOf(c.charAt(lastOpIndex));
            } catch (Exception ignored) {
            }
        }
    }

    private double eval(String expr) throws ArithmeticException {
        return new ExpressionParser(expr, radianMode).parse();
    }

    // ─────────────────────────────────────────────────────────────────
    //  FORMAT
    // ─────────────────────────────────────────────────────────────────
    static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "Error";
        if (Math.abs(v) < 1e-12 && v != 0) return "0";
        // Integer check (avoids trailing .0)
        if (v == Math.floor(v) && Math.abs(v) < 1e15)
            return String.valueOf((long) v);
        double abs = Math.abs(v);
        if (abs >= 1e10 || (abs > 0 && abs < 1e-5))
            return String.format("%.8g", v).replaceAll("0+E", "E");
        return String.format("%.10f", v)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    // ─────────────────────────────────────────────────────────────────
    //  HISTORY  (preferences + in-memory)
    // ─────────────────────────────────────────────────────────────────
    private void addToHistory(String entry, boolean isConversion) {
        List<String> target = isConversion ? convHistory : calcHistory;

        if (target.contains(entry)) return; // avoid duplicates

        target.add(0, entry);
        if (target.size() > 100) target.remove(target.size() - 1);
        saveHistoryToPrefs(); // Save immediately to ensure persistence
    }

    private void addCalculationToHistory(String entry) {
        addToHistory(entry, false);
    }

    private void addConversionToHistory(String entry) {
        addToHistory(entry, true);
    }

    private void loadHistoryFromPrefs() {
        Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);

        // Load Calculation History
        int nCalc = p.getInt("histCalcN", 0);
        calcHistory.clear();
        for (int i = 0; i < nCalc; i++) {
            String e = p.get("histCalc_" + i, null);
            if (e != null) calcHistory.add(e);
        }

        // Load Conversion History
        int nConv = p.getInt("histConvN", 0);
        convHistory.clear();
        for (int i = 0; i < nConv; i++) {
            String e = p.get("histConv_" + i, null);
            if (e != null) convHistory.add(e);
        }

        // Load Favorite Currencies
        String favStr = p.get("favCurrencies", "USD,EUR,INR,GBP,JPY");
        favoriteCurrencies.clear();
        for (String curr : favStr.split(",")) {
            if (!curr.trim().isEmpty()) {
                favoriteCurrencies.add(curr.trim());
            }
        }
    }

    private void saveHistoryToPrefs() {
        Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);

        // Save Calculation History
        p.putInt("histCalcN", calcHistory.size());
        for (int i = 0; i < calcHistory.size(); i++) {
            p.put("histCalc_" + i, calcHistory.get(i));
        }

        // Save Conversion History
        p.putInt("histConvN", convHistory.size());
        for (int i = 0; i < convHistory.size(); i++) {
            p.put("histConv_" + i, convHistory.get(i));
        }

        // Save Favorite Currencies
        p.put("favCurrencies", String.join(",", favoriteCurrencies));
    }

    public void clearHistory() {
        calcHistory.clear();
        convHistory.clear();
        saveHistoryToPrefs();
        db.clearHistory();
    }

    private void reset() {
        display.setText("0");
        startNewInput = true;
        repeatPossible = false;
        lastOperator = "";
        lastOperand = 0;
    }

    // ─────────────────────────────────────────────────────────────────
    //  HISTORY PANEL  (slide-in inside the main window)
    // ─────────────────────────────────────────────────────────────────
    private void openHistoryPanel() {
        JDialog dlg = new JDialog(this, "History", true);
        dlg.setSize(580, 650);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(UITheme.BG_DEEP());

        JLabel title = new JLabel("History", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(UITheme.ACCENT_CYAN);
        title.setBorder(new EmptyBorder(16, 0, 8, 0));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UITheme.BG_DEEP());
        tabbedPane.setForeground(UITheme.TEXT_PRIMARY());
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Calculations Tab
        tabbedPane.addTab("Calculations",
            createHistoryTab(calcHistory, "No calculations yet.", "Calculation History"));

        // Conversions Tab
        tabbedPane.addTab("Conversions",
            createHistoryTab(convHistory, "No conversions yet.", "Conversion History"));

        // Storage Info
        JLabel storageInfo = new JLabel("💾 All entries are saved in:  Preferences  +  MySQL Database",
                                       SwingConstants.CENTER);
        storageInfo.setFont(UITheme.FONT_STATUS);
        storageInfo.setForeground(UITheme.TEXT_DIM());
        storageInfo.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        btnPanel.setBackground(UITheme.BG_DEEP());

        JButton clearCalcBtn = styledDialogBtn("Clear Calculations", new Color(255, 100, 100));
        JButton clearConvBtn = styledDialogBtn("Clear Conversions", new Color(255, 100, 100));
        JButton clearAllBtn = styledDialogBtn("Clear All", UITheme.ACCENT_RED);
        JButton closeBtn = styledDialogBtn("Close", UITheme.ACCENT_CYAN);

        clearCalcBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dlg, "Clear all Calculation history?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                calcHistory.clear();
                saveHistoryToPrefs();
                db.clearHistory(); // Clear from database too
                refreshHistoryTabs(tabbedPane);
            }
        });

        clearConvBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dlg, "Clear all Conversion history?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                convHistory.clear();
                saveHistoryToPrefs();
                refreshHistoryTabs(tabbedPane);
            }
        });

        clearAllBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dlg, "Clear ALL history (both tabs)?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                calcHistory.clear();
                convHistory.clear();
                saveHistoryToPrefs();
                db.clearHistory(); // Clear from database too
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
        area.setText(list.isEmpty() ? emptyText : String.join("\n\n", list));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void refreshHistoryTabs(JTabbedPane tabbedPane) {
        // Refresh both tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            JPanel tab = (JPanel) tabbedPane.getComponentAt(i);
            JScrollPane scroll = (JScrollPane) tab.getComponent(0);
            JTextArea area = (JTextArea) scroll.getViewport().getView();

            List<String> list = (i == 0) ? calcHistory : convHistory;
            String empty = (i == 0) ? "No calculations yet." : "No conversions yet.";

            area.setText(list.isEmpty() ? empty : String.join("\n\n", list));
        }
    }

    private JButton styledDialogBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 32, 12, 32));
        return b;
    }

    // ─────────────────────────────────────────────────────────────────
    //  KEYBOARD HELP DIALOG
    // ─────────────────────────────────────────────────────────────────
    private void showKeyboardHelp() {
        String help = """
                Enter / =          Evaluate
                Backspace          Delete character
                Escape / Delete    Clear (AC)
                Ctrl + C / V       Copy / Paste
                ^                  Power
                %                  Percent
                !                  Factorial (Scientific)
                Ctrl + A           Select All
                Right-click on display → Context menu
                """;

        JTextArea ta = new JTextArea(help);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 15));
        ta.setBackground(UITheme.BG_SURFACE());
        ta.setForeground(UITheme.TEXT_PRIMARY());

        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Keyboard Shortcuts", JOptionPane.PLAIN_MESSAGE);
    }

    // ─────────────────────────────────────────────────────────────────
    //  SETTINGS DIALOG
    // ─────────────────────────────────────────────────────────────────
    private void showSettings() {
        SetupWizard wizard = new SetupWizard(this);
        wizard.setVisible(true);
        // Reinitialize DB and currency service after settings change
        if (db != null) {
            db.shutdown();
        }
        db = new DBManager(this);
        db.init();
        fx = new CurrencyService();
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABOUT DIALOG
    // ─────────────────────────────────────────────────────────────────
    private void showAbout() {
        String about = """
                GlassCalculator v2.0

                A modern, glassmorphism-themed calculator with scientific functions,
                currency conversion, and database-backed history.

                Built with Java Swing.

                Author: Ready-made application
                """;

        JTextArea ta = new JTextArea(about);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 15));
        ta.setBackground(UITheme.BG_SURFACE());
        ta.setForeground(UITheme.TEXT_PRIMARY());

        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "About GlassCalculator", JOptionPane.PLAIN_MESSAGE);
    }

    // ─────────────────────────────────────────────────────────────────
    //  RIGHT-CLICK CONTEXT MENU ON DISPLAY
    // ─────────────────────────────────────────────────────────────────
    private void hookDisplayContextMenu() {
        JPopupMenu ctx = new JPopupMenu();
        ctx.setBackground(UITheme.BG_ELEVATED());

        JMenuItem copy = ctxItem("Copy", () -> {
            display.selectAll();
            display.copy();
            toast("Copied");
        });
        JMenuItem paste = ctxItem("Paste", () -> {
            String clip = getClipboardText();
            if (clip != null && !clip.isBlank()) {
                display.setText(clip.trim());
                display.setCaretPosition(display.getText().length());
                startNewInput = false;
            }
        });
        JMenuItem clr = ctxItem("Clear (AC)", this::reset);
        JMenuItem hist = ctxItem("History", this::openHistoryPanel);

        ctx.add(copy);
        ctx.add(paste);
        ctx.addSeparator();
        ctx.add(clr);
        ctx.add(hist);

        display.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) ctx.show(display, e.getX(), e.getY());
            }
        });
    }

    private JMenuItem ctxItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        item.setBackground(UITheme.BG_ELEVATED());
        item.setForeground(UITheme.TEXT_PRIMARY());
        item.setBorder(new EmptyBorder(6, 14, 6, 14));
        item.addActionListener(e -> action.run());
        return item;
    }

    private String getClipboardText() {
        try {
            return (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  GLOBAL KEYBOARD HANDLER
    // ─────────────────────────────────────────────────────────────────
    private void hookKeyboard() {
        display.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int kc = e.getKeyCode();
                boolean ctrl = e.isControlDown() || e.isMetaDown();

                if (ctrl && kc == KeyEvent.VK_A) {
                    display.selectAll();
                    e.consume();
                    return;
                }
                if (ctrl && kc == KeyEvent.VK_C) {
                    display.selectAll();
                    display.copy();
                    toast("Copied");
                    e.consume();
                    return;
                }
                if (ctrl && kc == KeyEvent.VK_V) {
                    pasteFromClipboard();
                    e.consume();
                    return;
                }

                if (kc == KeyEvent.VK_ENTER || kc == KeyEvent.VK_EQUALS) {
                    dispatch("=");
                    highlightButton("=");
                    e.consume();
                    return;
                }
                if (kc == KeyEvent.VK_BACK_SPACE) {
                    dispatch("⌫");
                    highlightButton("⌫");
                    e.consume();
                    return;
                }
                if (kc == KeyEvent.VK_ESCAPE || kc == KeyEvent.VK_DELETE) {
                    dispatch("AC");
                    highlightButton("AC");
                    e.consume();
                    return;
                }

                char ch = e.getKeyChar();
                if (Character.isDigit(ch) || ch == '.') {
                    insert(String.valueOf(ch));
                    highlightButton(String.valueOf(ch));
                    e.consume();
                    return;
                }

                switch (ch) {
                    case '+':
                        insert("+");
                        highlightButton("+");
                        break;
                    case '-':
                        insert("−");
                        highlightButton("−");
                        break;
                    case '*':
                        insert("×");
                        highlightButton("×");
                        break;
                    case '/':
                        insert("÷");
                        highlightButton("÷");
                        break;
                    case '^':
                        insert("^");
                        highlightButton("xʸ");
                        break;
                    case '%':
                        dispatch("%");
                        highlightButton("%");
                        break;
                }
                e.consume();
            }

            private void highlightButton(String s) {
                GlassButton btn = buttonMap.get(s);
                if (btn != null) {
                    btn.triggerRipple();
                    btn.triggerScale();
                }
            }
        });
        display.requestFocusInWindow();
    }

    public void pasteFromClipboard() {
        String clip = getClipboardText();
        if (clip != null && !clip.isBlank()) {
            display.setText(clip.trim());
            startNewInput = false;
            toast("Pasted");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  TOAST
    // ─────────────────────────────────────────────────────────────────
    public void toast(String msg) {
        Toast t = new Toast(this, msg);
        t.show(display.getY());
    }

    // ─────────────────────────────────────────────────────────────────
    //  CURRENCY CONVERTER PANEL
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildCurrencyPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_DEEP());

        // ── Top: display area ────────────────────────────────────────
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(UITheme.BG_DEEP());
        top.setBorder(new EmptyBorder(24, 18, 12, 18));

        // Result row
        resultLbl = new JLabel("0", SwingConstants.RIGHT);
        resultLbl.setFont(UITheme.FONT_DISPLAY);
        resultLbl.setForeground(UITheme.TEXT_PRIMARY());

        toBox = styledCombo(CURRENCIES);
        toBox.setSelectedItem("INR");

        JPanel resultRow = new JPanel(new BorderLayout(10, 0));
        resultRow.setOpaque(false);
        resultRow.add(resultLbl, BorderLayout.CENTER);
        resultRow.add(toBox, BorderLayout.EAST);

        // Swap button
        JPanel swapRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        swapRow.setOpaque(false);
        GlassButton swapBtn = new GlassButton("↕");
        swapBtn.setFont(new Font("Segoe UI", Font.BOLD, 32));
        swapBtn.setForeground(UITheme.ACCENT_AMBER);
        swapBtn.setPreferredSize(new Dimension(60, 60));
        swapRow.add(swapBtn);

        // Input row
        inputFld = new ScalingDisplay();
        inputFld.setEditable(true);
        inputFld.setText("0");
        inputFld.setCaretColor(UITheme.ACCENT_CYAN);

        fromBox = styledCombo(CURRENCIES);
        fromBox.setSelectedItem("USD");

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputFld, BorderLayout.CENTER);
        inputRow.add(fromBox, BorderLayout.EAST);

        commitHistory = () -> {
            String input = inputFld.getText().trim();
            String result = resultLbl.getText().trim();
            String from = (String) fromBox.getSelectedItem();
            String to = (String) toBox.getSelectedItem();

            if (input.isEmpty() || result.isEmpty() ||
                result.equals("0") || result.equals("Fetching...") ||
                result.equals("Error") || from.equals(to)) {
                return;
            }

            String historyEntry = String.format("%s %s = %s %s", input, from, result, to);

            addConversionToHistory(historyEntry);     // ← Use conversion history

            // Save to DB async
            db.saveAsync(input + " " + from, result + " " + to, "conv");

            toast("Conversion saved to history");
        };

        // Status row
        statusLbl = new JLabel("Live rates · Ready", SwingConstants.CENTER);
        statusLbl.setFont(UITheme.FONT_STATUS);
        statusLbl.setForeground(UITheme.ACCENT_GREEN);

        GlassButton backBtn = new GlassButton("← Calc");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        backBtn.setForeground(UITheme.ACCENT_CYAN);
        backBtn.setPreferredSize(new Dimension(110, 38));
        backBtn.addActionListener(e -> cardLayout.show(mainCardPanel, "calc"));

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusRow.add(backBtn, BorderLayout.WEST);
        statusRow.add(statusLbl, BorderLayout.CENTER);

        top.add(resultRow);
        top.add(swapRow);
        top.add(inputRow);
        top.add(Box.createVerticalStrut(6));
        top.add(statusRow);
        root.add(top, BorderLayout.NORTH);

        // ── Favorites panel ─────────────────────────────────────────
        favoritesPanel = createFavoritesPanel();
        root.add(favoritesPanel, BorderLayout.CENTER);

        // ── Currency keypad ─────────────────────────────────────────
        int keypadGap = Math.max(6, (int)(10 * getDPIScale()));
        JPanel keypad = new JPanel(new GridLayout(5, 4, keypadGap, keypadGap));
        keypad.setBackground(UITheme.BG_DEEP());
        int keypadBorder = Math.max(6, (int)(8 * getDPIScale()));
        keypad.setBorder(new EmptyBorder(keypadBorder, keypadBorder * 2, keypadBorder * 3, keypadBorder * 2));

        // Live convert: debounced to avoid API hammering

        // Keypad button actions
        String[] keys = {"⌫", "AC", "%", "÷", "7", "8", "9", "×", "4", "5", "6", "−", "1", "2", "3", "+", "+/-", "0", ".", "="};
        for (String k : keys) {
            GlassButton btn = new GlassButton(k);
            buttonMap.put(k, btn);
            if ("÷×−+=".contains(k)) btn.setForeground(UITheme.ACCENT_AMBER);
            btn.addActionListener(ev -> {
                String cur = inputFld.getText();
                switch (k) {
                    case "⌫" -> {
                        if (cur.length() > 1) inputFld.setText(cur.substring(0, cur.length() - 1));
                        else inputFld.setText("0");
                    }
                    case "AC" -> inputFld.setText("0");
                    case "%" -> {
                        try {
                            inputFld.setText(fmt(new ExpressionParser(cur, false).parse() / 100));
                        } catch (ArithmeticException e) {
                            resultLbl.setText("Invalid expression");
                        } catch (Exception e) {
                            resultLbl.setText("Error");
                        }
                    }
                    case "+/-" -> {
                        try {
                            inputFld.setText(fmt(-new ExpressionParser(cur, false).parse()));
                        } catch (ArithmeticException e) {
                            resultLbl.setText("Invalid expression");
                        } catch (Exception e) {
                            resultLbl.setText("Error");
                        }
                    }
                    case "÷", "×", "−", "+" -> inputFld.setText(cur + k);
                    case "=" -> {
                        try {
                            inputFld.setText(fmt(new ExpressionParser(cur.replace("÷", "/").replace("×", "*").replace("−", "-"), false).parse()));
                        } catch (ArithmeticException e) {
                            resultLbl.setText("Error: " + e.getMessage());
                        } catch (Exception e) {
                            resultLbl.setText("Error");
                        }
                        doLive(true);
                    }
                    default -> {
                        if (k.matches("[0-9]")) inputFld.setText(cur.equals("0") ? k : cur + k);
                        else if (k.equals(".") && !cur.contains(".")) inputFld.setText(cur + ".");
                    }
                }
                fx.debounce(() -> doLive(false));
            });
            keypad.add(btn);
        }

        // Swap
        swapBtn.addActionListener(e -> {
            Object f = fromBox.getSelectedItem(), t = toBox.getSelectedItem();
            fromBox.setSelectedItem(t);
            toBox.setSelectedItem(f);
            fx.debounce(() -> doLive(false));
        });

        // fromBox / toBox listeners
        fromBox.addActionListener(e -> fx.debounce(() -> doLive(false)));
        toBox.addActionListener(e -> fx.debounce(() -> doLive(false)));

        // Keyboard on inputFld
        inputFld.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doLive(false);
                    e.consume();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cardLayout.show(mainCardPanel, "calc");
                    e.consume();
                }
            }
        });

        root.add(keypad, BorderLayout.SOUTH);
        return root;
    }

    private JPanel createFavoritesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(8, 18, 8, 18));

        // Title with add button
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Favorites", SwingConstants.LEFT);
        title.setFont(UITheme.FONT_LABEL);
        title.setForeground(UITheme.TEXT_DIM());

        GlassButton addBtn = new GlassButton("+");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        addBtn.setForeground(UITheme.ACCENT_GREEN);
        addBtn.setPreferredSize(new Dimension(40, 40));
        addBtn.addActionListener(e -> showAddFavoriteDialog());

        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(addBtn, BorderLayout.EAST);

        // Favorites buttons
        JPanel favButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        favButtons.setOpaque(false);
        updateFavoritesButtons(favButtons);

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(favButtons, BorderLayout.CENTER);

        return panel;
    }

    private void updateFavoritesButtons(JPanel container) {
        container.removeAll();
        for (String curr : favoriteCurrencies) {
            GlassButton btn = new GlassButton(curr);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setPreferredSize(new Dimension(60, 35));
            btn.addActionListener(e -> {
                // Set as target currency
                toBox.setSelectedItem(curr);
                fx.debounce(() -> doLive(false));
                toast("Set " + curr + " as target");
            });
            container.add(btn);
        }
        container.revalidate();
        container.repaint();
    }

    private void showAddFavoriteDialog() {
        JDialog dialog = new JDialog(this, "Add Favorite Currency", true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DEEP());
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JComboBox<String> currBox = new JComboBox<>(CURRENCIES);
        currBox.setBackground(UITheme.BG_ELEVATED());
        currBox.setForeground(UITheme.TEXT_PRIMARY());
        currBox.setPreferredSize(new Dimension(150, 32));

        panel.add(new JLabel("Select currency to add to favorites:"), BorderLayout.NORTH);
        panel.add(currBox, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(UITheme.BG_DEEP());

        GlassButton addBtn = new GlassButton("Add");
        addBtn.setForeground(UITheme.ACCENT_GREEN);
        addBtn.addActionListener(e -> {
            String selected = (String) currBox.getSelectedItem();
            if (selected != null && !favoriteCurrencies.contains(selected)) {
                favoriteCurrencies.add(selected);
                saveHistoryToPrefs();
                updateFavoritesButtons((JPanel) favoritesPanel.getComponent(1));
                toast(selected + " added to favorites");
                dialog.dispose();
            } else {
                toast("Currency already in favorites");
            }
        });

        GlassButton cancelBtn = new GlassButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(cancelBtn);
        btnPanel.add(addBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        cb.setBackground(UITheme.BG_ELEVATED());
        cb.setForeground(UITheme.TEXT_PRIMARY());
        cb.setFocusable(false);
        cb.setBorder(new EmptyBorder(8, 12, 8, 12));
        cb.setPreferredSize(new Dimension(138, 58));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object v,
                                                          int i, boolean sel, boolean foc) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, v, i, sel, foc);
                l.setFont(new Font("Segoe UI", Font.PLAIN, 20));
                l.setBorder(new EmptyBorder(10, 14, 10, 14));
                l.setBackground(sel ? UITheme.ACCENT_CYAN : UITheme.BG_ELEVATED());
                l.setForeground(sel ? UITheme.BG_DEEP() : UITheme.TEXT_PRIMARY());
                return l;
            }
        });
        return cb;
    }

    private void doLive(boolean commit) {
        commitFlag = commit;
        String raw = inputFld.getText().trim();
        if (raw.isEmpty() || raw.equals("0")) {
            resultLbl.setText("0");
            return;
        }
        if (!isValidExpression(raw)) {
            resultLbl.setText("Invalid Input");
            return;
        }
        double amount;
        try {
            amount = new ExpressionParser(raw, false).parse();
        } catch (Exception ex) {
            try {
                amount = Double.parseDouble(raw);
            } catch (Exception e2) {
                return;
            }
        }

        String from = (String) fromBox.getSelectedItem();
        String to = (String) toBox.getSelectedItem();
        if (from.equals(to)) {
            resultLbl.setText(fmt(amount));
            return;
        }

        final double finalAmount = amount;
        double rate = fx.rateOrFetch(from, to,
                () -> {   // onResult: retry with fresh cache
                    String f = (String) fromBox.getSelectedItem();
                    String t2 = (String) toBox.getSelectedItem();
                    // reuse cached data
                    double r2 = fx.rateOrFetch(f, t2, () -> {
                    }, () -> {
                    });
                        if (!Double.isNaN(r2)) {
                            resultLbl.setText(fmt(finalAmount * r2));
                            String change = fx.getRateChangeDirection(f, t2);
                            String rateText = String.format("1 %s = %.4f %s", f, r2, t2);
                            if (!change.isEmpty()) {
                                rateText += " " + change;
                            }
                            statusLbl.setText(rateText + " · " + fx.lastUpdated(f).split(" ")[1]);
                            if (commitFlag) commitHistory.run();
                        }
                },
                () -> {
                    resultLbl.setText("Error");
                    statusLbl.setText("Network error");
                }
        );

        if (Double.isNaN(rate)) {
            resultLbl.setText("Fetching...");
            statusLbl.setText("Connecting...");
        } else {
            resultLbl.setText(fmt(finalAmount * rate));

            // Show rate with change indicator
            String change = fx.getRateChangeDirection(from, to);
            String rateText = String.format("1 %s = %.4f %s", from, rate, to);
            if (!change.isEmpty()) {
                rateText += " " + change;
            }
            statusLbl.setText(rateText + " · " + fx.lastUpdated(from).split(" ")[1]); // Show time only

            if (commit) commitHistory.run();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  APP ICON  –  Robust loading for title bar + taskbar/dock
    // ─────────────────────────────────────────────────────────────────


    // ─────────────────────────────────────────────────────────────────
    //  MAIN
    // ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Initialize system theme detection
            UITheme.initializeSystemTheme();
            
            try {
                // Use system look and feel for better OS integration
                String systemLAF = UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(systemLAF);
            } catch (Exception e) {
                try {
                    // Fallback to cross-platform if system LAF fails
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {}
            }

            // --- ToolTip Styling ---
            UIManager.put("ToolTip.background", UITheme.BG_ELEVATED());
            UIManager.put("ToolTip.foreground", UITheme.TEXT_PRIMARY());
            UIManager.put("ToolTip.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.GLASS_BORDER(), 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));

            System.out.println("App starting. First run: " + AppConfig.isFirstRun() + 
                             ", Setup complete: " + AppConfig.isSetupComplete() +
                             ", Dark mode: " + UITheme.isDarkMode());
            
            if (AppConfig.isFirstRun()) {
                System.out.println("Showing setup wizard...");
                JFrame tempFrame = new JFrame();
                tempFrame.setUndecorated(true);
                tempFrame.pack();
                tempFrame.setLocationRelativeTo(null);

                SetupWizard wizard = new SetupWizard(tempFrame);
                wizard.setModal(true);
                wizard.setVisible(true);
                System.out.println("Wizard closed. Proceeding to calculator...");

                try {
                    GlassCalculator calc = new GlassCalculator();
                    calc.setVisible(true);
                    System.out.println("Calculator window shown.");
                } catch (Exception e) {
                    System.out.println("Error creating calculator: " + e.getMessage());
                    e.printStackTrace();
                }

                tempFrame.dispose();
            } else {
                System.out.println("Not first run, opening calculator directly.");
                new GlassCalculator().setVisible(true);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  THEME & STATUS REFRESH METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Refresh the UI theme after theme change
     */
    private void refreshTheme() {
        // Rebuild the UI with new theme
        SwingUtilities.invokeLater(() -> {
            try {
                // Update all components that use theme colors
                getContentPane().setBackground(UITheme.BG_DEEP());
                display.setBackground(UITheme.DISPLAY_BG());
                display.setForeground(UITheme.TEXT_PRIMARY());

                // Update status bar colors
                statusDB.setForeground(UITheme.ACCENT_RED);
                statusAngle.setForeground(UITheme.TEXT_DIM());
                statusMem.setForeground(UITheme.TEXT_DIM());

                // Update button panel background
                buttonPanel.setBackground(UITheme.BG_SURFACE());

                // Repaint all components
                repaint();
                revalidate();

                System.out.println("✅ Theme refreshed: " + UITheme.getThemeName());
            } catch (Exception e) {
                System.err.println("⚠️ Error refreshing theme: " + e.getMessage());
            }
        });
    }

    /**
     * Refresh the status bar with current values
     */
    public void refreshStatusBar() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Update memory display
                statusMem.setText("M: " + (memory == 0 ? "0" : fmt(memory)));
                statusMem.setForeground(memory == 0 ? UITheme.TEXT_DIM() : UITheme.ACCENT_AMBER);

                // Update angle mode display
                statusAngle.setText(radianMode ? "RAD" : "DEG");
                statusAngle.setForeground(UITheme.TEXT_DIM());

                // Update database status
                statusDB.setForeground(db.isReady() ? UITheme.ACCENT_GREEN : UITheme.ACCENT_RED);
                statusStorage.setForeground(db.isReady() ? UITheme.ACCENT_GREEN : UITheme.TEXT_DIM());

            } catch (Exception e) {
                System.err.println("⚠️ Error refreshing status bar: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  DPI SCALING UTILITY
    // ─────────────────────────────────────────────────────────────────
    private double getDPIScale() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            AffineTransform at = gc.getDefaultTransform();
            double scaleX = at.getScaleX();
            double scaleY = at.getScaleY();
            return Math.max(scaleX, scaleY);
        } catch (Exception e) {
            return 1.0; // Default scale if detection fails
        }
    }
}