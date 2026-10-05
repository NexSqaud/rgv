package ru.nexsqaud.rgv.core.index;

import ru.nexsqaud.rgv.api.RgvStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses and executes EMI-style tokenized search queries.
 * Supports:
 * - Simple text (display name substring)
 * - @mod (mod id or mod name)
 * - #tooltip (tooltip text)
 * - $tag / *tag (ore dictionary or tag)
 * - /regex/ (regular expression matching)
 * - -term (exclusion)
 */
public class RgvSearchFilter {

    private interface FilterPredicate {
        boolean test(RgvStack stack);
    }

    private final List<FilterPredicate> predicates = new ArrayList<>();
    private final String rawQuery;

    public RgvSearchFilter(String query) {
        this.rawQuery = query != null ? query.trim() : "";
        parse(this.rawQuery);
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public boolean isEmpty() {
        return rawQuery.isEmpty();
    }

    private void parse(String query) {
        if (query.isEmpty()) return;

        String[] tokens = query.split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;

            boolean negated = false;
            if (token.startsWith("-") && token.length() > 1) {
                negated = true;
                token = token.substring(1);
            }

            FilterPredicate predicate;

            if (token.startsWith("@") && token.length() > 1) {
                // Mod ID search: @modid
                final String modTerm = token.substring(1).toLowerCase();
                predicate = stack -> {
                    String id = stack.getId().toLowerCase();
                    int colon = id.indexOf(':');
                    String modId = colon != -1 ? id.substring(0, colon) : "minecraft";
                    return modId.contains(modTerm);
                };
            } else if (token.startsWith("#") && token.length() > 1) {
                // Tooltip search: #tooltip
                final String toolTerm = token.substring(1).toLowerCase();
                predicate = stack -> {
                    for (String line : stack.getTooltip()) {
                        if (line.toLowerCase().contains(toolTerm)) {
                            return true;
                        }
                    }
                    return false;
                };
            } else if ((token.startsWith("$") || token.startsWith("*")) && token.length() > 1) {
                // Tag / OreDictionary search: $tag
                final String tagTerm = token.substring(1).toLowerCase();
                predicate = stack -> {
                    if (stack.getId().toLowerCase().contains(tagTerm)) return true;
                    for (String line : stack.getTooltip()) {
                        if (line.toLowerCase().contains(tagTerm)) return true;
                    }
                    return false;
                };
            } else if (token.startsWith("/") && token.endsWith("/") && token.length() > 2) {
                // Regex search: /regex/
                String patternStr = token.substring(1, token.length() - 1);
                try {
                    final Pattern regex = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                    predicate = stack -> regex.matcher(stack.getDisplayName()).find();
                } catch (Exception e) {
                    predicate = stack -> false;
                }
            } else {
                // Default: display name or ID substring match
                final String term = token.toLowerCase();
                predicate = stack -> stack.getDisplayName().toLowerCase().contains(term)
                                  || stack.getId().toLowerCase().contains(term);
            }

            if (negated) {
                final FilterPredicate base = predicate;
                predicate = stack -> !base.test(stack);
            }

            predicates.add(predicate);
        }
    }

    public boolean test(RgvStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (FilterPredicate predicate : predicates) {
            if (!predicate.test(stack)) {
                return false;
            }
        }
        return true;
    }
}
