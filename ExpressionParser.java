package Project.model;

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

    private void skipWS() {
        while (pos < expr.length() && Character.isWhitespace(expr.charAt(pos))) pos++;
    }

    public double parse() throws ArithmeticException {
        skipWS();
        double v = addSub();
        skipWS();
        if (pos < expr.length()) throw new ArithmeticException(
                "Unexpected token at " + pos + ": '" + expr.charAt(pos) + "'");
        return v;
    }

    // ── Grammar ──────────────────────────────────────────────────
    // addSub   = mulDiv (('+' | '-') mulDiv)*
    // mulDiv   = unary  (('*' | '/') unary  | implicit-mul)*
    // unary    = '-' unary | '+' unary | power          ← sits ABOVE power so
    // power    = primary ('^' unary)?                      -2^2 = -(2^2) = -4
    // primary  = number | '(' addSub ')' | function '(' addSub ')' | const
    // ─────────────────────────────────────────────────────────────

    private double addSub() throws ArithmeticException {
        skipWS();
        double v = mulDiv();
        skipWS();
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '+') {
                pos++;
                skipWS();
                v += mulDiv();
                skipWS();
            } else if (c == '-') {
                pos++;
                skipWS();
                v -= mulDiv();
                skipWS();
            } else break;
        }
        return v;
    }

    private double mulDiv() throws ArithmeticException {
        skipWS();
        // FIX 1: call unary() (not power()) so that the chain is
        //         mulDiv → unary → power → primary
        //         This makes unary-minus bind looser than '^'.
        double v = unary();
        skipWS();
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '*') {
                pos++;
                skipWS();
                v *= unary();   // FIX 1 (cont.): was power()
                skipWS();
            } else if (c == '/') {
                pos++;
                skipWS();
                double d = unary();   // FIX 1 (cont.): was power()
                if (d == 0) throw new ArithmeticException("Division by zero");
                v /= d;
                skipWS();
            } else if (startsNewPrimary()) {   // implicit multiplication
                v *= unary();   // FIX 1 (cont.): was power()
                skipWS();
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
                s.startsWith("π") ||   // FIX 2: was missing — broke implicit mul like 2π
                s.startsWith("√(") ||
                (c == 'e' && (pos + 1 >= expr.length() || !Character.isLetter(expr.charAt(pos + 1))));
    }

    // FIX 1 (cont.): power() now calls primary() directly (not unary()).
    //   The exponent side still goes through unary() so that e.g. 2^-3 works.
    private double power() throws ArithmeticException {
        skipWS();
        double base = primary();   // was unary() — caused -2^2 == 4 instead of -4
        skipWS();
        if (pos < expr.length() && expr.charAt(pos) == '^') {
            pos++;
            skipWS();
            return Math.pow(base, unary()); // right-associative; exponent via unary() allows 2^-3
        }
        return base;
    }

    private double unary() throws ArithmeticException {
        skipWS();
        if (pos < expr.length() && expr.charAt(pos) == '-') {
            pos++;
            skipWS();
            return -unary();
        }
        if (pos < expr.length() && expr.charAt(pos) == '+') {
            pos++;
            skipWS();
            return unary();
        }
        return power();   // FIX 1 (cont.): was primary()
    }

    private double primary() throws ArithmeticException {
        skipWS();
        if (pos >= expr.length()) throw new ArithmeticException("Unexpected end of expression");
        char c = expr.charAt(pos);

        // Number literal (including E-notation)
        if (Character.isDigit(c) || c == '.') return parseNumber();

        // Parenthesised group
        if (c == '(') {
            pos++;
            skipWS();
            double v = addSub();
            skipWS();
            expect(')');
            return v;
        }

        // Named functions / constants
        String s = expr.substring(pos);

        if (s.startsWith("asin(")) return fn1(5, v -> {
            double r = Math.asin(v);
            return radians ? r : Math.toDegrees(r);
        });
        if (s.startsWith("acos(")) return fn1(5, v -> {
            double r = Math.acos(v);
            return radians ? r : Math.toDegrees(r);
        });
        if (s.startsWith("atan(")) return fn1(5, v -> {
            double r = Math.atan(v);
            return radians ? r : Math.toDegrees(r);
        });
        if (s.startsWith("sin(")) return fn1(4, v -> Math.sin(radians ? v : Math.toRadians(v)));
        if (s.startsWith("cos(")) return fn1(4, v -> Math.cos(radians ? v : Math.toRadians(v)));
        if (s.startsWith("tan(")) return fn1(4, v -> Math.tan(radians ? v : Math.toRadians(v)));
        if (s.startsWith("asinh(")) return fn1(6, v -> Math.log(v + Math.sqrt(v * v + 1)));
        if (s.startsWith("acosh(")) return fn1(6, v -> Math.log(v + Math.sqrt(v * v - 1)));
        if (s.startsWith("atanh(")) return fn1(6, v -> 0.5 * Math.log((1 + v) / (1 - v)));
        if (s.startsWith("sinh(")) return fn1(5, Math::sinh);
        if (s.startsWith("cosh(")) return fn1(5, Math::cosh);
        if (s.startsWith("tanh(")) return fn1(5, Math::tanh);
        if (s.startsWith("log2(")) return fn1(5, v -> Math.log(v) / Math.log(2));
        if (s.startsWith("log(")) return fn1(4, Math::log10);
        if (s.startsWith("ln(")) return fn1(3, Math::log);
        if (s.startsWith("sqrt(")) return fn1(5, Math::sqrt);
        if (s.startsWith("cbrt(")) return fn1(5, Math::cbrt);
        if (s.startsWith("√(")) {
            pos += 2;
            skipWS();
            double v = addSub();
            skipWS();
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

    // FIX 3: Removed unused 'tag' parameter (was accepted but never read).
    // FIX 4 & 5: Replaced custom DoubleUnary interface (and the unused
    //             'import java.util.function.*') with a local SAM. The standard
    //             DoubleUnaryOperator can't declare 'throws ArithmeticException',
    //             so we keep a minimal private interface — but we no longer
    //             maintain a dead import alongside it.
    @FunctionalInterface
    private interface DoubleUnary {
        double apply(double v) throws ArithmeticException;
    }

    private double fn1(int len, DoubleUnary fn) throws ArithmeticException {
        pos += len;
        skipWS();
        double v = addSub();
        skipWS();
        expect(')');
        return fn.apply(v);
    }

    private double parseNumber() {
        int start = pos;
        while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) pos++;
        // E-notation: e.g. 1.5e+10 or 1.5E-10
        if (pos < expr.length() && Character.toLowerCase(expr.charAt(pos)) == 'e') {
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

    private void expect(char ch) throws ArithmeticException {
        skipWS();
        if (pos >= expr.length() || expr.charAt(pos) != ch) {
            throw new ArithmeticException("Expected '" + ch + "' at position " + pos);
        }
        pos++;
    }
}