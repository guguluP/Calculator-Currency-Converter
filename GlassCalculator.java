//add package file of yours own
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

public class GlassCalculator extends JFrame implements ActionListener {
    private JTextField display;
    private boolean startNewInput = true;
    private final String API_KEY = "97ab7ceab50c9baf51e43393";
    private final String[] currencies = {
            "USD", "EUR", "INR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "RUB",
            "BRL", "ZAR", "MXN", "SGD", "HKD", "SEK", "NOK", "DKK", "KRW", "TRY"
    };
    private List<String> history = new ArrayList<>();

    public GlassCalculator() {
        setTitle("Glass Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(20, 20, 25));

        // ==================== macOS OPTIMIZED ICON LOADING ====================
        setMacIcon();
        // =====================================================================

        // ==================== TOP PANEL - Button moved to top-right corner ====================
        JPanel topPanel = new JPanel(new BorderLayout(0, 0));
        topPanel.setBackground(new Color(20, 20, 25));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 4));   // Reduced right & top padding

        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Segoe UI", Font.PLAIN, 42));
        display.setBackground(new Color(30, 30, 35));
        display.setForeground(Color.WHITE);
        display.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        topPanel.add(display, BorderLayout.CENTER);

        // Mode button (≡) - positioned very close to top-right corner
        JButton modeButton = new JButton("≡");
        modeButton.setFont(new Font("Segoe UI", Font.BOLD, 36));           // Slightly bigger for better visibility
        modeButton.setForeground(new Color(80, 200, 255));
        modeButton.setBackground(new Color(20, 20, 25));
        modeButton.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        modeButton.setFocusPainted(false);
        modeButton.setContentAreaFilled(false);
        modeButton.setOpaque(false);
        modeButton.addActionListener(e -> showModePopup(modeButton));
        topPanel.add(modeButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        // =================================================================================================

        JPanel buttonPanel = new JPanel(new GridLayout(5, 4, 8, 8));
        buttonPanel.setBackground(new Color(20, 20, 25));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] buttons = {
                "AC", "C", "%", "÷",
                "7", "8", "9", "×",
                "4", "5", "6", "−",
                "1", "2", "3", "+",
                "0", ".", "±", "="
        };

