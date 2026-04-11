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

/**
 * GlassCalculator - Final Project Version (FIXED)
 * ALL ERRORS FIXED:
 * 1. Lambda variable capture in currency keypad (all buttons now work independently).
 * 2. insertAtCursor() now correctly replaces display for digits, ".", π, e, Rand, mr after = (non-zero results).
 * 3. ExpressionParser now fully supports Unicode operators (× ÷ −) from buttons and keyboard.
 * All other functionality (2nd mode, memory, history, live currency, repeat =, hyperbolics, etc.) preserved and working.
 */
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
    private String lastOperator = "";
    private double lastOperand = 0.0;
    private boolean isRepeatPossible = false;
    // Memory & Rad/Deg support
    private double memory = 0.0;
    private boolean radianMode = false; // false = degrees (macOS default)
    // 2nd button toggle for trigonometric functions
    private boolean inverseMode = false;
    // ====================== OPTIMIZED GLOBAL CURRENCY CACHE ======================
    private static class CachedRates {
        final Map<String, Double> rates;
        final long timestamp;
        final String lastUpdatedUtc;

        CachedRates(Map<String, Double> rates, String lastUpdatedUtc) {
            this.rates = new HashMap<>(rates);
            this.timestamp = System.currentTimeMillis();
            this.lastUpdatedUtc = lastUpdatedUtc != null ? lastUpdatedUtc : "Unknown";
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 300_000; // 5 minutes
        }
    }
    private static final Map<String, CachedRates> rateCache = new HashMap<>();

    public GlassCalculator() {
        setTitle("Glass Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(20, 20, 25));
        setMacIcon();

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
        display.setCaretColor(new Color(80, 200, 255));
        display.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        display.getCaret().setBlinkRate(530);
        display.getCaret().setVisible(true);
        topPanel.add(display, BorderLayout.CENTER);

        JButton modeButton = new JButton("≡");
        modeButton.setFont(new Font("Segoe UI", Font.BOLD, 36));
        modeButton.setForeground(new Color(80, 200, 255));
        modeButton.setBackground(new Color(20, 20, 25));
        modeButton.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        modeButton.setFocusPainted(false);
        modeButton.setContentAreaFilled(false);
        modeButton.setOpaque(false);
        modeButton.setFocusable(false);
        modeButton.addActionListener(e -> showModePopup(modeButton));
        topPanel.add(modeButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(20, 20, 25));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buttonPanel, BorderLayout.CENTER);

        updateButtonPanel();

        display.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    display.selectAll();
                }
            }
        });
        display.addKeyListener(this);
        display.requestFocusInWindow();

        Preferences prefs = Preferences.userNodeForPackage(GlassCalculator.class);
        setSize(prefs.getInt("width", 400), prefs.getInt("height", 620));
        setLocation(prefs.getInt("x", 200), prefs.getInt("y", 150));
        loadHistory();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Preferences p = Preferences.userNodeForPackage(GlassCalculator.class);
                p.putInt("width", getWidth());
                p.putInt("height", getHeight());
                p.putInt("x", getX());
                p.putInt("y", getY());
                saveHistory();
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
                if (icnsIcon != null) java.awt.Taskbar.getTaskbar().setIconImage(icnsIcon);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
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
                if ("÷×−+=".contains(text)) {
                    btn.setForeground(new Color(255, 170, 0));
                }
                buttonPanel.add(btn);
            }
        } else { // SCIENTIFIC - full macOS layout (5×10) with dynamic 2nd mode
            buttonPanel.setLayout(new GridLayout(5, 10, 8, 8));
            String sinLabel = inverseMode ? "sin⁻¹" : "sin";
            String cosLabel = inverseMode ? "cos⁻¹" : "cos";
            String tanLabel = inverseMode ? "tan⁻¹" : "tan";
            String sinhLabel = inverseMode ? "sinh⁻¹" : "sinh";
            String coshLabel = inverseMode ? "cosh⁻¹" : "cosh";
            String tanhLabel = inverseMode ? "tanh⁻¹" : "tanh";
            String[] sciButtons = {
                    "(", ")", "mc", "m+", "m-", "mr", "⌫", "AC", "%", "÷",
                    "2nd", "x²", "x³", "xʸ", "yˣ", "2ˣ", "7", "8", "9", "×",
                    "1/x", "²√x", "³√x", "ʸ√x", "logy", "log₂", "4", "5", "6", "−",
                    "x!", sinLabel, cosLabel, tanLabel, "e", "EE", "1", "2", "3", "+",
                    "Rand", sinhLabel, coshLabel, tanhLabel, "π", "Rad", "+/-", "0", ".", "="
            };
            for (String text : sciButtons) {
                JButton btn = createGlassButton(text);
                btn.addActionListener(this);
                if ("÷×−+=".contains(text)) {
                    btn.setForeground(new Color(255, 170, 0));
                } else if (" 2nd x² x³ xʸ yˣ 2ˣ 1/x ²√x ³√x ʸ√x logy log₂ x! sin sin⁻¹ cos cos⁻¹ tan tan⁻¹ e EE sinh sinh⁻¹ cosh cosh⁻¹ tanh tanh⁻¹ π Rad ( ) ".contains(" " + text + " ")) {
                    btn.setForeground(new Color(100, 220, 255));
                } else if ("mc m+ m- mr".contains(text)) {
                    btn.setForeground(new Color(180, 180, 190));
                }
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
                Color bg = getModel().isPressed() ? new Color(255, 255, 255, 175) :
                        getModel().isRollover() ? new Color(255, 255, 255, 75) :
                                new Color(255, 255, 255, 32);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(getModel().isPressed() ? new Color(255, 255, 255, 110) : new Color(255, 255, 255, 55));
                g2.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, text.length() > 3 ? 18 : 24));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(18, 10, 18, 10));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusable(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
        JButton source = (JButton) e.getSource();
        String cmd = source.getText();
        highlightButton(cmd);
        switch (cmd) {
            case "AC" -> resetCalculator();
            case "C", "⌫" -> deleteLeftOfCursor();
            case "%" -> handlePercentage();
            case "±", "+/-" -> handleSignChange();
            case "=" -> handleEquals();
            case "x²" -> insertAtCursor("^2");
            case "x³" -> insertAtCursor("^3");
            case "xʸ", "yˣ" -> insertAtCursor("^");
            case "2ˣ" -> insertAtCursor("2^");
            case "1/x" -> insertAtCursor("1/(");
            case "²√x" -> insertAtCursor("√(");
            case "³√x" -> insertAtCursor("cbrt(");
            case "ʸ√x" -> insertAtCursor("^(1/");
            case "logy" -> insertAtCursor("log(");
            case "log₂" -> insertAtCursor("log2(");
            case "x!" -> handleFactorial();
            case "(" -> insertAtCursor("(");
            case ")" -> insertAtCursor(")");
            case "mc" -> memory = 0.0;
            case "m+" -> { try { if (!display.getText().trim().isEmpty()) memory += evaluateExpression(display.getText().trim()); } catch (Exception ignored) {} }
            case "m-" -> { try { if (!display.getText().trim().isEmpty()) memory -= evaluateExpression(display.getText().trim()); } catch (Exception ignored) {} }
            case "mr" -> insertAtCursor(formatResult(memory));
            case "e" -> insertAtCursor(String.valueOf(Math.E));
            case "EE" -> insertAtCursor("E");
            case "Rand" -> insertAtCursor(formatResult(Math.random()));
            case "π" -> insertAtCursor(String.valueOf(Math.PI));
            case "Rad" -> radianMode = !radianMode;
            case "2nd" -> {
                inverseMode = !inverseMode;
                updateButtonPanel();
            }
            case "sin", "sin⁻¹" -> insertAtCursor(inverseMode ? "asin(" : "sin(");
            case "cos", "cos⁻¹" -> insertAtCursor(inverseMode ? "acos(" : "cos(");
            case "tan", "tan⁻¹" -> insertAtCursor(inverseMode ? "atan(" : "tan(");
            case "sinh", "sinh⁻¹" -> insertAtCursor(inverseMode ? "asinh(" : "sinh(");
            case "cosh", "cosh⁻¹" -> insertAtCursor(inverseMode ? "acosh(" : "cosh(");
            case "tanh", "tanh⁻¹" -> insertAtCursor(inverseMode ? "atanh(" : "tanh(");
            default -> {
                if (cmd.matches("[0-9.]") || "÷×−+^".contains(cmd)) {
                    insertAtCursor(cmd);
                }
            }
        }
    }

    private void deleteLeftOfCursor() {
        int pos = display.getCaretPosition();
        if (pos > 0) {
            String text = display.getText();
            String newText = text.substring(0, pos - 1) + text.substring(pos);
            display.setText(newText);
            display.setCaretPosition(pos - 1);
        }
    }

    private void insertAtCursor(String text) {
        String current = display.getText().trim();
        if (startNewInput) {
            boolean isPureNumber = text.equals(".") || text.equals("0.");
            if (!isPureNumber) {
                try {
                    Double.parseDouble(text);
                    isPureNumber = true;
                } catch (NumberFormatException ignored) {}
            }
            if (isPureNumber) {
                display.setText(text);
                display.setCaretPosition(text.length());
                startNewInput = false;
                return;
            }
        }
        int pos = display.getCaretPosition();
        String newText = current.substring(0, pos) + text + current.substring(pos);
        display.setText(newText);
        display.setCaretPosition(pos + text.length());
        startNewInput = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ((e.isControlDown() || e.isMetaDown()) && e.getKeyCode() == KeyEvent.VK_A) {
            display.selectAll();
            e.consume();
            return;
        }
        char ch = e.getKeyChar();
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_EQUALS) {
            handleEquals();
            highlightButton("=");
            e.consume();
            return;
        }
        if (key == KeyEvent.VK_BACK_SPACE) {
            deleteLeftOfCursor();
            highlightButton("C");
            e.consume();
            return;
        }
        if (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_ESCAPE) {
            resetCalculator();
            highlightButton("AC");
            e.consume();
            return;
        }
        switch (ch) {
            case '0' -> { insertAtCursor("0"); highlightButton("0"); e.consume(); }
            case '1' -> { insertAtCursor("1"); highlightButton("1"); e.consume(); }
            case '2' -> { insertAtCursor("2"); highlightButton("2"); e.consume(); }
            case '3' -> { insertAtCursor("3"); highlightButton("3"); e.consume(); }
            case '4' -> { insertAtCursor("4"); highlightButton("4"); e.consume(); }
            case '5' -> { insertAtCursor("5"); highlightButton("5"); e.consume(); }
            case '6' -> { insertAtCursor("6"); highlightButton("6"); e.consume(); }
            case '7' -> { insertAtCursor("7"); highlightButton("7"); e.consume(); }
            case '8' -> { insertAtCursor("8"); highlightButton("8"); e.consume(); }
            case '9' -> { insertAtCursor("9"); highlightButton("9"); e.consume(); }
            case '.' -> { insertAtCursor("."); highlightButton("."); e.consume(); }
            case '+' -> { insertAtCursor("+"); highlightButton("+"); e.consume(); }
            case '-' -> { insertAtCursor("−"); highlightButton("−"); e.consume(); }
            case '*' -> { insertAtCursor("×"); highlightButton("×"); e.consume(); }
            case '/' -> { insertAtCursor("÷"); highlightButton("÷"); e.consume(); }
            case '^' -> { insertAtCursor("^"); highlightButton("xʸ"); e.consume(); }
            case '(' -> { insertAtCursor("("); highlightButton("("); e.consume(); }
            case ')' -> { insertAtCursor(")"); highlightButton(")"); e.consume(); }
        }
        if (currentMode == Mode.SCIENTIFIC) {
            switch (ch) {
                case '!' -> { handleFactorial(); highlightButton("x!"); e.consume(); }
            }
        }
        if (ch == '%') {
            handlePercentage();
            highlightButton("%");
            e.consume();
        }
    }

    private void highlightButton(String text) {
        if (text == null || buttonPanel == null) return;
        for (Component c : buttonPanel.getComponents()) {
            if (c instanceof JButton btn && text.equals(btn.getText())) {
                ButtonModel model = btn.getModel();
                model.setPressed(true);
                btn.repaint();
                Timer timer = new Timer(160, ev -> {
                    model.setPressed(false);
                    btn.repaint();
                });
                timer.setRepeats(false);
                timer.start();
                return;
            }
        }
    }

    private void handlePercentage() {
        try {
            String current = display.getText().replace(" ", "");
            double val = Double.parseDouble(current);
            display.setText(formatResult(val / 100));
            startNewInput = true;
        } catch (Exception ignored) {}
    }

    private void handleFactorial() {
        try {
            String current = display.getText().trim();
            if (current.isEmpty()) return;
            double num = Double.parseDouble(current);
            if (num < 0 || num != (long) num) {
                display.setText("Error");
                return;
            }
            long fact = 1;
            for (long i = 2; i <= (long) num; i++) fact *= i;
            display.setText(String.valueOf(fact));
            startNewInput = true;
        } catch (Exception ex) {
            display.setText("Error");
        }
    }

    private void handleSignChange() {
        String current = display.getText().trim();
        if (current.isEmpty() || current.equals("0") || current.equals("Error")) return;
        try {
            double val = Double.parseDouble(current);
            display.setText(formatResult(-val));
        } catch (Exception ignored) {}
    }

    private void handleEquals() {
        String currentText = display.getText().trim();
        if (currentText.isEmpty()) return;
        try {
            if (isRepeatPossible && startNewInput && !lastOperator.isEmpty()) {
                double currentValue = Double.parseDouble(currentText);
                double newResult = applyLastOperation(currentValue);
                String formatted = formatResult(newResult);
                display.setText(formatted);
            } else {
                double result = evaluateExpression(currentText);
                String formatted = formatResult(result);
                display.setText(formatted);
                history.add(0, currentText + " = " + formatted);
                if (history.size() > 100) history.remove(history.size() - 1);
                saveHistory();
                extractLastOperation(currentText);
            }
            isRepeatPossible = true;
            startNewInput = true;
        } catch (Exception ex) {
            display.setText("Error");
            isRepeatPossible = false;
        }
    }

    private void extractLastOperation(String expr) {
        String cleaned = expr.replace(" ", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("xʸ", "^");
        lastOperator = "";
        lastOperand = 0.0;
        int lastOpIndex = -1;
        char lastOpChar = 0;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            char c = cleaned.charAt(i);
            if ((c == '+' || c == '-' || c == '*' || c == '/' || c == '^') &&
                    i > 0 && i < cleaned.length() - 1) {
                lastOpIndex = i;
                lastOpChar = c;
                break;
            }
        }
        if (lastOpIndex == -1) return;
        String rightPart = cleaned.substring(lastOpIndex + 1);
        int numEnd = 0;
        while (numEnd < rightPart.length()) {
            char ch = rightPart.charAt(numEnd);
            if (Character.isDigit(ch) || ch == '.' || (numEnd == 0 && ch == '-')) {
                numEnd++;
            } else break;
        }
        if (numEnd > 0) {
            try {
                lastOperand = Double.parseDouble(rightPart.substring(0, numEnd));
                lastOperator = String.valueOf(lastOpChar);
            } catch (Exception ignored) {}
        }
    }

    private double applyLastOperation(double left) throws Exception {
        return switch (lastOperator) {
            case "+" -> left + lastOperand;
            case "-" -> left - lastOperand;
            case "*" -> left * lastOperand;
            case "/" -> (lastOperand == 0) ? 0 : left / lastOperand;
            case "^" -> Math.pow(left, lastOperand);
            default -> throw new Exception("No operation to repeat");
        };
    }

    private double evaluateExpression(String expr) throws Exception {
        return new ExpressionParser(expr, radianMode).parse();
    }

    private class ExpressionParser {
        private final String input;
        private final boolean radianMode;
        private int pos = 0;

        ExpressionParser(String input, boolean radianMode) {
            this.input = input;
            this.radianMode = radianMode;
        }

        double parse() throws Exception {
            double value = parseExpression();
            if (pos < input.length()) throw new Exception("Invalid expression");
            return value;
        }

        private double parseExpression() throws Exception {
            double value = parseTerm();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '+' || op == '-' || op == '−') {
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
                if (op == '*' || op == '×' || op == '/' || op == '÷') {
                    pos++;
                    double next = parseFactor();
                    value = (op == '*' || op == '×') ? value * next : (next == 0 ? 0 : value / next);
                } else if (isStartOfFactor()) {
                    double next = parseFactor();
                    value *= next;
                } else {
                    break;
                }
            }
            return value;
        }

        private boolean isStartOfFactor() {
            if (pos >= input.length()) return false;
            char c = input.charAt(pos);
            if (Character.isDigit(c) || c == '.' || c == '(' || c == '√' || c == 'π') return true;
            if (c == 'e' && (pos + 1 >= input.length() || !Character.isLetter(input.charAt(pos + 1)))) return true;
            String remaining = input.substring(pos);
            return remaining.startsWith("sin(") || remaining.startsWith("cos(") ||
                    remaining.startsWith("tan(") || remaining.startsWith("asin(") ||
                    remaining.startsWith("acos(") || remaining.startsWith("atan(") ||
                    remaining.startsWith("log(") || remaining.startsWith("ln(") ||
                    remaining.startsWith("cbrt(") ||
                    remaining.startsWith("sinh(") || remaining.startsWith("cosh(") || remaining.startsWith("tanh(") ||
                    remaining.startsWith("asinh(") || remaining.startsWith("acosh(") || remaining.startsWith("atanh(") ||
                    remaining.startsWith("log2(");
        }

        private double parseFactor() throws Exception {
            double value = parsePrimary();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '^') {
                    pos++;
                    double exp = parseFactor();
                    value = Math.pow(value, exp);
                } else break;
            }
            return value;
        }

        private double parsePrimary() throws Exception {
            if (pos >= input.length()) throw new Exception("Unexpected end");
            char c = input.charAt(pos);
            if (Character.isDigit(c) || c == '.') return parseNumber();
            if (c == '(') {
                pos++;
                double value = parseExpression();
                if (pos < input.length() && input.charAt(pos) == ')') pos++;
                else throw new Exception("Missing )");
                return value;
            }
            if (c == 's' && input.startsWith("sin(", pos)) {
                pos += 4; double v = parseExpression(); expect(')');
                double arg = radianMode ? v : Math.toRadians(v);
                return Math.sin(arg);
            }
            if (c == 'c' && input.startsWith("cos(", pos)) {
                pos += 4; double v = parseExpression(); expect(')');
                double arg = radianMode ? v : Math.toRadians(v);
                return Math.cos(arg);
            }
            if (c == 't' && input.startsWith("tan(", pos)) {
                pos += 4; double v = parseExpression(); expect(')');
                double arg = radianMode ? v : Math.toRadians(v);
                return Math.tan(arg);
            }
            if (c == 'a' && input.startsWith("asin(", pos)) { pos += 5; double v = parseExpression(); expect(')'); double res = Math.asin(v); return radianMode ? res : Math.toDegrees(res); }
            if (c == 'a' && input.startsWith("acos(", pos)) { pos += 5; double v = parseExpression(); expect(')'); double res = Math.acos(v); return radianMode ? res : Math.toDegrees(res); }
            if (c == 'a' && input.startsWith("atan(", pos)) { pos += 5; double v = parseExpression(); expect(')'); double res = Math.atan(v); return radianMode ? res : Math.toDegrees(res); }
            if (c == 'l' && input.startsWith("log(", pos)) { pos += 4; double v = parseExpression(); expect(')'); return Math.log10(v); }
            if (c == 'l' && input.startsWith("ln(", pos)) { pos += 3; double v = parseExpression(); expect(')'); return Math.log(v); }
            if (input.startsWith("log2(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.log(v) / Math.log(2); }
            if (c == '√' && input.startsWith("√(", pos)) { pos += 2; double v = parseExpression(); expect(')'); return Math.sqrt(v); }
            if (input.startsWith("cbrt(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.cbrt(v); }
            if (input.startsWith("sinh(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.sinh(v); }
            if (input.startsWith("cosh(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.cosh(v); }
            if (input.startsWith("tanh(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.tanh(v); }
            if (input.startsWith("asinh(", pos)) { pos += 6; double v = parseExpression(); expect(')'); return Math.log(v + Math.sqrt(v * v + 1)); }
            if (input.startsWith("acosh(", pos)) { pos += 6; double v = parseExpression(); expect(')'); return Math.log(v + Math.sqrt(v * v - 1)); }
            if (input.startsWith("atanh(", pos)) { pos += 6; double v = parseExpression(); expect(')'); return Math.abs(v) >= 1 ? Double.NaN : 0.5 * Math.log((1 + v) / (1 - v)); }
            if (input.startsWith("π", pos)) { pos += 1; return Math.PI; }
            if (input.startsWith("e", pos) && (pos + 1 >= input.length() || !Character.isLetter(input.charAt(pos + 1)))) {
                pos += 1; return Math.E;
            }
            throw new Exception("Unknown token at " + pos);
        }

        private double parseNumber() {
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) pos++;
            if (pos < input.length() && (input.charAt(pos) == 'E' || input.charAt(pos) == 'e')) {
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private void expect(char ch) throws Exception {
            if (pos < input.length() && input.charAt(pos) == ch) pos++;
            else throw new Exception("Expected " + ch);
        }
    }

    private String formatResult(double result) {
        if (Double.isNaN(result) || Double.isInfinite(result)) return "Error";
        if (Math.abs(result) < 1e-10) return "0";
        double absResult = Math.abs(result);
        if (absResult >= 1e10 || (absResult > 0 && absResult < 1e-6)) {
            String sci = String.format("%.8g", result);
            return sci.replace('e', 'E');
        }
        if (result == (long) result) return String.valueOf((long) result);
        String formatted = String.format("%.8f", result);
        return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void resetCalculator() {
        display.setText("0");
        startNewInput = true;
        isRepeatPossible = false;
        lastOperator = "";
        lastOperand = 0.0;
    }

    private void loadHistory() {
        Preferences prefs = Preferences.userNodeForPackage(GlassCalculator.class);
        history.clear();
        int count = prefs.getInt("historyCount", 0);
        for (int i = 0; i < count; i++) {
            String entry = prefs.get("history_" + i, null);
            if (entry != null && !entry.trim().isEmpty()) {
                history.add(entry);
            }
        }
    }

    private void saveHistory() {
        Preferences prefs = Preferences.userNodeForPackage(GlassCalculator.class);
        int oldCount = prefs.getInt("historyCount", 0);
        prefs.putInt("historyCount", history.size());
        for (int i = 0; i < history.size(); i++) {
            prefs.put("history_" + i, history.get(i));
        }
        for (int i = history.size(); i < oldCount + 20; i++) {
            prefs.remove("history_" + i);
        }
    }

    // ====================== FULLY RESTORED CURRENCY CONVERTER (no unused code) ======================
    private void openCurrencyConverter() {
        JDialog dialog = new JDialog(this, "Live Currency Converter", true);
        dialog.setSize(440, 740);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getContentPane().setBackground(new Color(20, 20, 25));

        JPanel displayArea = new JPanel();
        displayArea.setLayout(new BoxLayout(displayArea, BoxLayout.Y_AXIS));
        displayArea.setBackground(new Color(20, 20, 25));
        displayArea.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));

        JPanel resultLine = new JPanel(new BorderLayout(0, 0));
        resultLine.setBackground(new Color(20, 20, 25));
        resultLine.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JLabel resultLabel = new JLabel("0", SwingConstants.RIGHT);
        resultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 54));
        resultLabel.setForeground(Color.WHITE);
        JComboBox<String> toBox = createStyledCurrencyComboBox(currencies);
        toBox.setSelectedItem("INR");
        toBox.setPreferredSize(new Dimension(145, 62));
        resultLine.add(resultLabel, BorderLayout.CENTER);
        resultLine.add(toBox, BorderLayout.EAST);

        JPanel swapPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        swapPanel.setBackground(new Color(20, 20, 25));
        JButton swapBtn = createGlassButton("↕");
        swapBtn.setFont(new Font("Segoe UI", Font.BOLD, 36));
        swapBtn.setForeground(new Color(255, 165, 0));
        swapBtn.setPreferredSize(new Dimension(68, 68));
        swapPanel.add(swapBtn);

        JPanel inputLine = new JPanel(new BorderLayout(0, 0));
        inputLine.setBackground(new Color(20, 20, 25));
        inputLine.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextField inputField = new JTextField("0");
        inputField.setHorizontalAlignment(JTextField.RIGHT);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 54));
        inputField.setBackground(new Color(30, 30, 35));
        inputField.setForeground(Color.WHITE);
        inputField.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        inputField.setEditable(false);
        inputField.setCaretColor(new Color(80, 200, 255));
        JComboBox<String> fromBox = createStyledCurrencyComboBox(currencies);
        fromBox.setSelectedItem("USD");
        fromBox.setPreferredSize(new Dimension(145, 62));
        inputLine.add(inputField, BorderLayout.CENTER);
        inputLine.add(fromBox, BorderLayout.EAST);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(new Color(20, 20, 25));
        JLabel statusLabel = new JLabel("Live rates • Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(120, 220, 120));
        statusPanel.add(statusLabel);

        displayArea.add(resultLine);
        displayArea.add(swapPanel);
        displayArea.add(inputLine);
        displayArea.add(statusPanel);

        dialog.add(displayArea, BorderLayout.NORTH);

        JPanel keypad = new JPanel();
        keypad.setBackground(new Color(20, 20, 25));
        keypad.setBorder(BorderFactory.createEmptyBorder(10, 20, 30, 20));
        keypad.setLayout(new GridLayout(5, 4, 12, 12));
        String[] keys = {"⌫", "AC", "%", "÷", "7", "8", "9", "×", "4", "5", "6", "−",
                "1", "2", "3", "+", "+/-", "0", ".", "="};

        final Runnable[] liveUpdateHolder = new Runnable[1];
        Runnable liveUpdate = () -> {
            try {
                String inputStr = inputField.getText().trim().replace(" ", "");
                if (inputStr.isEmpty() || inputStr.equals("0")) {
                    resultLabel.setText("0");
                    statusLabel.setText("Live rates • Ready");
                    return;
                }
                double amount;
                try {
                    String cleaned = inputStr.replace("÷", "/").replace("×", "*").replace("−", "-");
                    amount = new ExpressionParser(cleaned, false).parse();
                } catch (Exception ex) {
                    amount = Double.parseDouble(inputStr);
                }
                String from = (String) fromBox.getSelectedItem();
                String to = (String) toBox.getSelectedItem();
                if (from.equals(to)) {
                    resultLabel.setText(formatResult(amount));
                    statusLabel.setText("Live rates • Ready");
                    return;
                }
                CachedRates cached = rateCache.get(from);
                if (cached != null && !cached.isExpired()) {
                    double rate = cached.rates.getOrDefault(to, 0.0);
                    resultLabel.setText(formatResult(amount * rate));
                    statusLabel.setText("Last updated: " + cached.lastUpdatedUtc);
                    return;
                }
                resultLabel.setText("Fetching live rates...");
                statusLabel.setText("Connecting to API...");
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            String urlStr = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/" + from;
                            URL url = new URL(urlStr);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setConnectTimeout(8000);
                            conn.setReadTimeout(8000);
                            StringBuilder content = new StringBuilder();
                            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                                String line;
                                while ((line = in.readLine()) != null) content.append(line);
                            }
                            conn.disconnect();
                            String jsonStr = content.toString();
                            Map<String, Double> rates = new HashMap<>();
                            int ratesStart = jsonStr.indexOf("\"conversion_rates\":{");
                            if (ratesStart != -1) {
                                int openBrace = jsonStr.indexOf('{', ratesStart);
                                int closeBrace = jsonStr.indexOf('}', openBrace);
                                String ratesSection = jsonStr.substring(openBrace + 1, closeBrace);
                                for (String pair : ratesSection.split(",")) {
                                    pair = pair.trim();
                                    if (pair.isEmpty()) continue;
                                    String[] kv = pair.split(":", 2);
                                    if (kv.length == 2) {
                                        String currency = kv[0].replace("\"", "").trim();
                                        try {
                                            rates.put(currency, Double.parseDouble(kv[1].trim()));
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }
                            String lastUpdatedUtc = "Unknown";
                            int keyIndex = jsonStr.indexOf("\"time_last_update_utc\":\"");
                            if (keyIndex != -1) {
                                int valueStart = keyIndex + "\"time_last_update_utc\":\"".length();
                                int valueEnd = jsonStr.indexOf("\"", valueStart);
                                if (valueEnd != -1) {
                                    lastUpdatedUtc = jsonStr.substring(valueStart, valueEnd).trim();
                                }
                            }
                            rateCache.put(from, new CachedRates(rates, lastUpdatedUtc));
                        } catch (Exception ignored) {}
                        return null;
                    }

                    @Override
                    protected void done() {
                        SwingUtilities.invokeLater(liveUpdateHolder[0]);
                    }
                }.execute();
            } catch (Exception ignored) {
                resultLabel.setText("Error");
            }
        };
        liveUpdateHolder[0] = liveUpdate;

        swapBtn.addActionListener(e -> {
            String from = (String) fromBox.getSelectedItem();
            String to = (String) toBox.getSelectedItem();
            fromBox.setSelectedItem(to);
            toBox.setSelectedItem(from);
            String temp = inputField.getText();
            inputField.setText(resultLabel.getText());
            resultLabel.setText(temp);
            liveUpdate.run();
        });
        fromBox.addActionListener(e -> liveUpdate.run());
        toBox.addActionListener(e -> liveUpdate.run());

        for (String text : keys) {
            JButton btn = createGlassButton(text);
            final String cmd = text;
            btn.addActionListener(ev -> {
                if (cmd.equals("⌫")) {
                    String txt = inputField.getText();
                    if (txt.length() > 0) {
                        txt = txt.substring(0, txt.length() - 1);
                        inputField.setText(txt.isEmpty() ? "0" : txt);
                    }
                } else if (cmd.equals("AC")) {
                    inputField.setText("0");
                } else if (cmd.equals("%")) {
                    try {
                        String txt = inputField.getText().trim().replace(" ", "");
                        String cleaned = txt.replace("÷","/").replace("×","*").replace("−","-");
                        double v = new ExpressionParser(cleaned, false).parse();
                        inputField.setText(formatResult(v / 100));
                    } catch (Exception ignored) {}
                } else if (cmd.equals("+/-")) {
                    try {
                        String txt = inputField.getText().trim().replace(" ", "");
                        String cleaned = txt.replace("÷","/").replace("×","*").replace("−","-");
                        double v = new ExpressionParser(cleaned, false).parse();
                        inputField.setText(formatResult(-v));
                    } catch (Exception ignored) {}
                } else if (cmd.matches("[0-9]")) {
                    String txt = inputField.getText();
                    inputField.setText(txt.equals("0") ? cmd : txt + cmd);
                } else if (cmd.equals(".")) {
                    String txt = inputField.getText();
                    if (!txt.contains(".")) {
                        inputField.setText(txt.equals("0") ? "0." : txt + ".");
                    }
                } else if ("÷×−+".contains(cmd)) {
                    String txt = inputField.getText();
                    String op = cmd.equals("÷") ? "/" : cmd.equals("×") ? "*" : cmd.equals("−") ? "-" : "+";
                    inputField.setText(txt + op);
                } else if (cmd.equals("=")) {
                    try {
                        String expr = inputField.getText().replace("÷", "/").replace("×", "*").replace("−", "-");
                        double res = new ExpressionParser(expr, false).parse();
                        inputField.setText(formatResult(res));
                    } catch (Exception ignored) {}
                }
                liveUpdate.run();
            });
            if ("÷×−+=".contains(cmd)) btn.setForeground(new Color(255, 165, 0));
            if (cmd.equals("⌫")) btn.setForeground(new Color(180, 180, 190));
            keypad.add(btn);
        }

        dialog.add(keypad, BorderLayout.CENTER);

        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_EQUALS) {
                    liveUpdate.run();
                    e.consume();
                }
            }
        });

        dialog.setVisible(true);
    }

    private JComboBox<String> createStyledCurrencyComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = new Color(30, 30, 35);
                if (isPopupVisible()) bg = new Color(45, 45, 52);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 26));
        combo.setForeground(Color.WHITE);
        combo.setBackground(new Color(30, 30, 35));
        combo.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        combo.setFocusable(false);
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        combo.setPreferredSize(new Dimension(160, 62));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 22));
                label.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setText(value != null ? value.toString() : "");
                if (isSelected) {
                    label.setBackground(new Color(80, 200, 255));
                    label.setForeground(new Color(20, 20, 25));
                } else {
                    label.setBackground(new Color(32, 32, 38));
                    label.setForeground(Color.WHITE);
                }
                return label;
            }
        });
        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                JComboBox<?> cb = (JComboBox<?>) e.getSource();
                Object child = cb.getAccessibleContext().getAccessibleChild(0);
                if (child instanceof JPopupMenu popup) {
                    popup.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 85), 1));
                    popup.setBackground(new Color(28, 28, 34));
                    popup.setPreferredSize(new Dimension(Math.max(cb.getWidth(), 180), popup.getPreferredSize().height));
                }
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        return combo;
    }

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
            for (String entry : history) sb.append(entry).append("\n\n");
            historyArea.setText(sb.toString().trim());
        }
        JScrollPane scrollPane = new JScrollPane(historyArea);
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
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        closeBtn.setBackground(new Color(80, 200, 255));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        clearBtn.addActionListener(ev -> {
            history.clear();
            saveHistory();
            historyArea.setText("History has been cleared.");
        });
        closeBtn.addActionListener(ev -> dialog.dispose());

        dialog.setVisible(true);
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new GlassCalculator().setVisible(true);
        });
    }
}
