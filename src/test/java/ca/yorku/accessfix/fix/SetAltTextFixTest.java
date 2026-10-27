package ca.yorku.accessfix.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.yorku.accessfix.TestSnapshots;
import ca.yorku.accessfix.domain.PageSnapshot;
import org.junit.jupiter.api.Test;

/** WORKED EXAMPLE: the execute and undo round trip every FixCommand must pass. */
class SetAltTextFixTest {

    @Test
    void executeSetsAltAndUndoRemovesIt() {
        PageSnapshot snapshot = TestSnapshots.fromHtml("<img src='cat.png'>");
        String original = snapshot.getHtml();
        String imgId = TestSnapshots.idOf(snapshot, "img");
        SetAltTextFix fix = new SetAltTextFix("image-alt:" + imgId, imgId, "A sleeping cat");

        fix.execute(snapshot);
        assertEquals("A sleeping cat", snapshot.parse().selectFirst("img").attr("alt"));

        fix.undo(snapshot);
        assertEquals(original, snapshot.getHtml(), "undo must restore the exact HTML");
    }

    @Test
    void undoRestoresPreviousValue() {
        PageSnapshot snapshot = TestSnapshots.fromHtml("<img src='cat.png' alt='IMG_2041.jpg'>");
        String imgId = TestSnapshots.idOf(snapshot, "img");
        SetAltTextFix fix = new SetAltTextFix("image-alt:" + imgId, imgId, "A sleeping cat");

        fix.execute(snapshot);
        fix.undo(snapshot);

        assertEquals("IMG_2041.jpg", snapshot.parse().selectFirst("img").attr("alt"));
    }

    @Test
    void failsClearlyWhenElementIsMissing() {
        PageSnapshot snapshot = TestSnapshots.fromHtml("<p>No images</p>");
        SetAltTextFix fix = new SetAltTextFix("image-alt:e99", "e99", "text");

        assertThrows(IllegalStateException.class, () -> fix.execute(snapshot));
    }

    @Test
    void undoBeforeExecuteIsRefused() {
        PageSnapshot snapshot = TestSnapshots.fromHtml("<img src='cat.png'>");
        SetAltTextFix fix = new SetAltTextFix("x", TestSnapshots.idOf(snapshot, "img"), "text");

        assertThrows(IllegalStateException.class, () -> fix.undo(snapshot));
    }
}
