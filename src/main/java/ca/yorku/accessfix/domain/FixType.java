package ca.yorku.accessfix.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The five fix types and the rules each one can fix (F08).
 *
 * <p>Stage 1 says this mapping lives in one place and that plan validation, ProposeFixTool and
 * FixService all use it. Don't copy this table anywhere else; call {@link #canFix(String)}.
 */
public enum FixType {
    SET_ALT_TEXT(RuleIds.IMAGE_ALT),
    ADD_LABEL(RuleIds.FORM_LABEL),
    SET_ARIA_LABEL(RuleIds.FORM_LABEL, RuleIds.LINK_NAME, RuleIds.BUTTON_NAME),
    SET_LANGUAGE(RuleIds.PAGE_LANG),
    SET_TITLE(RuleIds.PAGE_TITLE);

    private final Set<String> fixableRules;

    FixType(String... ruleIds) {
        this.fixableRules = Set.of(ruleIds);
    }

    public Set<String> fixableRules() {
        return fixableRules;
    }

    /** True if this fix type is allowed for an issue of the given rule. */
    public boolean canFix(String ruleId) {
        return fixableRules.contains(ruleId);
    }

    /** All fix types allowed for a rule. Empty means the issue can only be fixed manually. */
    public static Set<FixType> allowedFor(String ruleId) {
        return Arrays.stream(values())
                .filter(type -> type.canFix(ruleId))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FixType.class)));
    }

    /** The name the model sees in tool schemas, for example "set_alt_text". */
    public String toolName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a name from the model. Returns empty for unknown names instead of throwing. */
    public static Optional<FixType> fromToolName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(type -> type.toolName().equals(name.trim())).findFirst();
    }
}
