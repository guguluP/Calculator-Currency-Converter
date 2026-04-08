package JAVA.Methods;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class GlassCalculator extends JFrame implements ActionListener, KeyListener {
    private JTextField display;
    private boolean startNewInput = true;
    private final String API_KEY = "97ab7ceab50c9baf51e43393";
    private final String[] currencies = {
            "USD", "EUR", "INR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "RUB",
            "BRL", "ZAR", "MXN", "SGD", "HKD", "SEK", "NOK", "DKK", "KRW", "TRY"
    };
    private List<String> history = new ArrayList<>();
    private JPanel buttonPanel;
    private enum Mode { BASIC, SCIENTIFIC }
    private Mode currentMode = Mode.BASIC;

    public GlassCalculator() {
        setTitle("Glass Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(20, 20, 25));
        setMacIcon();

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(0, 0));
        topPanel.setBackground(new Color(20, 20, 25));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 4));
        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Segoe UI", Font.PLAIN, 42));
        display.setBackground(new Color(30, 30, 35));
        display.setForeground(Color.WHITE);
        display.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        topPanel.add(display, BorderLayout.CENTER);

        JButton modeButton = new JButton("≡");
        modeButton.setFont(new Font("Segoe UI", Font.BOLD, 36));
        modeButton.setForeground(new Color(80, 200, 255));
        modeButton.setBackground(new Color(20, 20, 25));
        modeButton.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        modeButton.setFocusPainted(false);
        modeButton.setContentAreaFilled(false);
        modeButton.setOpaque(false);
        modeButton.addActionListener(e -> showModePopup(modeButton));
        topPanel.add(modeButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Button Panel
        buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(20, 20, 25));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buttonPanel, BorderLayout.CENTER);
        updateButtonPanel();

        // Keyboard Support
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        // Window preferences
        Preferences prefs = Preferences.userNodeForPackage(GlassCalculator.class);
        setSize(prefs.getInt("width", 400), prefs.getInt("height", 620));
        setLocation(prefs.getInt("x", 200), prefs.getInt("y", 150));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);
                p.putInt("width", getWidth());
                p.putInt("height", getHeight());
                p.putInt("x", getX());
                p.putInt("y", getY());
            }
        });
    }

    private void setMacIcon() {
        try {
            Image icnsIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/AppIcon.icns"));
            if (icnsIcon != null) setIconImage(icnsIcon);
            else {
                icnsIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("AppIcon.icns"));
                if (icnsIcon != null) setIconImage(icnsIcon);
            }
            if (icnsIcon == null) {
                Image pngIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/AppIcon.png"));
                if (pngIcon != null) setIconImage(pngIcon);
            }
            try {
                if (icnsIcon != null) {
                    java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                    if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                        taskbar.setIconImage(icnsIcon);
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.out.println("Could not load custom icon: " + e.getMessage());
        }
    }

    private void updateButtonPanel() {
        buttonPanel.removeAll();
        if (currentMode == Mode.BASIC) {
            buttonPanel.setLayout(new GridLayout(5, 4, 8, 8));
            String[] buttons = {"AC", "C", "%", "÷", "7", "8", "9", "×", "4", "5", "6", "−",
                    "1", "2", "3", "+", "0", ".", "±", "="};
            for (String text : buttons) {
                JButton btn = createGlassButton(text);
                btn.addActionListener(this);
                buttonPanel.add(btn);
            }
        } else {
            buttonPanel.setLayout(new GridLayout(7, 5, 8, 8));
            String[] sciButtons = {"AC", "C", "%", "÷", "√", "sin", "cos", "tan", "log", "ln",
                    "7", "8", "9", "×", "x²", "4", "5", "6", "−", "xʸ", "1", "2", "3", "+", "(",
                    "0", ".", "±", "=", ")", "π", "e", "1/x", "!", "INV"};
            for (String text : sciButtons) {
                JButton btn = createGlassButton(text);
                btn.addActionListener(this);
                if ("sin cos tan log ln √ x² xʸ π e 1/x ! INV".contains(text))
                    btn.setForeground(new Color(100, 220, 255));
                buttonPanel.add(btn);
            }
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? new Color(255, 255, 255, 70) :
                        getModel().isRollover() ? new Color(255, 255, 255, 45) :
                                new Color(255, 255, 255, 22);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(255, 255, 255, 55));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 20, 20);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, text.length() > 3 ? 18 : 24));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(18, 10, 18, 10));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        if ("÷×−+".contains(text)) btn.setForeground(new Color(255, 165, 50));
        if ("AC%C=±".contains(text)) btn.setForeground(new Color(80, 200, 255));
        return btn;
    }

    private void showModePopup(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(new Color(32, 32, 38));
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 80), 1),
                BorderFactory.createEmptyBorder(8, 4, 8, 4)));
        JMenuItem basicItem = new JMenuItem(currentMode == Mode.BASIC ? " ✓ Basic" : " Basic");
        stylePopupItem(basicItem);
        basicItem.addActionListener(e -> { currentMode = Mode.BASIC; updateButtonPanel(); });
        popup.add(basicItem);
        JMenuItem scientificItem = new JMenuItem(currentMode == Mode.SCIENTIFIC ? " ✓ Scientific" : " Scientific");
        stylePopupItem(scientificItem);
        scientificItem.addActionListener(e -> { currentMode = Mode.SCIENTIFIC; updateButtonPanel(); });
        popup.add(scientificItem);
        popup.addSeparator();
        JMenuItem convertItem = new JMenuItem(" Convert");
        stylePopupItem(convertItem);
        convertItem.addActionListener(e -> openCurrencyConverter());
        popup.add(convertItem);
        JMenuItem historyItem = new JMenuItem(" History");
        stylePopupItem(historyItem);
        historyItem.addActionListener(e -> openHistoryDialog());
        popup.add(historyItem);
        popup.show(invoker, invoker.getWidth() - 240, invoker.getHeight() + 4);
    }

    private void stylePopupItem(JMenuItem item) {
        item.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        item.setBackground(new Color(32, 32, 38));
        item.setForeground(Color.WHITE);
        item.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = ((JButton) e.getSource()).getText();
        switch (cmd) {
            case "AC" -> resetCalculator();
            case "C" -> handleBackspace();
            case "%" -> handlePercentage();
            case "±" -> handleSignChange();
            case "=" -> handleEquals();
            case "√" -> handleUnary("√(");
            case "x²" -> { display.setText(display.getText() + "^2"); startNewInput = false; }
            case "xʸ" -> handleOperator("xʸ");
            case "sin","cos","tan","log","ln" -> handleTrigOrLog(cmd);
            case "π" -> insertConstant(Math.PI);
            case "e" -> insertConstant(Math.E);
            case "1/x" -> { display.setText("1/(" + display.getText() + ")"); startNewInput = false; }
            case "!" -> handleFactorial();
            case "(" -> insertText("(");
            case ")" -> insertText(")");
            case "INV" -> {}
            default -> {
                if (cmd.matches("[0-9.]")) handleNumberOrDecimal(cmd);
                else if ("÷×−+".contains(cmd)) handleOperator(cmd);
            }
        }
    }

    // ====================== KEYBOARD SUPPORT ======================
    @Override
    public void keyPressed(KeyEvent e) {
        char ch = e.getKeyChar();
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_EQUALS) { handleEquals(); return; }
        if (key == KeyEvent.VK_BACK_SPACE) { handleBackspace(); return; }
        if (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_ESCAPE) { resetCalculator(); return; }
        switch (ch) {
            case '0','1','2','3','4','5','6','7','8','9','.' -> handleNumberOrDecimal(String.valueOf(ch));
            case '+','-','*','/','^' -> handleOperatorFromKeyboard(ch);
            case '(' -> insertText("(");
            case ')' -> insertText(")");
            case 's','S' -> handleTrigOrLog("sin");
            case 'c','C' -> handleTrigOrLog("cos");
            case 't','T' -> handleTrigOrLog("tan");
            case 'l','L' -> handleTrigOrLog("log");
            case 'n','N' -> handleTrigOrLog("ln");
            case 'p','P' -> insertConstant(Math.PI);
            case 'e','E' -> insertConstant(Math.E);
            case 'r','R' -> handleUnary("√(");
            case '!' -> handleFactorial();
            case '%' -> handlePercentage();
        }
    }

    private void handleOperatorFromKeyboard(char ch) {
        String op = switch (ch) {
            case '*' -> "×"; case '/' -> "÷"; case '-' -> "−"; case '+' -> "+"; case '^' -> "xʸ";
            default -> String.valueOf(ch);
        };
        handleOperator(op);
    }

    private void insertText(String text) {
        String current = display.getText();
        display.setText(startNewInput ? text : current + text);
        startNewInput = false;
    }

    private void insertConstant(double value) {
        String valStr = formatResult(value);
        String current = display.getText();
        display.setText((startNewInput || current.equals("0")) ? valStr : current + valStr);
        startNewInput = false;
    }

    // ====================== CORE METHODS ======================
    private void handleNumberOrDecimal(String cmd) {
        String current = display.getText();
        if (startNewInput) {
            display.setText(cmd.equals(".") ? "0." : cmd);
            startNewInput = false;
            return;
        }
        if (cmd.equals(".") && getLastNumber(current).contains(".")) return;
        if (current.equals("0") && !cmd.equals(".")) display.setText(cmd);
        else display.setText(current + cmd);
    }

    private String getLastNumber(String expr) {
        String[] parts = expr.split("[÷×−+^]");
        return parts.length > 0 ? parts[parts.length - 1].trim() : "";
    }

    private void handleBackspace() {
        String current = display.getText().trim();
        if (current.equals("0") || current.isEmpty()) return;
        String newText = current.substring(0, current.length() - 1).trim();
        if (newText.endsWith(" ")) newText = newText.substring(0, newText.length() - 1).trim();
        display.setText(newText.isEmpty() ? "0" : newText);
        startNewInput = false;
    }

    private void handlePercentage() {
        try {
            String current = display.getText().replace(" ", "");
            double val = Double.parseDouble(current);
            display.setText(formatResult(val / 100));
            startNewInput = true;
        } catch (Exception ignored) {}
    }

    private void handleOperator(String cmd) {
        String current = display.getText().trim();
        if (current.isEmpty() || endsWithOperator(current)) return;
        String symbol = switch (cmd) {
            case "÷" -> "÷"; case "×" -> "×"; case "−" -> "−"; case "+" -> "+"; case "xʸ" -> "^";
            default -> cmd;
        };
        display.setText(current + " " + symbol + " ");
        startNewInput = false;
    }

    private void handleTrigOrLog(String cmd) {
        String current = display.getText();
        display.setText(startNewInput ? cmd + "(" : current + cmd + "(");
        startNewInput = false;
    }

    private void handleUnary(String prefix) {
        String current = display.getText();
        display.setText(prefix + current + ")");
        startNewInput = false;
    }

    private void handleFactorial() {
        try {
            String current = display.getText().trim();
            if (current.isEmpty()) return;
            double num = Double.parseDouble(current);
            if (num < 0 || num != (long) num) { display.setText("Error"); return; }
            long fact = 1;
            for (long i = 2; i <= (long) num; i++) fact *= i;
            display.setText(String.valueOf(fact));
            startNewInput = true;
        } catch (Exception ex) { display.setText("Error"); }
    }

    private void handleSignChange() {
        String current = display.getText().trim();
        if (current.isEmpty() || current.equals("0") || current.equals("Error")) return;
        try {
            if (current.contains(" ")) {
                String last = getLastNumber(current);
                if (!last.isEmpty()) {
                    double val = Double.parseDouble(last);
                    String newLast = formatResult(-val);
                    display.setText(current.substring(0, current.lastIndexOf(last)) + newLast);
                }
            } else {
                double val = Double.parseDouble(current);
                display.setText(formatResult(-val));
            }
        } catch (Exception ignored) {}
    }

    private boolean endsWithOperator(String text) {
        if (text.isEmpty()) return false;
        char last = text.charAt(text.length() - 1);
        return "÷×−+^".indexOf(last) != -1;
    }

    // ====================== FIXED PURE-JAVA EVALUATOR (NO SCRIPTENGINE) ======================
    private void handleEquals() {
        String originalExpr = display.getText().trim();
        if (originalExpr.isEmpty()) return;

        try {
            double result = evaluateExpression(originalExpr);
            String resultStr = formatResult(result);
            display.setText(resultStr);
            history.add(0, originalExpr + " = " + resultStr);
            if (history.size() > 100) history.remove(history.size() - 1);
        } catch (Exception ex) {
            display.setText("Error");
        }
        startNewInput = true;
    }

    private double evaluateExpression(String expr) throws Exception {
        // Clean input
        String cleaned = expr.replace(" ", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("xʸ", "^");

        // Pure Java recursive parser (supports all features)
        return new ExpressionParser(cleaned).parse();
    }

    // ====================== TINY PURE-JAVA PARSER (no external deps) ======================
    private static class ExpressionParser {
        private final String input;
        private int pos = 0;

        ExpressionParser(String input) { this.input = input; }

        double parse() throws Exception {
            double value = parseExpression();
            if (pos < input.length()) throw new Exception("Invalid expression");
            return value;
        }

        private double parseExpression() throws Exception {
            double value = parseTerm();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double next = parseTerm();
                    value = (op == '+') ? value + next : value - next;
                } else break;
            }
            return value;
        }

        private double parseTerm() throws Exception {
            double value = parseFactor();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '*' || op == '/') {
                    pos++;
                    double next = parseFactor();
                    value = (op == '*') ? value * next : (next == 0 ? 0 : value / next);
                } else break;
            }
            return value;
        }

        private double parseFactor() throws Exception {
            double value = parsePrimary();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '^') {
                    pos++;
                    double exp = parseFactor(); // right-associative
                    value = Math.pow(value, exp);
                } else break;
            }
            return value;
        }

        private double parsePrimary() throws Exception {
            if (pos >= input.length()) throw new Exception("Unexpected end");

            char c = input.charAt(pos);
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }
            if (c == '(') {
                pos++;
                double value = parseExpression();
                if (pos < input.length() && input.charAt(pos) == ')') pos++;
                else throw new Exception("Missing )");
                return value;
            }
            if (c == 's' && input.startsWith("sin(", pos)) {
                pos += 4; double v = parseExpression(); expect(')'); return Math.sin(v);
            }
            if (c == 'c' && input.startsWith("cos(", pos)) {
                pos += 4; double v = parseExpression(); expect(')'); return Math.cos(v);
            }
            if (c == 't' && input.startsWith("tan(", pos)) {
                pos += 4; double v = parseExpression(); expect(')'); return Math.tan(v);
            }
            if (c == 'l' && input.startsWith("log(", pos)) {
                pos += 4; double v = parseExpression(); expect(')'); return Math.log10(v);
            }
            if (c == 'l' && input.startsWith("ln(", pos)) {
                pos += 3; double v = parseExpression(); expect(')'); return Math.log(v);
            }
            if (c == '√' && input.startsWith("√(", pos)) {
                pos += 2; double v = parseExpression(); expect(')'); return Math.sqrt(v);
            }
            if (input.startsWith("π", pos)) { pos += 1; return Math.PI; }
            if (input.startsWith("e", pos) && (pos + 1 >= input.length() || !Character.isLetter(input.charAt(pos+1)))) {
                pos += 1; return Math.E;
            }
            throw new Exception("Unknown token at " + pos);
        }

        private double parseNumber() {
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) pos++;
            return Double.parseDouble(input.substring(start, pos));
        }

        private void expect(char ch) throws Exception {
            if (pos < input.length() && input.charAt(pos) == ch) pos++;
            else throw new Exception("Expected " + ch);
        }
    }

   private String formatResult(double result) {
    if (Double.isNaN(result) || Double.isInfinite(result)) return "Error";
    
    double absResult = Math.abs(result);
    
    // Use clean scientific notation for very large (>= 10 billion) or very small numbers
    if (absResult >= 1e10 || (absResult > 0 && absResult < 1e-6)) {
        String sci = String.format("%.8g", result);
        return sci.replace('e', 'E');   // e.g. 1.23456789E+12 or 1.23456789E-7
    }
    
    // Normal nice formatting for regular numbers
    if (result == (long) result) {
        return String.valueOf((long) result);
    }
    
    String formatted = String.format("%.8f", result);
    return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
}

    private void resetCalculator() {
        display.setText("0");
        startNewInput = true;
    }

    // ====================== CURRENCY & HISTORY (unchanged) ======================

    // ... (openCurrencyConverter, fetchLiveRates, openHistoryDialog remain exactly the same as in the last full version I sent)

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    // ====================== CURRENCY CONVERTER ======================
    private void openCurrencyConverter() {
        JDialog dialog = new JDialog(this, "Live Currency Converter", true);
        dialog.setSize(460, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(new Color(20, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Live Currency Converter");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(80, 200, 255));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        dialog.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        dialog.add(new JLabel("Amount:"), gbc);
        JTextField amountField = new JTextField("1");
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        gbc.gridx = 1;
        dialog.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("From:"), gbc);
        JComboBox<String> fromBox = new JComboBox<>(currencies);
        fromBox.setSelectedItem("USD");
        fromBox.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        gbc.gridx = 1;
        dialog.add(fromBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dialog.add(new JLabel("To:"), gbc);
        JComboBox<String> toBox = new JComboBox<>(currencies);
        toBox.setSelectedItem("INR");
        toBox.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        gbc.gridx = 1;
        dialog.add(toBox, gbc);

        JLabel resultLabel = new JLabel("Click Convert to get live rate");
        resultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        resultLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        dialog.add(resultLabel, gbc);

        JLabel rateLabel = new JLabel(" ");
        rateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        rateLabel.setForeground(new Color(150, 150, 150));
        gbc.gridy = 5;
        dialog.add(rateLabel, gbc);

        JButton convertBtn = new JButton("Convert (Live)");
        convertBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        convertBtn.setBackground(new Color(80, 200, 255));
        convertBtn.setForeground(Color.BLACK);
        gbc.gridy = 6; gbc.gridwidth = 1; gbc.gridx = 0;
        dialog.add(convertBtn, gbc);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        gbc.gridx = 1;
        dialog.add(closeBtn, gbc);

        convertBtn.addActionListener(ev -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                String from = (String) fromBox.getSelectedItem();
                String to = (String) toBox.getSelectedItem();
                if (from.equals(to)) {
                    resultLabel.setText(String.format("%.4f %s = %.4f %s", amount, from, amount, to));
                    rateLabel.setText("1 " + from + " = 1 " + to);
                    return;
                }
                Map<String, Double> rates = fetchLiveRates(from);
                double rate = rates.getOrDefault(to, 0.0);
                double converted = amount * rate;
                resultLabel.setText(String.format("%.4f %s = %.4f %s", amount, from, converted, to));
                rateLabel.setText(String.format("1 %s = %.4f %s", from, rate, to));
            } catch (NumberFormatException ex) {
                resultLabel.setText("Invalid amount");
                rateLabel.setText("");
            } catch (Exception ex) {
                resultLabel.setText("Failed to fetch live rates");
                rateLabel.setText("Check your internet");
                ex.printStackTrace();
            }
        });
        closeBtn.addActionListener(ev -> dialog.dispose());
        dialog.setVisible(true);
    }

    private Map<String, Double> fetchLiveRates(String base) throws Exception {
        String urlStr = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/" + base;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }
        conn.disconnect();

        String jsonStr = content.toString();
        Map<String, Double> rates = new HashMap<>();
        int ratesStart = jsonStr.indexOf("\"conversion_rates\":{");
        if (ratesStart == -1) throw new Exception("Invalid API response");

        int openBrace = jsonStr.indexOf('{', ratesStart);
        int closeBrace = jsonStr.indexOf('}', openBrace);
        if (closeBrace == -1) closeBrace = jsonStr.length();

        String ratesSection = jsonStr.substring(openBrace + 1, closeBrace);
        String[] pairs = ratesSection.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String currency = keyValue[0].replace("\"", "").trim();
                try {
                    double rate = Double.parseDouble(keyValue[1].trim());
                    rates.put(currency, rate);
                } catch (NumberFormatException ignored) {}
            }
        }
        return rates;
    }

    // ====================== HISTORY DIALOG ======================
    private void openHistoryDialog() {
        JDialog dialog = new JDialog(this, "Calculation History", true);
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(20, 20, 25));

        JLabel title = new JLabel("Calculation History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(80, 200, 255));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(title, BorderLayout.NORTH);

        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        historyArea.setBackground(new Color(30, 30, 35));
        historyArea.setForeground(Color.WHITE);
        historyArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        if (history.isEmpty()) {
            historyArea.setText("No calculations performed yet.\n\nPress = on any calculation to add it here.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String entry : history) {
                sb.append(entry).append("\n\n");
            }
            historyArea.setText(sb.toString().trim());
        }

        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        btnPanel.setBackground(new Color(20, 20, 25));
        JButton clearBtn = new JButton("Clear History");
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        clearBtn.setBackground(new Color(220, 50, 50));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        closeBtn.setBackground(new Color(80, 200, 255));
        closeBtn.setForeground(Color.BLACK);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        clearBtn.addActionListener(ev -> {
            history.clear();
            historyArea.setText("History has been cleared.");
        });
        closeBtn.addActionListener(ev -> dialog.dispose());
        dialog.setVisible(true);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new GlassCalculator().setVisible(true);
        });
    }
}
