package Project.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import Project.model.UITheme;

public final class ScalingDisplay extends JTextField {
    public ScalingDisplay() {
        setEditable(false);
        setHorizontalAlignment(RIGHT);
        setBackground(new Color(28, 28, 38));
        setForeground(UITheme.TEXT_PRIMARY);
        setCaretColor(UITheme.ACCENT_CYAN);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        setFont(UITheme.FONT_DISPLAY);
        getCaret().setBlinkRate(530);
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        // Shrink font dynamically when expression is long
        int len = (t == null) ? 0 : t.length();
        Font f = UITheme.FONT_DISPLAY;
        if (len > 24) f = f.deriveFont(20f);
        else if (len > 18) f = f.deriveFont(26f);
        else if (len > 12) f = f.deriveFont(34f);
        setFont(f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_PANEL, UITheme.RADIUS_PANEL);
        g2.dispose();
        super.paintComponent(g);
    }
}