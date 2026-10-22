package de.tomalbrc.bil.file.extra;

/**
 * Rewrites Molang that Blockbench evaluates but molang-compiler can't parse.
 */
public final class MolangCompat {
    private static final String UNARY_BEFORE = "+-*/%<>=!&|?:,(;";

    private MolangCompat() {
    }

    /**
     * Rewrites each negated bracket {@code -( … )} as {@code (0-( … ))}, since molang-compiler can
     * negate a number or a name but not a bracket. A minus that subtracts is left alone.
     */
    public static String negatedGroups(String expression) {
        if (!expression.contains("(")) return expression;

        StringBuilder out = new StringBuilder(expression.length() + 8);
        append(expression, 0, expression.length(), out);
        return out.toString();
    }

    private static void append(String expression, int from, int to, StringBuilder out) {
        int i = from;
        while (i < to) {
            char ch = expression.charAt(i);
            if (ch == '-' && isUnary(expression, i)) {
                int open = skipSpaces(expression, i + 1, to);
                int close = open < to && expression.charAt(open) == '(' ? closing(expression, open, to) : -1;
                if (close >= 0) {
                    out.append("(0-(");
                    append(expression, open + 1, close, out);
                    out.append("))");
                    i = close + 1;
                    continue;
                }
            }
            out.append(ch);
            i++;
        }
    }

    private static boolean isUnary(String expression, int minus) {
        int i = minus - 1;
        while (i >= 0 && Character.isWhitespace(expression.charAt(i))) i--;
        if (i < 0) return true;
        if (UNARY_BEFORE.indexOf(expression.charAt(i)) >= 0) return true;

        int end = i + 1;
        while (i >= 0 && (Character.isLetterOrDigit(expression.charAt(i)) || expression.charAt(i) == '_')) i--;
        return expression.substring(i + 1, end).equals("return") && (i < 0 || expression.charAt(i) != '.');
    }

    private static int skipSpaces(String expression, int from, int to) {
        while (from < to && Character.isWhitespace(expression.charAt(from))) from++;
        return from;
    }

    private static int closing(String expression, int open, int to) {
        int depth = 0;
        for (int i = open; i < to; i++) {
            char ch = expression.charAt(i);
            if (ch == '(') depth++;
            else if (ch == ')' && --depth == 0) return i;
        }
        return -1;
    }
}
