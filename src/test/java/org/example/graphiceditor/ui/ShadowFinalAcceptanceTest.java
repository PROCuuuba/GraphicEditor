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
class ShadowFinalAcceptanceTest extends VectorGuiTestSupport {
    @Test
    void movingShadowToNegativeOffsetsAndDisablingItClearsOldPixelsButPreservesThePolygon() throws Exception {
        withEditor(editor -> {
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.RED);
            setColor(editor, "border-color", Color.BLUE);
            setSpinner(editor, "border-width", 4);
            enable(editor, "shadow-enabled", true);
            setColor(editor, "shadow-color", Color.MAGENTA);
            setSpinner(editor, "shadow-x", 80);
            setSpinner(editor, "shadow-y", 80);
            setSpinner(editor, "shadow-blur", 0);
            finishSquare(editor, 220, 220, 100);
            Polygon geometry = VectorStyle.geometryOf(editor.model().getShapes().getFirst());
            PixelReader initial = image(editor);
            assertColor(initial, 360, 360, Color.MAGENTA);
            assertColor(initial, 175, 185, Color.WHITE);
            assertColor(initial, 270, 270, Color.RED);
            assertColor(initial, 270, 220, Color.BLUE);

            setSpinner(editor, "shadow-x", -80);
            setSpinner(editor, "shadow-y", -70);

            VectorStyle shiftedStyle = VectorStyle.from(editor.model().getShapes().getFirst());
            assertTrue(shiftedStyle.shadowEnabled());
            assertEquals(Color.MAGENTA, shiftedStyle.shadowColor());
            assertEquals(-80, shiftedStyle.shadowOffsetX());
            assertEquals(-70, shiftedStyle.shadowOffsetY());
            assertEquals(0, shiftedStyle.shadowBlur());
            assertOtherEffects(shiftedStyle);
            assertSame(geometry, VectorStyle.geometryOf(editor.model().getShapes().getFirst()));
            PixelReader shifted = image(editor);
            assertColor(shifted, 360, 360, Color.WHITE);
            assertColor(shifted, 175, 185, Color.MAGENTA);
            assertColor(shifted, 270, 270, Color.RED);
            assertColor(shifted, 270, 220, Color.BLUE);

            enable(editor, "shadow-enabled", false);

            VectorStyle shadowless = VectorStyle.from(editor.model().getShapes().getFirst());
            assertFalse(shadowless.shadowEnabled());
            assertOtherEffects(shadowless);
            assertSame(geometry, VectorStyle.geometryOf(editor.model().getShapes().getFirst()));
            assertEquals(1, editor.model().getShapes().size());
            PixelReader removed = image(editor);
            assertColor(removed, 360, 360, Color.WHITE);
            assertColor(removed, 175, 185, Color.WHITE);
            assertColor(removed, 270, 270, Color.RED);
            assertColor(removed, 270, 220, Color.BLUE);
        });
    }

    private static void assertOtherEffects(VectorStyle style) {
        assertTrue(style.fillEnabled());
        assertEquals(Color.RED, style.fillColor());
        assertTrue(style.borderEnabled());
        assertEquals(Color.BLUE, style.borderColor());
        assertEquals(4, style.borderWidth());
    }
}
