// ═════════════════════════════════════════════════════════════════════
// FIXED ButtonFactory.java
// ═════════════════════════════════════════════════════════════════════

package Project.ui;

import Project.model.UITheme;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;

/**
 * Factory for creating calculator buttons with consistent styling.
 * Applies appropriate colors, fonts, and tooltips based on button function.
 */
public class ButtonFactory {
    
    public static GlassButton createButton(String text) {
        GlassButton btn = new GlassButton(text);

        // Color-code buttons by function
        if ("÷×−+=".contains(text)) {
            btn.setForeground(UITheme.ACCENT_AMBER);
        } else if ("AC C ⌫".contains(text)) {
            btn.setForeground(UITheme.TEXT_DIM());
        } else if (isSciToken(text)) {
            btn.setForeground(UITheme.ACCENT_CYAN);
        }

        // Add descriptive tooltips
        String tooltip = getTooltip(text);
        if (tooltip != null) btn.setToolTipText(tooltip);

        return btn;
    }

    private static String getTooltip(String t) {
        return switch (t) {
            // ─── Basic Functions ───
            case "AC" -> "Clear All (Esc / Del)";
            case "C", "⌫" -> "Backspace / Delete";
            case "%" -> "Percent (%)";
            case "±", "+/-" -> "Toggle Sign";
            case "=" -> "Calculate (Enter / =)";
            case "÷" -> "Divide (/)";
            case "×" -> "Multiply (*)";
            case "−" -> "Subtract (-)";
            case "+" -> "Add (+)";
            case "." -> "Decimal Point";
            case "(" -> "Left Parenthesis";
            case ")" -> "Right Parenthesis";
            
            // ─── Memory Functions ───
            case "mc" -> "Clear Memory";
            case "m+" -> "Add to Memory";
            case "m-" -> "Subtract from Memory";
            case "mr" -> "Recall Memory";
            
            // ─── Scientific Functions ───
            case "x²" -> "Square (x²)";
            case "x³" -> "Cube (x³)";
            case "xʸ", "yˣ" -> "Power (x^y, use ^)";
            case "2ˣ" -> "2 to the power of x";
            case "²√x" -> "Square Root";
            case "³√x" -> "Cube Root";
            case "ʸ√x" -> "Y-th Root";
            case "1/x" -> "Reciprocal (1/x)";
            case "log₁₀" -> "Logarithm Base 10";
            case "log₂" -> "Logarithm Base 2";
            case "ln" -> "Natural Logarithm";
            case "x!" -> "Factorial (!)";
            case "sin" -> "Sine";
            case "sin⁻¹" -> "Arcsine (Inverse Sine)";
            case "cos" -> "Cosine";
            case "cos⁻¹" -> "Arccosine (Inverse Cosine)";
            case "tan" -> "Tangent";
            case "tan⁻¹" -> "Arctangent (Inverse Tangent)";
            case "sinh" -> "Hyperbolic Sine";
            case "sinh⁻¹" -> "Inverse Hyperbolic Sine";
            case "cosh" -> "Hyperbolic Cosine";
            case "cosh⁻¹" -> "Inverse Hyperbolic Cosine";
            case "tanh" -> "Hyperbolic Tangent";
            case "tanh⁻¹" -> "Inverse Hyperbolic Tangent";
            case "π" -> "Pi (3.141592...)";
            case "e" -> "Euler's Number (2.71828...)";
            case "EE" -> "Scientific Notation (Enter Exponent)";
            case "Rad" -> "Toggle Radians / Degrees";
            case "2nd" -> "Enable Inverse Functions";
            case "Rand" -> "Random Number (0-1)";
            default -> null;
        };
    }

    private static boolean isSciToken(String t) {
        return switch (t) {
            case "2nd", "x²", "x³", "xʸ", "yˣ", "2ˣ", "1/x",
                 "²√x", "³√x", "ʸ√x", "log₁₀", "log₂", "ln",
                 "x!", "sin", "sin⁻¹", "cos", "cos⁻¹", "tan", "tan⁻¹",
                 "sinh", "sinh⁻¹", "cosh", "cosh⁻¹", "tanh", "tanh⁻¹",
                 "e", "EE", "π", "Rad", "(", ")",
                 "mc", "m+", "m-", "mr", "Rand" -> true;
            default -> false;
        };
    }
}