package ca.yorku.accessfix.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The F08 mapping is a requirement, so it gets tests even though it is small. */
class FixTypeTest {

    @Test
    void ariaLabelFixesFieldsLinksAndButtons() {
        assertTrue(FixType.SET_ARIA_LABEL.canFix(RuleIds.FORM_LABEL));
        assertTrue(FixType.SET_ARIA_LABEL.canFix(RuleIds.LINK_NAME));
        assertTrue(FixType.SET_ARIA_LABEL.canFix(RuleIds.BUTTON_NAME));
        assertFalse(FixType.SET_ARIA_LABEL.canFix(RuleIds.IMAGE_ALT));
    }

    @Test
    void formFieldsAllowTwoFixTypes() {
        assertEquals(Set.of(FixType.ADD_LABEL, FixType.SET_ARIA_LABEL), FixType.allowedFor(RuleIds.FORM_LABEL));
    }

    @Test
    void unknownRuleHasNoFixTypes() {
        assertTrue(FixType.allowedFor("made-up-rule").isEmpty());
    }

    @Test
    void parsesModelNamesSafely() {
        assertEquals(Optional.of(FixType.SET_ALT_TEXT), FixType.fromToolName("set_alt_text"));
        assertEquals(Optional.empty(), FixType.fromToolName("delete_page"));
        assertEquals(Optional.empty(), FixType.fromToolName(null));
    }
}