        for (String text : buttons) {
            JButton btn = createGlassButton(text);
            btn.addActionListener(this);
            buttonPanel.add(btn);
        }
        add(buttonPanel, BorderLayout.CENTER);

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
// **
    private void setMacIcon() {
        try {
            Image icnsIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/AppIcon.icns"));
            if (icnsIcon != null) {
                setIconImage(icnsIcon);
            } else {
                icnsIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("AppIcon.icns"));
                if (icnsIcon != null) setIconImage(icnsIcon);
            }

            // Try PNG fallback
            if (icnsIcon == null) {
                Image pngIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/AppIcon.png"));
                if (pngIcon != null) setIconImage(pngIcon);
            }

            // macOS Dock icon support
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
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 24));
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
                BorderFactory.createEmptyBorder(8, 4, 8, 4)
        ));

        JMenuItem basicItem = new JMenuItem(" ✓ Basic");
        stylePopupItem(basicItem);
        basicItem.setEnabled(false);
        popup.add(basicItem);

        popup.addSeparator();

        JMenuItem scientificItem = new JMenuItem(" Scientific");
        stylePopupItem(scientificItem);
        scientificItem.setForeground(new Color(160, 160, 170));
        scientificItem.setEnabled(false);
        popup.add(scientificItem);

        JMenuItem programmerItem = new JMenuItem(" Programmer");
        stylePopupItem(programmerItem);
        programmerItem.setForeground(new Color(160, 160, 170));
        programmerItem.setEnabled(false);
        popup.add(programmerItem);

        popup.addSeparator();

        JMenuItem convertItem = new JMenuItem(" Convert");
        stylePopupItem(convertItem);
        convertItem.addActionListener(e -> openCurrencyConverter());
        popup.add(convertItem);

        JMenuItem rpnItem = new JMenuItem(" RPN Mode");
        stylePopupItem(rpnItem);
        rpnItem.setForeground(new Color(160, 160, 170));
        rpnItem.setEnabled(false);
        popup.add(rpnItem);

        popup.addSeparator();

        JMenuItem notesItem = new JMenuItem(" Math Notes...");
        stylePopupItem(notesItem);
        notesItem.setForeground(new Color(160, 160, 170));
        notesItem.setEnabled(false);
        popup.add(notesItem);

        popup.addSeparator();

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
        if (cmd.matches("[0-9.]")) {
            handleNumberOrDecimal(cmd);
        } else if ("AC".equals(cmd)) {
            resetCalculator();
        } else if ("C".equals(cmd)) {
            handleBackspace();
        } else if ("%".equals(cmd)) {
            handlePercentage();
        } else if ("÷×−+".contains(cmd)) {
            handleOperator(cmd);
        } else if ("=".equals(cmd)) {
            handleEquals();
        } else if ("±".equals(cmd)) {
            handleSignChange();
        }
    }

    private void handleSignChange() {
        String current = display.getText().trim();
        if (current.isEmpty() || current.equals("0") || current.equals("Error")) return;
        try {
            if (current.contains(" ")) {
                String lastNumber = getLastNumber(current);
                if (!lastNumber.isEmpty()) {
                    double val = Double.parseDouble(lastNumber);
                    String newLast = formatResult(-val);
                    display.setText(current.substring(0, current.lastIndexOf(lastNumber)) + newLast);
                }
            } else {
                double val = Double.parseDouble(current);
                display.setText(formatResult(-val));
            }
        } catch (Exception ignored) {}
    }

    private void handleNumberOrDecimal(String cmd) {
        String current = display.getText();
        if (startNewInput) {
            display.setText(cmd.equals(".") ? "0." : cmd);
            startNewInput = false;
            return;
        }
        if (cmd.equals(".")) {
            String lastNumber = getLastNumber(current);
            if (lastNumber.contains(".")) return;
        }
        if (current.equals("0") && !cmd.equals(".")) {
            display.setText(cmd);
        } else {
            display.setText(current + cmd);
        }
    }

    private String getLastNumber(String expr) {
        String[] parts = expr.split("[÷×−+]");
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
            case "÷" -> "÷";
            case "×" -> "×";
            case "−" -> "−";
            case "+" -> "+";
            default -> cmd;
        };
        display.setText(current + " " + symbol + " ");
        startNewInput = false;
    }

    private void handleEquals() {
        String expr = display.getText().trim();
        if (expr.isEmpty()) return;
        try {
            double result = evaluateExpression(expr);
            String resultStr = formatResult(result);
            display.setText(resultStr);
            String historyEntry = expr + " = " + resultStr;
            history.add(0, historyEntry);
            if (history.size() > 100) history.remove(history.size() - 1);
        } catch (Exception ex) {
            display.setText("Error");
        }
        startNewInput = true;
    }

    private void openCurrencyConverter() {
        // Your existing currency converter code (unchanged)
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
        int ratesEnd = jsonStr.indexOf("}", ratesStart + 20);
        String ratesSection = jsonStr.substring(ratesStart + 20, ratesEnd);
        String[] pairs = ratesSection.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String currency = keyValue[0].replace("\"", "").trim();
                double rate = Double.parseDouble(keyValue[1].trim());
                rates.put(currency, rate);
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
            for (String entry : history) {
                sb.append(entry).append("\n\n");
            }
            historyArea.setText(sb.toString().trim());
        }

        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        buttonPanel.setBackground(new Color(20, 20, 25));

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

        buttonPanel.add(clearBtn);
        buttonPanel.add(closeBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        clearBtn.addActionListener(ev -> {
            history.clear();
            historyArea.setText("History has been cleared.");
        });
        closeBtn.addActionListener(ev -> dialog.dispose());

        dialog.setVisible(true);
    }

    private boolean endsWithOperator(String text) {
        if (text.isEmpty()) return false;
        char last = text.charAt(text.length() - 1);
        return "÷×−+".indexOf(last) != -1;
    }

    private double evaluateExpression(String expr) {
        String[] parts = expr.split("\\s+");
        if (parts.length == 0) return 0;
        double result = Double.parseDouble(parts[0]);
        for (int i = 1; i + 1 < parts.length; i += 2) {
            String op = parts[i];
            double num = Double.parseDouble(parts[i + 1]);
            result = switch (op) {
                case "+" -> result + num;
                case "−" -> result - num;
                case "×" -> result * num;
                case "÷" -> (num == 0) ? 0 : result / num;
                default -> result;
            };
        }
        return result;
    }

    private String formatResult(double result) {
        if (Double.isNaN(result) || Double.isInfinite(result)) return "Error";
        if (result == (long) result) return String.valueOf((long) result);
        return String.format("%.8f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void resetCalculator() {
        display.setText("0");
        startNewInput = true;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new GlassCalculator().setVisible(true);
        });
    }
}
