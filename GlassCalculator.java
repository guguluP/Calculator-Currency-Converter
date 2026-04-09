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
 * GlassCalculator - Final Project Version
 * Trigonometric keyboard shortcuts strictly disabled in Basic mode
 * 
 * FEATURES:
 * - Persistent history (saved across restarts)
 * - Ctrl+A / Cmd+A and double-click to select all
 * - Double Equals repeat (2×2=4=8=16=32...)
 * - NEW: Keyboard = / Enter support in Currency Converter
 *   (Just type amount and press = or Enter → instant live conversion)
 */
public class GlassCalculator extends JFrame implements ActionListener, KeyListener {
    private JTextField display;
    private boolean startNewInput = true;
    private boolean inverseMode = false;
    private final String API_KEY = "97ab7ceab50c9baf51e43393";
    private final String[] currencies = {
            "USD", "EUR", "INR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "RUB",
            "BRL", "ZAR", "MXN", "SGD", "HKD", "SEK", "NOK", "DKK", "KRW", "TRY"
    };
    private List<String> history = new ArrayList<>();
    private JPanel buttonPanel;
    private enum Mode { BASIC, SCIENTIFIC }
    private Mode currentMode = Mode.BASIC;

    // Double Equals repeat feature
    private String lastOperator = "";
    private double lastOperand = 0.0;
    private boolean isRepeatPossible = false;

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

        // Double-click selects all
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

