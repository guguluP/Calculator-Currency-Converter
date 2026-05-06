package Project.model;

import java.util.function.*;

public final class ExpressionParser {
    private final String expr;
    private final boolean radians;
    private int pos;

    public ExpressionParser(String expression, boolean radians) {
        // Normalise operators to ASCII
        this.expr = expression.trim()
                .replace('×', '*').replace('÷', '/').replace('−', '-');
        this.radians = radians;
        this.pos = 0;
    }

    public double parse() throws ArithmeticException {
        double v = addSub();
        if (pos < expr.length()) throw new ArithmeticException(
                "Unexpected token at " + pos + ": '" + expr.charAt(pos) + "'");
        return v;
    }

    // ── Grammar ──────────────────────────────────────────────────
    // addSub   = mulDiv (('+' | '-') mulDiv)*
    // mulDiv   = power  (('*' | '/') power  | implicit-mul)*
    // power    = unary  ('^' power)?
    // unary    = '-' unary | primary
    // primary  = number | '(' addSub ')' | function '(' addSub ')' | const
    // ─────────────────────────────────────────────────────────────

    private double addSub() throws ArithmeticException {
        double v = mulDiv();
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '+') {
                pos++;
                v += mulDiv();
            } else if (c == '-') {
                pos++;
                v -= mulDiv();
            } else break;
        }
        return v;
    }

    private double mulDiv() throws ArithmeticException {
        double v = power();
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '*') {
                pos++;
                v *= power();
            } else if (c == '/') {
                pos++;
                double d = power();
                if (d == 0) throw new ArithmeticException("Division by zero");
                v /= d;
            } else if (startsNewPrimary()) {   // implicit multiplication
                v *= power();
            } else break;
        }
        return v;
    }

    private boolean startsNewPrimary() {
        if (pos >= expr.length()) return false;
        char c = expr.charAt(pos);
        if (Character.isDigit(c) || c == '.' || c == '(') return true;
        String s = expr.substring(pos);
        return s.startsWith("sin(") || s.startsWith("cos(") || s.startsWith("tan(") ||
                s.startsWith("asin(") || s.startsWith("acos(") || s.startsWith("atan(") ||
                s.startsWith("sinh(") || s.startsWith("cosh(") || s.startsWith("tanh(") ||
                s.startsWith("asinh(") || s.startsWith("acosh(") || s.startsWith("atanh(") ||
                s.startsWith("log2(") || s.startsWith("log(") || s.startsWith("ln(") ||
                s.startsWith("sqrt(") || s.startsWith("cbrt(") || s.startsWith("pi") ||
                s.startsWith("√(") ||
                (c == 'e' && (pos + 1 >= expr.length() || !Character.isLetter(expr.charAt(pos + 1))));
    }

    private double power() throws ArithmeticException {
        double base = unary();
        if (pos < expr.length() && expr.charAt(pos) == '^') {
            pos++;
            return Math.pow(base, power()); // right-associative
        }
        return base;
    }

    private double unary() throws ArithmeticException {
        if (pos < expr.length() && expr.charAt(pos) == '-') {
            pos++;
            return -unary();
        }
        if (pos < expr.length() && expr.charAt(pos) == '+') {
            pos++;
            return unary();
        }
        return primary();
    }

    private double primary() throws ArithmeticException {
        if (pos >= expr.length()) throw new ArithmeticException("Unexpected end of expression");
        char c = expr.charAt(pos);

        // Number literal (including E-notation)
        if (Character.isDigit(c) || c == '.') return parseNumber();

        // Parenthesised group
        if (c == '(') {
            pos++;
            double v = addSub();
            if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
            return v;
        }

        // Named functions / constants
        String s = expr.substring(pos);

        if (s.startsWith("asin(")) return fn1("asin(", 5, v -> {
            double r = Math.asin(v);
            return radians ? r : Math.toDegrees(r);
        });
        if (s.startsWith("acos(")) return fn1("acos(", 5, v -> {
            double r = Math.acos(v);
            return radians ? r : Math.toDegrees(r);
        });
        if (s.startsWith("atan(")) return fn1("atan(", 5, v -> {
            double r = Math.atan(v);
            return radians ? r : Math.toDegrees(r);
        });
        if (s.startsWith("sin(")) return fn1("sin(", 4, v -> Math.sin(radians ? v : Math.toRadians(v)));
        if (s.startsWith("cos(")) return fn1("cos(", 4, v -> Math.cos(radians ? v : Math.toRadians(v)));
        if (s.startsWith("tan(")) return fn1("tan(", 4, v -> Math.tan(radians ? v : Math.toRadians(v)));
        if (s.startsWith("asinh(")) return fn1("asinh(", 6, v -> Math.log(v + Math.sqrt(v * v + 1)));
        if (s.startsWith("acosh(")) return fn1("acosh(", 6, v -> Math.log(v + Math.sqrt(v * v - 1)));
        if (s.startsWith("atanh(")) return fn1("atanh(", 6, v -> 0.5 * Math.log((1 + v) / (1 - v)));
        if (s.startsWith("sinh(")) return fn1("sinh(", 5, Math::sinh);
        if (s.startsWith("cosh(")) return fn1("cosh(", 5, Math::cosh);
        if (s.startsWith("tanh(")) return fn1("tanh(", 5, Math::tanh);
        if (s.startsWith("log2(")) return fn1("log2(", 5, v -> Math.log(v) / Math.log(2));
        if (s.startsWith("log(")) return fn1("log(", 4, Math::log10);
        if (s.startsWith("ln(")) return fn1("ln(", 3, Math::log);
        if (s.startsWith("sqrt(")) return fn1("sqrt(", 5, Math::sqrt);
        if (s.startsWith("cbrt(")) return fn1("cbrt(", 5, Math::cbrt);
        if (s.startsWith("√(")) {
            pos += 2;
            double v = addSub();
            expect(')');
            return Math.sqrt(v);
        }
        if (s.startsWith("pi")) {
            pos += 2;
            return Math.PI;
        }
        if (s.startsWith("π")) {
            pos += 1;
            return Math.PI;
        }
        if (s.startsWith("e") && (pos + 1 >= expr.length() || !Character.isLetter(expr.charAt(pos + 1)))) {
            pos++;
            return Math.E;
        }

        throw new ArithmeticException("Unknown token: '" + s.charAt(0) + "' at pos " + pos);
    }

    @FunctionalInterface
    interface DoubleUnary {
        double apply(double v) throws ArithmeticException;
    }

    private double fn1(String tag, int len, DoubleUnary fn) throws ArithmeticException {
        pos += len;
        double v = addSub();
        expect(')');
        return fn.apply(v);
    }

    private double parseNumber() {
        int start = pos;
        while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) pos++;
        // E-notation: e.g. 1.5E+10
        if (pos < expr.length() && (expr.charAt(pos) == 'E')) {
            pos++;
            if (pos < expr.length() && (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) pos++;
            while (pos < expr.length() && Character.isDigit(expr.charAt(pos))) pos++;
        }
        try {
            return Double.parseDouble(expr.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new ArithmeticException("Bad number at " + start);
        }
    }

    private void expect(char ch) {
        if (pos < expr.length() && expr.charAt(pos) == ch) pos++;
    }
}