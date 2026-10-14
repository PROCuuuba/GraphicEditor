package org.example.graphiceditor.ui;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("gui")
class FillFinalAcceptanceTest extends VectorGuiTestSupport {
    @Test
    void removingAndRestoringFillRepaintsTheInteriorWithoutChangingBorderOrShadow() throws Exception {
        withEditor(editor -> {
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.RED);
            setColor(editor, "border-color", Color.DARKGREEN);
            setSpinner(editor, "border-width", 6);
            enable(editor, "shadow-enabled", true);
            setColor(editor, "shadow-color", Color.MAGENTA);
            setSpinner(editor, "shadow-x", 100);
            setSpinner(editor, "shadow-y", 0);
            setSpinner(editor, "shadow-blur", 0);
            finishSquare(editor, 220, 220, 80);
            Polygon geometry = VectorStyle.geometryOf(editor.model().getShapes().getFirst());
            PixelReader filled = image(editor);
            assertColor(filled, 260, 260, Color.RED);
            assertColor(filled, 260, 220, Color.DARKGREEN);
            assertColor(filled, 360, 260, Color.MAGENTA);

            enable(editor, "fill-enabled", false);

            VectorStyle unfilled = VectorStyle.from(editor.model().getShapes().getFirst());
            assertFalse(unfilled.fillEnabled());
            assertOtherEffects(unfilled);
            assertSame(geometry, VectorStyle.geometryOf(editor.model().getShapes().getFirst()));
            PixelReader empty = image(editor);
            assertColor(empty, 260, 260, Color.WHITE);
            assertColor(empty, 260, 220, Color.DARKGREEN);
            assertColor(empty, 360, 260, Color.MAGENTA);

            setColor(editor, "fill-color", Color.BLUE);
            enable(editor, "fill-enabled", true);

            VectorStyle refilled = VectorStyle.from(editor.model().getShapes().getFirst());
            assertTrue(refilled.fillEnabled());
            assertEquals(Color.BLUE, refilled.fillColor());
            assertOtherEffects(refilled);
            assertSame(geometry, VectorStyle.geometryOf(editor.model().getShapes().getFirst()));
            assertEquals(1, editor.model().getShapes().size());
            PixelReader changed = image(editor);
            assertColor(changed, 260, 260, Color.BLUE);
            assertColor(changed, 260, 220, Color.DARKGREEN);
            assertColor(changed, 360, 260, Color.MAGENTA);
        });
    }

    private static void assertOtherEffects(VectorStyle style) {
        assertTrue(style.borderEnabled());
        assertEquals(Color.DARKGREEN, style.borderColor());
        assertEquals(6, style.borderWidth());
        assertTrue(style.shadowEnabled());
        assertEquals(Color.MAGENTA, style.shadowColor());
        assertEquals(100, style.shadowOffsetX());
        assertEquals(0, style.shadowOffsetY());
        assertEquals(0, style.shadowBlur());
    }
}
