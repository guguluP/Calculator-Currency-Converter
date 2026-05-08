package Project.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import Project.model.UITheme;
import Project.config.AppConfig;

/**
 * Setup Wizard dialog for first-time configuration of database and API credentials.
 * Appears on first run to guide users through secure credential setup.
 *
 * @author GlassCalculator Team
 * @version 1.1 - Fixed theme initialization and UI improvements
 */
public class SetupWizard extends JDialog {
    private int currentStep = 0;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // Step definitions
    private static final int STEP_WELCOME = 0;
    private static final int STEP_DATABASE = 1;
    private static final int STEP_API = 2;
    private static final int STEP_SUMMARY = 3;
    private static final int TOTAL_STEPS = 4;

    // Database configuration fields
    private JComboBox<String> dbTypeCombo;
    private JTextField dbHostField;
    private JTextField dbPortField;
    private JTextField dbUserField;
    private JPasswordField dbPasswordField;
    private JTextField dbNameField;

    // API configuration fields
    private JPasswordField apiKeyField;
    private JCheckBox skipApiSetup;

    // Summary fields
    private JTextArea summaryArea;

    public SetupWizard(JFrame parent) {
        super(parent, "GlassCalculator - First Time Setup", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(650, 550);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        // Initialize theme before creating components
        UITheme.initializeSystemTheme();
        
        getContentPane().setBackground(UITheme.BG_DEEP());
        setLayout(new BorderLayout());

        // Create card layout for multi-step wizard
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(UITheme.BG_DEEP());

        // Add steps
        cardPanel.add(createWelcomeStep(), "welcome");
        cardPanel.add(createDatabaseStep(), "database");
        cardPanel.add(createApiStep(), "api");
        cardPanel.add(createSummaryStep(), "summary");

        add(cardPanel, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        // Allow closing with confirmation
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(SetupWizard.this,
                    "Setup is not complete. Exit anyway?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
    }

    /**
     * Creates the welcome step UI.
     */
    private JPanel createWelcomeStep() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("Welcome to GlassCalculator");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(UITheme.ACCENT_CYAN);

        JTextArea messageArea = new JTextArea(
            "This is your first time running GlassCalculator.\n\n" +
            "This wizard will help you configure:\n" +
            "• Database connection settings\n" +
            "• Optional API credentials\n\n" +
            "Let's get started!"
        );
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messageArea.setForeground(UITheme.TEXT_PRIMARY());
        messageArea.setBackground(UITheme.BG_DEEP());
        messageArea.setBorder(new EmptyBorder(20, 0, 20, 0));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(messageArea, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the database configuration step.
     */
    private JPanel createDatabaseStep() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("Database Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(UITheme.ACCENT_CYAN);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 12, 16));
        formPanel.setBackground(UITheme.BG_DEEP());
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        dbTypeCombo = createComboBox(new String[]{"MySQL", "PostgreSQL", "SQLite"});
        dbTypeCombo.setSelectedItem("SQLite");
        dbHostField = createTextField("localhost");
        dbPortField = createTextField("3306");
        dbUserField = createTextField("root");
        dbPasswordField = new JPasswordField();
        dbPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dbPasswordField.setBackground(UITheme.BG_ELEVATED());
        dbPasswordField.setForeground(UITheme.TEXT_PRIMARY());
        dbNameField = createTextField("glasscalc");

        formPanel.add(createLabel("Database Type:"));
        formPanel.add(dbTypeCombo);
        formPanel.add(createLabel("Host:"));
        formPanel.add(dbHostField);
        formPanel.add(createLabel("Port:"));
        formPanel.add(dbPortField);
        formPanel.add(createLabel("Username:"));
        formPanel.add(dbUserField);
        formPanel.add(createLabel("Password:"));
        formPanel.add(dbPasswordField);
        formPanel.add(createLabel("Database Name:"));
        formPanel.add(dbNameField);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the API configuration step.
     */
    private JPanel createApiStep() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("API Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(UITheme.ACCENT_CYAN);

        JPanel formPanel = new JPanel(new GridLayout(3, 1, 12, 16));
        formPanel.setBackground(UITheme.BG_DEEP());
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        skipApiSetup = new JCheckBox("Skip API setup (not recommended)");
        skipApiSetup.setBackground(UITheme.BG_DEEP());
        skipApiSetup.setForeground(UITheme.TEXT_PRIMARY());
        skipApiSetup.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel apiLabel = createLabel("Exchange Rate API Key:");
        apiKeyField = new JPasswordField();
        apiKeyField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        apiKeyField.setBackground(UITheme.BG_ELEVATED());
        apiKeyField.setForeground(UITheme.TEXT_PRIMARY());

        skipApiSetup.addActionListener(e -> {
            apiKeyField.setEnabled(!skipApiSetup.isSelected());
        });

        formPanel.add(skipApiSetup);
        formPanel.add(apiLabel);
        formPanel.add(apiKeyField);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the summary/confirmation step.
     */
    private JPanel createSummaryStep() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DEEP());
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("Setup Summary");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(UITheme.ACCENT_CYAN);

        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        summaryArea.setBackground(UITheme.BG_SURFACE());
        summaryArea.setForeground(UITheme.TEXT_PRIMARY());
        summaryArea.setBorder(new EmptyBorder(16, 16, 16, 16));

        JScrollPane scrollPane = new JScrollPane(summaryArea);
        scrollPane.setBorder(null);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the footer panel with navigation buttons.
     */
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setBackground(UITheme.BG_SURFACE());

        JButton backBtn = createButton("Back");
        JButton nextBtn = createButton("Next");
        JButton finishBtn = createButton("Finish");

        backBtn.addActionListener(e -> previousStep());
        nextBtn.addActionListener(e -> nextStep());
        finishBtn.addActionListener(e -> completeSetup());

        panel.add(backBtn);
        panel.add(nextBtn);
        panel.add(finishBtn);

        updateButtonStates();
        return panel;
    }

    private void nextStep() {
        if (currentStep < TOTAL_STEPS - 1) {
            currentStep++;
            cardLayout.show(cardPanel, String.valueOf(currentStep));
            updateButtonStates();
        }
    }

    private void previousStep() {
        if (currentStep > 0) {
            currentStep--;
            cardLayout.show(cardPanel, String.valueOf(currentStep));
            updateButtonStates();
        }
    }

    private void completeSetup() {
        try {
            // Save database configuration
            String dbType = (String) dbTypeCombo.getSelectedItem();
            String host = dbHostField.getText().trim();
            int port = Integer.parseInt(dbPortField.getText().trim());
            String username = dbUserField.getText().trim();
            String password = new String(dbPasswordField.getPassword());
            String dbName = dbNameField.getText().trim();

            AppConfig.setDatabaseConfig(dbType, host, port, username, password, dbName);

            // Save API key if provided
            if (apiKeyField.getPassword().length > 0) {
                String apiKey = new String(apiKeyField.getPassword()).trim();
                AppConfig.setApiKey(apiKey);
            }

            AppConfig.markSetupComplete();
            dispose();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateButtonStates() {
        // Implementation depends on access to buttons
    }

    // ─────────────────────────────────────────────────────────────────
    //  UI HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(UITheme.TEXT_PRIMARY());
        return label;
    }

    private JTextField createTextField(String initial) {
        JTextField field = new JTextField(initial);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(UITheme.BG_ELEVATED());
        field.setForeground(UITheme.TEXT_PRIMARY());
        field.setBorder(new EmptyBorder(8, 10, 8, 10));
        return field;
    }

    private JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(UITheme.BG_ELEVATED());
        combo.setForeground(UITheme.TEXT_PRIMARY());
        return combo;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(UITheme.ACCENT_CYAN);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        return btn;
    }
}