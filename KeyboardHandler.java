package Project.ui;

import Project.GlassCalculator;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.text.JTextComponent;

/**
 * Handles keyboard input for the calculator.
 * Provides shortcuts for all calculator operations.
 * 
 * Keyboard shortcuts:
 * - Numbers: 0-9
 * - Operators: +, -, *, /, %
 * - Power: ^ (caret)
 * - Calculate: Enter or =
 * - Backspace: Backspace key
 * - Clear: Delete or Escape
 * - Copy: Ctrl+C / Cmd+C
 * - Paste: Ctrl+V / Cmd+V
 * - Select All: Ctrl+A / Cmd+A
 */
public class KeyboardHandler extends KeyAdapter {
    private final GlassCalculator calculator;

    public KeyboardHandler(GlassCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        JTextComponent display = (JTextComponent) e.getSource();
        int kc = e.getKeyCode();
        boolean ctrl = e.isControlDown() || e.isMetaDown();

        // ═══════════════════════════════════════════════════════════════
        //  CLIPBOARD OPERATIONS
        // ═══════════════════════════════════════════════════════════════
        if (ctrl && kc == KeyEvent.VK_A) {
            display.selectAll();
            e.consume();
            return;
        }
        if (ctrl && kc == KeyEvent.VK_C) {
            display.selectAll();
            display.copy();
            calculator.toast("Copied to clipboard");
            e.consume();
            return;
        }
        if (ctrl && kc == KeyEvent.VK_V) {
            calculator.pasteFromClipboard();
            e.consume();
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        //  CALCULATION OPERATIONS
        // ═══════════════════════════════════════════════════════════════
        if (kc == KeyEvent.VK_ENTER || kc == KeyEvent.VK_EQUALS) {
            calculator.dispatch("=");
            calculator.highlightButton("=");
            e.consume();
            return;
        }
        if (kc == KeyEvent.VK_BACK_SPACE) {
            calculator.dispatch("⌫");
            calculator.highlightButton("⌫");
            e.consume();
            return;
        }
        if (kc == KeyEvent.VK_ESCAPE || kc == KeyEvent.VK_DELETE) {
            calculator.dispatch("AC");
            calculator.highlightButton("AC");
            e.consume();
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        //  NUMBER AND DECIMAL INPUT
        // ═══════════════════════════════════════════════════════════════
        char ch = e.getKeyChar();
        if (Character.isDigit(ch) || ch == '.') {
            calculator.insert(String.valueOf(ch));
            calculator.highlightButton(String.valueOf(ch));
            e.consume();
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        //  OPERATOR KEYS
        // ═══════════════════════════════════════════════════════════════
        switch (ch) {
            case '+':
                calculator.insert("+");
                calculator.highlightButton("+");
                e.consume();
                break;
            case '-':
                calculator.insert("−");
                calculator.highlightButton("−");
                e.consume();
                break;
            case '*':
                calculator.insert("×");
                calculator.highlightButton("×");
                e.consume();
                break;
            case '/':
                calculator.insert("÷");
                calculator.highlightButton("÷");
                e.consume();
                break;
            case '^':
                calculator.insert("^");
                calculator.highlightButton("xʸ");
                e.consume();
                break;
            case '%':
                calculator.dispatch("%");
                calculator.highlightButton("%");
                e.consume();
                break;
            case '(':
                calculator.insert("(");
                calculator.highlightButton("(");
                e.consume();
                break;
            case ')':
                calculator.insert(")");
                calculator.highlightButton(")");
                e.consume();
                break;
        }
    }
}