    private void updateDisplay(String text) {
        display.setText(text);
        SwingUtilities.invokeLater(() -> display.setCaretPosition(display.getText().length()));
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

    private void toggleInverseMode() {
        inverseMode = !inverseMode;
        highlightButton("INV");
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
            case "C" -> deleteLeftOfCursor();
            case "%" -> handlePercentage();
            case "±" -> handleSignChange();
            case "=" -> handleEquals();
            case "√" -> insertAtCursor("√(");
            case "x²" -> insertAtCursor("^2");
            case "xʸ" -> insertAtCursor("^");
            case "sin" -> insertAtCursor(inverseMode ? "asin(" : "sin(");
            case "cos" -> insertAtCursor(inverseMode ? "acos(" : "cos(");
            case "tan" -> insertAtCursor(inverseMode ? "atan(" : "tan(");
            case "log" -> insertAtCursor("log(");
            case "ln" -> insertAtCursor("ln(");
            case "π" -> insertAtCursor(String.valueOf(Math.PI));
            case "e" -> insertAtCursor(String.valueOf(Math.E));
            case "1/x" -> insertAtCursor("1/(");
            case "!" -> handleFactorial();
            case "(" -> insertAtCursor("(");
            case ")" -> insertAtCursor(")");
            case "INV" -> toggleInverseMode();
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
        if (startNewInput && (current.equals("0") || current.equals("0."))) {
            display.setText(text);
            display.setCaretPosition(text.length());
        } else {
            int pos = display.getCaretPosition();
            String newText = current.substring(0, pos) + text + current.substring(pos);
            display.setText(newText);
            display.setCaretPosition(pos + text.length());
        }
        startNewInput = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A ||
            e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_A) {
            display.selectAll();
            e.consume();
            return;
        }

        char ch = e.getKeyChar();
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_EQUALS) { handleEquals(); highlightButton("="); e.consume(); return; }
        if (key == KeyEvent.VK_BACK_SPACE) { deleteLeftOfCursor(); highlightButton("C"); e.consume(); return; }
        if (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_ESCAPE) { resetCalculator(); highlightButton("AC"); e.consume(); return; }

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
                case 's','S' -> { insertAtCursor(inverseMode ? "asin(" : "sin("); highlightButton("sin"); e.consume(); }
                case 'c','C' -> { insertAtCursor(inverseMode ? "acos(" : "cos("); highlightButton("cos"); e.consume(); }
                case 't','T' -> { insertAtCursor(inverseMode ? "atan(" : "tan("); highlightButton("tan"); e.consume(); }
                case 'l','L' -> { insertAtCursor("log("); highlightButton("log"); e.consume(); }
                case 'n','N' -> { insertAtCursor("ln("); highlightButton("ln"); e.consume(); }
                case 'p','P' -> { insertAtCursor(String.valueOf(Math.PI)); highlightButton("π"); e.consume(); }
                case 'e','E' -> { insertAtCursor(String.valueOf(Math.E)); highlightButton("e"); e.consume(); }
                case 'r','R' -> { insertAtCursor("√("); highlightButton("√"); e.consume(); }
                case '!' -> { handleFactorial(); highlightButton("!"); e.consume(); }
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
            if (Character.isDigit(ch) || ch == '.' ||
                (numEnd == 0 && ch == '-')) {
                numEnd++;
            } else {
                break;
            }
        }

        if (numEnd > 0) {
            String numberStr = rightPart.substring(0, numEnd);
            try {
                lastOperand = Double.parseDouble(numberStr);
                lastOperator = String.valueOf(lastOpChar);
            } catch (Exception ignored) {}
        }
    }

    private double applyLastOperation(double left) throws Exception {
        switch (lastOperator) {
            case "+": return left + lastOperand;
            case "-": return left - lastOperand;
            case "*": return left * lastOperand;
            case "/": return (lastOperand == 0) ? 0 : left / lastOperand;
            case "^": return Math.pow(left, lastOperand);
            default: throw new Exception("No operation to repeat");
        }
    }

    private double evaluateExpression(String expr) throws Exception {
        String cleaned = expr.replace(" ", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("xʸ", "^");
        return new ExpressionParser(cleaned).parse();
    }

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
            if (Character.isDigit(c) || c == '.' || c == '(' || c == '√' || c == 'π') {
                return true;
            }
            if (c == 'e' && (pos + 1 >= input.length() || !Character.isLetter(input.charAt(pos + 1)))) {
                return true;
            }
            String remaining = input.substring(pos);
            return remaining.startsWith("sin(") || remaining.startsWith("cos(") ||
                    remaining.startsWith("tan(") || remaining.startsWith("asin(") ||
                    remaining.startsWith("acos(") || remaining.startsWith("atan(") ||
                    remaining.startsWith("log(") || remaining.startsWith("ln(");
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
            if (c == 'a' && input.startsWith("asin(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.toDegrees(Math.asin(v)); }
            if (c == 'a' && input.startsWith("acos(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.toDegrees(Math.acos(v)); }
            if (c == 'a' && input.startsWith("atan(", pos)) { pos += 5; double v = parseExpression(); expect(')'); return Math.toDegrees(Math.atan(v)); }
            if (c == 's' && input.startsWith("sin(", pos)) { pos += 4; double v = parseExpression(); expect(')'); return Math.sin(Math.toRadians(v)); }
            if (c == 'c' && input.startsWith("cos(", pos)) { pos += 4; double v = parseExpression(); expect(')'); return Math.cos(Math.toRadians(v)); }
            if (c == 't' && input.startsWith("tan(", pos)) { pos += 4; double v = parseExpression(); expect(')'); return Math.tan(Math.toRadians(v)); }
            if (c == 'l' && input.startsWith("log(", pos)) { pos += 4; double v = parseExpression(); expect(')'); return Math.log10(v); }
            if (c == 'l' && input.startsWith("ln(", pos)) { pos += 3; double v = parseExpression(); expect(')'); return Math.log(v); }
            if (c == '√' && input.startsWith("√(", pos)) { pos += 2; double v = parseExpression(); expect(')'); return Math.sqrt(v); }
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

        // ====================== KEYBOARD EQUALS IN CURRENCY CONVERTER ======================
        // Press = or Enter in the amount field → instantly triggers conversion
        amountField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_EQUALS) {
                    convertBtn.doClick();
                    e.consume();
                }
            }
        });
        // =================================================================================

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
            } catch (Exception ex) {
                resultLabel.setText("Failed to fetch live rates");
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
            while ((inputLine = in.readLine()) != null) content.append(inputLine);
        }
        conn.disconnect();

        String jsonStr = content.toString();
        Map<String, Double> rates = new HashMap<>();
        int ratesStart = jsonStr.indexOf("\"conversion_rates\":{");
        if (ratesStart == -1) throw new Exception("Invalid API response");
        int openBrace = jsonStr.indexOf('{', ratesStart);
        int closeBrace = jsonStr.indexOf('}', openBrace);
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
