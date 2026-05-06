package Project.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import Project.model.UITheme;

public final class GlassButton extends JButton {
    private float rippleAlpha = 0f;
    private final Timer rippleTimer;

    public GlassButton(String text) {
        super(text);
        setFont(text.length() > 3 ? UITheme.FONT_BTN_SM : UITheme.FONT_BTN_LG);
        setForeground(UITheme.TEXT_PRIMARY);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(16, 10, 16, 10));

        rippleTimer = new Timer(20, null);
        rippleTimer.addActionListener(e -> {
            rippleAlpha = Math.max(rippleAlpha - 0.06f, 0f);
            repaint();
            if (rippleAlpha <= 0f) rippleTimer.stop();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                triggerRipple();
            }
        });
    }

    /**
     * Expose the ripple effect so the keyboard shortcuts can trigger it visually
     */
    public void triggerRipple() {
        rippleAlpha = 0.55f;
        rippleTimer.restart();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight(), r = UITheme.RADIUS_BTN;
        Shape shape = new RoundRectangle2D.Float(0, 0, w, h, r, r);

        // Body fill
        Color base = getModel().isPressed() ? UITheme.GLASS_PRESS
                : getModel().isRollover() ? UITheme.GLASS_HOVER
                  : UITheme.GLASS_FILL;
        g2.setColor(base);
        g2.fill(shape);

        // Top-edge inner highlight (glass sheen)
        GradientPaint sheen = new GradientPaint(
                0, 0, new Color(255, 255, 255, 60),
                0, h * 0.45f, new Color(255, 255, 255, 0));
        g2.setPaint(sheen);
        g2.fill(shape);

        // Border
        g2.setColor(UITheme.GLASS_BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, r - 1, r - 1));

        // Ripple overlay
        if (rippleAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, rippleAlpha));
            g2.setColor(Color.WHITE);
            g2.fill(shape);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}