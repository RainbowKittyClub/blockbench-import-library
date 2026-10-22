package de.tomalbrc.bil.file.extra;

import de.tomalbrc.bil.BIL;
import de.tomalbrc.bil.file.bbmodel.BbPlaceholderButton;
import gg.moonflower.molangcompiler.api.MolangEnvironmentBuilder;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Blockbench's animation variable placeholders, bound the way Blockbench binds them: each one is a
 * variable (or query) whose value is a Molang expression, never text pasted into a keyframe.
 */
public class BbVariablePlaceholders {
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    // slider('id', …), toggle('id') or impulse('id', …): the control's type and id
    private static final Pattern CONTROL = Pattern.compile("(slider|toggle|impulse)\\(\\s*['\"]?([^'\",)\\s]*)['\"]?.*\\)");
    // Bare words Molang itself owns; a placeholder by one of these names can't be told apart.
    private static final Set<String> RESERVED = Set.of("math", "query", "q", "variable", "v", "temp", "t",
            "context", "c", "this", "return", "loop", "for_each", "break", "continue", "true", "false");

    private final Map<String, String> variables = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, String> queries = new Object2ObjectLinkedOpenHashMap<>();
    private final Pattern bareNames;
    private Map<String, MolangExpression> compiledVariables;
    private Map<String, MolangExpression> compiledQueries;

    public BbVariablePlaceholders(String data) {
        StringBuilder bare = new StringBuilder();
        for (String line : data.split("\n")) {
            int split = line.indexOf('=');
            if (split < 0) continue;

            String key = line.substring(0, split).replaceAll("[\\s;]", "");
            String value = valueOf(line.substring(split + 1));
            if (key.isEmpty() || value.isEmpty()) continue;

            if (key.startsWith("v.") || key.startsWith("variable.")) {
                putNamed(this.variables, key.substring(key.indexOf('.') + 1), value);
            } else if (key.startsWith("q.") || key.startsWith("query.")) {
                putNamed(this.queries, key.substring(key.indexOf('.') + 1), value);
            } else if (NAME.matcher(key).matches() && !RESERVED.contains(key)) {
                this.variables.put(key, value);
                bare.append(bare.isEmpty() ? "" : "|").append(Pattern.quote(key));
            }
        }
        this.bareNames = bare.isEmpty() ? null : Pattern.compile("(?<![\\w.])(?:" + bare + ")(?!\\w)");
    }

    /**
     * Rewrites each bare placeholder name in {@code expression} to the variable it is bound to.
     */
    public String rewrite(String expression) {
        if (this.bareNames != null) {
            expression = this.bareNames.matcher(expression).replaceAll(m -> "v." + Matcher.quoteReplacement(m.group()));
        }
        return expression.replace('\n', ' ').trim();
    }

    /**
     * Binds every placeholder in {@code builder}. Values stay expressions, so one may read a query
     * or another placeholder. A slider, toggle or impulse reads the value its saved control holds.
     */
    public void bind(MolangEnvironmentBuilder<?> builder, @Nullable List<BbPlaceholderButton> buttons) {
        if (this.compiledVariables == null) {
            this.compiledVariables = this.compile(this.variables, buttons);
            this.compiledQueries = this.compile(this.queries, buttons);
        }
        this.compiledVariables.forEach(builder::setVariable);
        this.compiledQueries.forEach(builder::setQuery);
    }

    private Map<String, MolangExpression> compile(Map<String, String> sources, @Nullable List<BbPlaceholderButton> buttons) {
        return sources.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            String value = controlValue(entry.getValue(), buttons);
            try {
                return BIL.COMPILER.compile(MolangCompat.negatedGroups(this.rewrite(value)));
            } catch (MolangSyntaxException e) {
                throw new IllegalArgumentException("Bad value for placeholder " + entry.getKey(), e);
            }
        }, (a, b) -> b, Object2ObjectLinkedOpenHashMap::new));
    }

    private static String controlValue(String value, @Nullable List<BbPlaceholderButton> buttons) {
        if (value.startsWith("'")) return "0";

        Matcher control = CONTROL.matcher(value);
        if (!control.matches()) return value;

        if (buttons != null) for (BbPlaceholderButton button : buttons) {
            if (control.group(1).equals(button.type) && control.group(2).equals(button.id))
                return Float.toString(button.value);
        }
        return "0";
    }

    private static void putNamed(Map<String, String> target, String name, String value) {
        if (NAME.matcher(name).matches()) target.put(name, value);
    }

    private static String valueOf(String raw) {
        String value = raw.trim();
        return value.endsWith(";") ? value.substring(0, value.length() - 1).trim() : value;
    }
}
