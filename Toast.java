package Project.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import Project.model.UITheme;
import Project.GlassCalculator;

public final class Toast extends JWindow {
    private final GlassCalculator calculator;
    private float alpha = 0f;
    private final Timer fadeIn, fadeOut;

    public Toast(GlassCalculator calculator, String message) {
        super(calculator);
        this.calculator = calculator;
        JLabel lbl = new JLabel(message, SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setBorder(new EmptyBorder(10, 22, 10, 22));
        setLayout(new BorderLayout());

        JPanel bg = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(UITheme.GLASS_BORDER);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bg.setOpaque(false);
        bg.add(lbl);
        add(bg);
        pack();
        setBackground(new Color(0, 0, 0, 0));

        fadeIn = new Timer(25, null);
        fadeOut = new Timer(25, null);
        fadeIn.addActionListener(e -> {
            alpha = Math.min(alpha + 0.1f, 1f);
            repaint();
            if (alpha >= 1f) {
                fadeIn.stop();
            }
        });
        fadeOut.addActionListener(e -> {
            alpha = Math.max(alpha - 0.08f, 0f);
            repaint();
            if (alpha <= 0f) {
                fadeOut.stop();
                dispose();
            }
        });
    }

    public void show(int displayY) {
        int x = calculator.getX() + (calculator.getWidth() - getWidth()) / 2;
        int y = displayY + calculator.getY() - getHeight() - 8;
        setLocation(x, y);
        setVisible(true);
        fadeIn.start();
        new Timer(2000, e -> {
            fadeIn.stop();
            fadeOut.start();
        }).start();
    }
}