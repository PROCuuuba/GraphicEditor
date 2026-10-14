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
class BorderFinalAcceptanceTest extends VectorGuiTestSupport {
    @Test
    void borderWidthExtremesAndRemovalRepaintWithoutChangingFillOrShadow() throws Exception {
        withEditor(editor -> {
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.RED);
            setColor(editor, "border-color", Color.BLACK);
            setSpinner(editor, "border-width", 0.5);
            enable(editor, "shadow-enabled", true);
            setColor(editor, "shadow-color", Color.MAGENTA);
            setSpinner(editor, "shadow-x", 100);
            setSpinner(editor, "shadow-y", 0);
            setSpinner(editor, "shadow-blur", 0);
            finishSquare(editor, 220, 220, 80);
            Polygon geometry = VectorStyle.geometryOf(editor.model().getShapes().getFirst());
            assertEquals(0.5, VectorStyle.from(editor.model().getShapes().getFirst()).borderWidth());
            PixelReader thin = image(editor);
            assertColor(thin, 260, 211, Color.WHITE);
            assertFalse(Color.WHITE.equals(thin.getColor(260, 219))
                    && Color.RED.equals(thin.getColor(260, 220)), "Тонкий контур должен быть виден");
            assertColor(thin, 260, 260, Color.RED);
            assertColor(thin, 360, 260, Color.MAGENTA);

            setSpinner(editor, "border-width", 20);

            VectorStyle thickStyle = VectorStyle.from(editor.model().getShapes().getFirst());
            assertTrue(thickStyle.borderEnabled());
            assertEquals(Color.BLACK, thickStyle.borderColor());
            assertEquals(20, thickStyle.borderWidth());
            assertOtherEffects(thickStyle);
            assertSame(geometry, VectorStyle.geometryOf(editor.model().getShapes().getFirst()));
            PixelReader thick = image(editor);
            assertColor(thick, 260, 211, Color.BLACK);
            assertColor(thick, 260, 220, Color.BLACK);
            assertColor(thick, 260, 260, Color.RED);
            assertColor(thick, 360, 260, Color.MAGENTA);

            enable(editor, "border-enabled", false);

            VectorStyle borderless = VectorStyle.from(editor.model().getShapes().getFirst());
            assertFalse(borderless.borderEnabled());
            assertOtherEffects(borderless);
            assertSame(geometry, VectorStyle.geometryOf(editor.model().getShapes().getFirst()));
            assertEquals(1, editor.model().getShapes().size());
            PixelReader removed = image(editor);
            assertColor(removed, 260, 211, Color.WHITE);
            assertColor(removed, 260, 220, Color.RED);
            assertColor(removed, 260, 260, Color.RED);
            assertColor(removed, 360, 260, Color.MAGENTA);
        });
    }

    private static void assertOtherEffects(VectorStyle style) {
        assertTrue(style.fillEnabled());
        assertEquals(Color.RED, style.fillColor());
        assertTrue(style.shadowEnabled());
        assertEquals(Color.MAGENTA, style.shadowColor());
        assertEquals(100, style.shadowOffsetX());
        assertEquals(0, style.shadowOffsetY());
        assertEquals(0, style.shadowBlur());
    }
}
