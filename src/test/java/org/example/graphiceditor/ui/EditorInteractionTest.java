package org.example.graphiceditor.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Tool;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("gui")
class EditorInteractionTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX должен запуститься за 10 секунд");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void zoomChangesOnlyDisplayAndUpdatesScrollableBounds() throws Exception {
        withEditor(editor -> {
            CanvasViewport viewport = editor.viewport();
            viewport.setZoom(1);
            assertEquals(1, viewport.getZoom());
            editor.button("zoom-in").fire();
            assertTrue(viewport.getZoom() > 1);
            editor.button("zoom-out").fire();
            editor.button("zoom-reset").fire();
            assertEquals(1, viewport.getZoom());
            assertEquals("100%", editor.node("zoom-label", Label.class).getText());

            viewport.setZoom(2);
            editor.pane().applyCss();
            editor.pane().layout();
            assertEquals(900, editor.canvas().getWidth());
            assertEquals(650, editor.canvas().getHeight());
            assertEquals(900, editor.model().getWidth());
            assertEquals(650, editor.model().getHeight());
            Point2D origin = editor.canvas().localToScene(0, 0);
            Point2D right = editor.canvas().localToScene(900, 0);
            assertEquals(1800, origin.distance(right), 0.001);
            ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
            assertTrue(scroll.getContent().getLayoutBounds().getWidth() >= 1800);
            assertTrue(scroll.getContent().getLayoutBounds().getHeight() >= 1300);

            viewport.setZoom(4);
            assertEquals(4, viewport.getZoom());
            viewport.zoomIn();
            assertEquals(4, viewport.getZoom());
            viewport.setZoom(0.25);
            viewport.zoomOut();
            assertEquals(0.25, viewport.getZoom());
            assertTrue(editor.model().getPixels().isEmpty());
            assertTrue(editor.model().getShapes().isEmpty());
        });
    }

    @Test
    void fitMakesTheWholeLogicalCanvasVisibleWithoutChangingItsDimensions() throws Exception {
        withEditor(editor -> {
            editor.viewport().setZoom(4);
            editor.button("zoom-fit").fire();
            editor.pane().layout();
            ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
            double zoom = editor.viewport().getZoom();
            assertTrue(zoom >= 0.25 && zoom <= 4);
            assertTrue(900 * zoom <= scroll.getViewportBounds().getWidth() + 1);
            assertTrue(650 * zoom <= scroll.getViewportBounds().getHeight() + 1);
            assertEquals(900, editor.canvas().getWidth());
            assertEquals(650, editor.canvas().getHeight());
        });
    }

    @Test
    void automaticFitGrowsWithTheWindowAndKeepsTheCanvasCenteredAndVisible() throws Exception {
        withEditor(1000, 720, editor -> {
            click(editor.canvas(), 180, 140);
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Shape original = editor.model().getShapes().getFirst();
            double smallerZoom = editor.viewport().getZoom();
            assertCanvasCenteredAndVisible(editor);

            editor.resize(1900, 1200);

            assertTrue(editor.viewport().getZoom() > smallerZoom);
            assertCanvasCenteredAndVisible(editor);
            assertEquals(900, editor.canvas().getWidth());
            assertEquals(650, editor.canvas().getHeight());
            assertEquals(900, editor.model().getWidth());
            assertEquals(650, editor.model().getHeight());
            assertEquals(180, editor.model().getPixels().getFirst().getX(), 1e-9);
            assertEquals(140, editor.model().getPixels().getFirst().getY(), 1e-9);
            assertSame(original, editor.model().getShapes().getFirst());

            editor.resize(1000, 720);
            assertEquals(smallerZoom, editor.viewport().getZoom(), 0.02);
            assertCanvasCenteredAndVisible(editor);
        });
    }

    @Test
    void manualZoomSurvivesWindowResizeAndFitReactivatesAutomaticSizing() throws Exception {
        withEditor(1000, 720, editor -> {
            editor.viewport().setZoom(1.7);
            editor.resize(1900, 1200);
            assertEquals(1.7, editor.viewport().getZoom());
            editor.resize(1100, 760);
            assertEquals(1.7, editor.viewport().getZoom());

            editor.button("zoom-fit").fire();
            double smallerZoom = editor.viewport().getZoom();
            assertCanvasCenteredAndVisible(editor);
            editor.resize(1900, 1200);
            assertTrue(editor.viewport().getZoom() > smallerZoom);
            assertCanvasCenteredAndVisible(editor);

            shortcut(editor.pane(), KeyCode.DIGIT0, false, true);
            assertEquals(1, editor.viewport().getZoom());
            editor.resize(1000, 720);
            assertEquals(1, editor.viewport().getZoom());
        });
    }

    @Test
    void zoomPreservesTheLogicalPointAtViewportCenterAndUnderTheWheelCursor() throws Exception {
        withEditor(850, 620, editor -> {
            editor.viewport().setZoom(1);
            ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
            Node visibleArea = scroll.lookup(".viewport");
            assertNotNull(visibleArea);
            scroll.setHvalue(0.35);
            scroll.setVvalue(0.4);
            editor.pane().layout();
            Point2D center = new Point2D(scroll.getViewportBounds().getWidth() / 2,
                    scroll.getViewportBounds().getHeight() / 2);
            Point2D logicalCenter = editor.canvas().sceneToLocal(visibleArea.localToScene(center));
            assertTrue(logicalCenter.getX() > 0 && logicalCenter.getX() < 900);
            assertTrue(logicalCenter.getY() > 0 && logicalCenter.getY() < 650);

            editor.viewport().setZoom(2);
            Point2D afterCenterZoom = editor.canvas().sceneToLocal(visibleArea.localToScene(center));
            assertEquals(logicalCenter.getX(), afterCenterZoom.getX(), 1);
            assertEquals(logicalCenter.getY(), afterCenterZoom.getY(), 1);

            editor.viewport().resetZoom();
            scroll.setHvalue(0.35);
            scroll.setVvalue(0.4);
            editor.pane().layout();
            Point2D cursor = new Point2D(scroll.getViewportBounds().getWidth() * 0.37,
                    scroll.getViewportBounds().getHeight() * 0.61);
            Point2D scenePoint = visibleArea.localToScene(cursor);
            Point2D logicalCursor = editor.canvas().sceneToLocal(scenePoint);
            Point2D screenPoint = editor.canvas().localToScreen(logicalCursor);
            assertNotNull(screenPoint);
            Event.fireEvent(editor.canvas(), new ScrollEvent(ScrollEvent.SCROLL,
                    scenePoint.getX(), scenePoint.getY(), screenPoint.getX(), screenPoint.getY(),
                    false, true, false, false, false, false,
                    0, 40, 0, 40, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0,
                    new PickResult(editor.canvas(),
                            new Point3D(logicalCursor.getX(), logicalCursor.getY(), 0), 0)));

            assertTrue(editor.viewport().getZoom() > 1);
            Point2D afterWheelZoom = editor.canvas().sceneToLocal(visibleArea.localToScene(cursor));
            assertEquals(logicalCursor.getX(), afterWheelZoom.getX(), 1);
            assertEquals(logicalCursor.getY(), afterWheelZoom.getY(), 1);
        });
    }

    @Test
    void unmodifiedMouseWheelZoomsSmoothlyAroundTheCursorAndHonorsLimits() throws Exception {
        withEditor(850, 620, editor -> {
            editor.viewport().setZoom(1.6);
            ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
            scroll.setHvalue(0.35);
            scroll.setVvalue(0.4);
            editor.pane().layout();
            Point2D anchor = viewportAnchor(editor, 0.37, 0.61);
            Point2D logical = logicalAt(editor, anchor);

            wheel(editor, anchor, 40, false, false, false);

            double coarseZoom = editor.viewport().getZoom();
            assertTrue(coarseZoom > 1.6);
            assertPointEquals(logical, logicalAt(editor, anchor));
            wheel(editor, anchor, -40, false, false, false);
            assertTrue(editor.viewport().getZoom() < coarseZoom);
            assertEquals(1.6, editor.viewport().getZoom(), 0.0001);
            assertPointEquals(logical, logicalAt(editor, anchor));

            wheel(editor, anchor, 1, false, false, false);
            assertTrue(editor.viewport().getZoom() > 1.6);
            assertTrue(editor.viewport().getZoom() < coarseZoom);
            assertPointEquals(logical, logicalAt(editor, anchor));

            editor.viewport().setZoom(4);
            wheel(editor, anchor, 40, false, false, false);
            assertEquals(4, editor.viewport().getZoom());
            editor.viewport().setZoom(0.25);
            wheel(editor, anchor, -40, false, false, false);
            assertEquals(0.25, editor.viewport().getZoom());
        });
    }

    @Test
    void controlAndCommandWheelZoomAlsoWorkWhileDirectTouchScrollStaysScroll() throws Exception {
        withEditor(850, 620, editor -> {
            for (boolean command : List.of(false, true)) {
                editor.viewport().setZoom(1.6);
                Point2D anchor = viewportAnchor(editor, 0.42, 0.46);
                Point2D logical = logicalAt(editor, anchor);
                wheel(editor, anchor, 12, !command, command, false);
                double increased = editor.viewport().getZoom();
                assertTrue(increased > 1.6);
                assertPointEquals(logical, logicalAt(editor, anchor));
                wheel(editor, anchor, -12, !command, command, false);
                assertTrue(editor.viewport().getZoom() < increased);
                assertEquals(1.6, editor.viewport().getZoom(), 0.0001);
                assertPointEquals(logical, logicalAt(editor, anchor));
            }

            Point2D anchor = viewportAnchor(editor, 0.5, 0.5);
            wheel(editor, anchor, 40, false, false, true);
            assertEquals(1.6, editor.viewport().getZoom());
            wheel(editor, anchor, 40, true, false, true);
            assertTrue(editor.viewport().getZoom() > 1.6);
        });
    }

    @Test
    void pinchZoomUsesTheGestureFactorAndPreservesThePointUnderTheFingers() throws Exception {
        withEditor(850, 620, editor -> {
            editor.viewport().setZoom(1.6);
            ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
            scroll.setHvalue(0.3);
            scroll.setVvalue(0.4);
            editor.pane().layout();
            Point2D anchor = viewportAnchor(editor, 0.38, 0.57);
            Point2D logical = logicalAt(editor, anchor);

            pinch(editor, anchor, 1.4);
            assertEquals(2.24, editor.viewport().getZoom(), 0.0001);
            assertPointEquals(logical, logicalAt(editor, anchor));
            pinch(editor, anchor, 1 / 1.4);
            assertEquals(1.6, editor.viewport().getZoom(), 0.0001);
            assertPointEquals(logical, logicalAt(editor, anchor));

            for (double invalidFactor : List.of(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0)) {
                pinch(editor, anchor, invalidFactor);
                assertEquals(1.6, editor.viewport().getZoom(), 0.0001);
            }

            pinch(editor, anchor, 100);
            assertEquals(4, editor.viewport().getZoom());
            pinch(editor, anchor, 0.001);
            assertEquals(0.25, editor.viewport().getZoom());
        });
    }

    @Test
    void rasterAndPolygonMouseCoordinatesStayLogicalAtEveryZoom() throws Exception {
        withEditor(editor -> {
            for (double zoom : List.of(0.5, 2.0, 4.0)) {
                editor.button("clear-canvas").fire();
                editor.toggle("mode-raster").fire();
                editor.viewport().setZoom(zoom);
                editor.pane().layout();
                click(editor.canvas(), 180.5, 140.5);
                assertEquals(180.5, editor.model().getPixels().getFirst().getX(), 1e-9);
                assertEquals(140.5, editor.model().getPixels().getFirst().getY(), 1e-9);

                editor.toggle("mode-polygon").fire();
                triangle(editor);
                editor.button("polygon-finish").fire();
                assertVerticesEquals(List.of(new Vertex(100, 100), new Vertex(220, 100),
                        new Vertex(220, 220)), editor.model().getShapes().getFirst().getVertices());
            }
        });
    }

    @Test
    void clickingTheFirstVertexWithinTenScreenPixelsClosesWithoutADuplicate() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            for (double zoom : List.of(0.5, 2.0, 4.0)) {
                editor.button("clear-canvas").fire();
                editor.viewport().setZoom(zoom);
                editor.pane().layout();
                triangle(editor);
                click(editor.canvas(), 100 + 9 / zoom, 100);

                assertEquals(1, editor.model().getShapes().size());
                assertVerticesEquals(List.of(new Vertex(100, 100), new Vertex(220, 100),
                        new Vertex(220, 220)), editor.model().getShapes().getFirst().getVertices());
                assertTrue(editor.button("polygon-finish").isDisabled());
            }
        });
    }

    @Test
    void closingNeedsThreeVerticesAndPointsOutsideTheScreenRadiusRemainSeparate() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            editor.viewport().setZoom(2);
            click(editor.canvas(), 100, 100);
            click(editor.canvas(), 220, 100);
            click(editor.canvas(), 102, 100);
            assertTrue(editor.model().getShapes().isEmpty());
            assertTrue(editor.button("polygon-finish").isDisabled());

            click(editor.canvas(), 220, 220);
            click(editor.canvas(), 106, 100);
            assertTrue(editor.model().getShapes().isEmpty());
            editor.button("polygon-finish").fire();
            assertVerticesEquals(List.of(new Vertex(100, 100), new Vertex(220, 100),
                    new Vertex(220, 220), new Vertex(106, 100)),
                    editor.model().getShapes().getFirst().getVertices());
        });
    }

    @Test
    void aNewPolygonSnapsToExactVerticesOfAnExistingPolygon() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Vertex originalVertex = editor.model().getShapes().getFirst().getVertices().getFirst();
            editor.viewport().setZoom(2);
            click(editor.canvas(), 103, 101);
            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 350, 220);
            editor.button("polygon-finish").fire();

            assertEquals(2, editor.model().getShapes().size());
            assertEquals(originalVertex,
                    editor.model().getShapes().getLast().getVertices().getFirst());
            assertEquals(3, editor.model().getShapes().getLast().getVertices().size());
        });
    }

    @Test
    void keyboardSwitchesToolsAndModesAndFinishesOrCancelsPolygonDrafts() throws Exception {
        withEditor(editor -> {
            key(editor.pane(), KeyCode.DIGIT2);
            assertEquals(Tool.CIRCLE, editor.controller().getTool());
            key(editor.pane(), KeyCode.DIGIT3);
            assertEquals(Tool.SQUARE, editor.controller().getTool());
            key(editor.pane(), KeyCode.DIGIT1);
            assertEquals(Tool.POINT, editor.controller().getTool());
            key(editor.pane(), KeyCode.P);
            assertTrue(editor.toggle("mode-polygon").isSelected());
            triangle(editor);
            key(editor.pane(), KeyCode.ENTER);
            assertEquals(1, editor.model().getShapes().size());

            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 450, 100);
            key(editor.pane(), KeyCode.ESCAPE);
            assertTrue(editor.button("polygon-cancel").isDisabled());
            assertEquals(1, editor.model().getShapes().size());
            key(editor.pane(), KeyCode.B);
            assertTrue(editor.toggle("mode-raster").isSelected());
        });
    }

    @Test
    void keyboardDeletesOnlyTheSelectionAndClearRemovesAllContent() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Shape original = editor.model().getShapes().getFirst();
            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 450, 100);
            click(editor.canvas(), 450, 220);
            editor.button("polygon-finish").fire();
            key(editor.pane(), KeyCode.DELETE);
            assertEquals(1, editor.model().getShapes().size());
            assertSame(original, editor.model().getShapes().getFirst());

            key(editor.pane(), KeyCode.B);
            click(editor.canvas(), 500, 300);
            key(editor.pane(), KeyCode.P);
            click(editor.canvas(), 600, 100);
            shortcut(editor.pane(), KeyCode.DELETE, true, false);
            assertTrue(editor.model().getShapes().isEmpty());
            assertTrue(editor.model().getPixels().isEmpty());
            assertTrue(editor.button("polygon-cancel").isDisabled());
        });
    }

    @Test
    void controlAndCommandZoomShortcutsWorkAndTypingFieldsKeepTheirOwnKeys() throws Exception {
        withEditor(editor -> {
            editor.viewport().setZoom(1);
            shortcut(editor.pane(), KeyCode.EQUALS, false, false);
            assertTrue(editor.viewport().getZoom() > 1);
            shortcut(editor.pane(), KeyCode.DIGIT0, false, false);
            assertEquals(1, editor.viewport().getZoom());
            shortcut(editor.pane(), KeyCode.MINUS, false, true);
            assertTrue(editor.viewport().getZoom() < 1);
            shortcut(editor.pane(), KeyCode.DIGIT0, false, true);
            assertEquals(1, editor.viewport().getZoom());
            shortcut(editor.pane(), KeyCode.DIGIT9, false, false);
            assertTrue(editor.viewport().getZoom() > 0);

            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 450, 100);
            click(editor.canvas(), 450, 220);
            Spinner<?> spinner = editor.node("border-width", Spinner.class);
            TextField field = spinner.getEditor();
            field.requestFocus();
            assertSame(spinner, editor.pane().getScene().getFocusOwner());
            for (KeyCode code : List.of(KeyCode.B, KeyCode.P, KeyCode.DIGIT2,
                    KeyCode.ENTER, KeyCode.DELETE, KeyCode.BACK_SPACE)) {
                key(field, code);
            }
            assertTrue(editor.toggle("mode-polygon").isSelected());
            assertEquals(Tool.POINT, editor.controller().getTool());
            assertEquals(1, editor.model().getShapes().size());
            assertFalse(editor.button("polygon-finish").isDisabled());

            TextField actualField = new TextField("Текст");
            editor.pane().setTop(actualField);
            editor.pane().applyCss();
            editor.pane().layout();
            actualField.requestFocus();
            assertSame(actualField, editor.pane().getScene().getFocusOwner());
            for (KeyCode code : List.of(KeyCode.B, KeyCode.P, KeyCode.DIGIT2,
                    KeyCode.ENTER, KeyCode.DELETE, KeyCode.BACK_SPACE)) {
                key(actualField, code);
            }
            assertTrue(editor.toggle("mode-polygon").isSelected());
            assertEquals(Tool.POINT, editor.controller().getTool());
            assertEquals(1, editor.model().getShapes().size());
            assertFalse(editor.button("polygon-finish").isDisabled());
        });
    }

    @Test
    void draggingASelectedVertexAtDoubleZoomPreservesStyleAndOtherVertices() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            editor.node("fill-enabled", CheckBox.class).fire();
            editor.node("fill-color", ColorPicker.class).setValue(Color.GOLD);
            editor.node("border-color", ColorPicker.class).setValue(Color.PURPLE);
            editor.node("shadow-enabled", CheckBox.class).fire();
            editor.node("shadow-color", ColorPicker.class).setValue(Color.GRAY);
            editor.button("polygon-apply").fire();
            Shape original = editor.model().getShapes().getFirst();
            VectorStyle originalStyle = VectorStyle.from(original);
            editor.viewport().setZoom(2);

            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 103, 101, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 153, 141, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 153, 141, false);

            assertEquals(1, editor.model().getShapes().size());
            Shape moved = editor.model().getShapes().getFirst();
            assertVerticesEquals(List.of(new Vertex(150, 140), new Vertex(220, 100),
                    new Vertex(220, 220)), moved.getVertices());
            assertEquals(original.getVertices().subList(1, 3), moved.getVertices().subList(1, 3));
            assertEquals(originalStyle, VectorStyle.from(moved));
            assertTrue(editor.button("polygon-finish").isDisabled());
            assertTrue(editor.model().getPixels().isEmpty());
        });
    }

    @Test
    void draggingAnUnselectedPolygonVertexAtHalfZoomSelectsOnlyThatPolygon() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Shape original = editor.model().getShapes().getFirst();
            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 450, 100);
            click(editor.canvas(), 450, 220);
            editor.button("polygon-finish").fire();
            Shape untouched = editor.model().getShapes().getLast();
            ListView<?> list = editor.node("polygon-list", ListView.class);
            assertEquals(1, list.getSelectionModel().getSelectedIndex());
            editor.viewport().setZoom(0.5);

            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 100, 100, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 140, 150, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 140, 150, false);

            assertEquals(2, editor.model().getShapes().size());
            assertVerticesEquals(List.of(new Vertex(140, 150), new Vertex(220, 100),
                    new Vertex(220, 220)), editor.model().getShapes().getFirst().getVertices());
            assertEquals(original.getVertices().subList(1, 3),
                    editor.model().getShapes().getFirst().getVertices().subList(1, 3));
            assertSame(untouched, editor.model().getShapes().getLast());
            assertEquals(0, list.getSelectionModel().getSelectedIndex());
            assertTrue(editor.button("polygon-finish").isDisabled());
        });
    }

    @Test
    void draggingOutsideTheCanvasRetainsTheLastValidVertexPosition() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Shape original = editor.model().getShapes().getFirst();

            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 100, 100, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 130, 150, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 1010, 150, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 1010, 150, false);

            assertVerticesEquals(List.of(new Vertex(130, 150), new Vertex(220, 100),
                    new Vertex(220, 220)), editor.model().getShapes().getFirst().getVertices());
            assertEquals(original.getVertices().subList(1, 3),
                    editor.model().getShapes().getFirst().getVertices().subList(1, 3));
            assertEquals(1, editor.model().getShapes().size());
            assertTrue(editor.button("polygon-finish").isDisabled());
        });
    }

    @Test
    void escapeDuringVertexDraggingRestoresTheOriginalShapeBeforeMouseRelease() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Shape original = editor.model().getShapes().getFirst();

            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 100, 100, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 150, 160, true);
            assertVertexEquals(new Vertex(150, 160),
                    editor.model().getShapes().getFirst().getVertices().getFirst());
            key(editor.pane(), KeyCode.ESCAPE);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 150, 160, false);

            assertSame(original, editor.model().getShapes().getFirst());
            assertEquals(1, editor.model().getShapes().size());
            assertTrue(editor.button("polygon-finish").isDisabled());
            assertTrue(editor.button("polygon-cancel").isDisabled());
        });
    }

    @Test
    void clickingAnExistingVertexWithoutDraggingStartsASnappedNewPolygon() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            triangle(editor);
            editor.button("polygon-finish").fire();
            Shape original = editor.model().getShapes().getFirst();
            editor.viewport().setZoom(2);

            click(editor.canvas(), 103, 101);
            assertSame(original, editor.model().getShapes().getFirst());
            assertFalse(editor.button("polygon-cancel").isDisabled());
            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 350, 220);
            editor.button("polygon-finish").fire();

            assertEquals(2, editor.model().getShapes().size());
            assertSame(original, editor.model().getShapes().getFirst());
            assertVerticesEquals(List.of(new Vertex(100, 100), new Vertex(350, 100),
                    new Vertex(350, 220)), editor.model().getShapes().getLast().getVertices());
            assertEquals(original.getVertices().getFirst(),
                    editor.model().getShapes().getLast().getVertices().getFirst());
        });
    }

    @Test
    void deleteOrApplyWhileAVertexIsPressedDoesNotCreateADraftOnMouseRelease() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            for (boolean applyStyle : List.of(false, true)) {
                editor.button("clear-canvas").fire();
                triangle(editor);
                editor.button("polygon-finish").fire();
                mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 100, 100, true);
                if (applyStyle) shortcut(editor.pane(), KeyCode.ENTER, false, false);
                else key(editor.pane(), KeyCode.DELETE);
                mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 100, 100, false);

                assertEquals(applyStyle ? 1 : 0, editor.model().getShapes().size());
                assertTrue(editor.button("polygon-finish").isDisabled());
                assertTrue(editor.button("polygon-cancel").isDisabled());
                assertTrue(editor.model().getPixels().isEmpty());
            }
        });
    }

    @Test
    void zoomDuringMouseGesturesKeepsTheLastPositionAndEndsDrawingUntilTheNextPress() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            for (boolean useWheel : List.of(true, false)) {
                editor.button("clear-canvas").fire();
                triangle(editor);
                editor.button("polygon-finish").fire();
                editor.viewport().setZoom(1.6);
                mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 100, 100, true);
                mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 140, 150, true);
                List<Vertex> beforeZoom = editor.model().getShapes().getFirst().getVertices();
                assertVertexEquals(new Vertex(140, 150), beforeZoom.getFirst());

                if (useWheel) {
                    wheel(editor, viewportAnchor(editor, 0.5, 0.5), 40, false, false, false);
                } else {
                    shortcut(editor.pane(), KeyCode.EQUALS, false, true);
                }
                assertTrue(editor.viewport().getZoom() > 1.6);
                mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 230, 250, true);
                mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 230, 250, false);

                assertEquals(1, editor.model().getShapes().size());
                assertEquals(beforeZoom, editor.model().getShapes().getFirst().getVertices());
                assertTrue(editor.button("polygon-finish").isDisabled());
                assertTrue(editor.button("polygon-cancel").isDisabled());
                assertTrue(editor.model().getPixels().isEmpty());
            }

            editor.button("clear-canvas").fire();
            editor.toggle("mode-raster").fire();
            editor.viewport().setZoom(1.6);
            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 180, 140, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 190, 140, true);
            int pixelsBeforeZoom = editor.model().getPixels().size();
            assertTrue(pixelsBeforeZoom > 1);
            wheel(editor, viewportAnchor(editor, 0.5, 0.5), 40, false, false, false);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 250, 150, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 250, 150, false);

            assertEquals(pixelsBeforeZoom, editor.model().getPixels().size());
            assertTrue(editor.model().getShapes().isEmpty());
        });
    }

    private static void triangle(Editor editor) {
        click(editor.canvas(), 100, 100);
        click(editor.canvas(), 220, 100);
        click(editor.canvas(), 220, 220);
    }

    private static void assertVerticesEquals(List<Vertex> expected, List<Vertex> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertVertexEquals(expected.get(index), actual.get(index));
        }
    }

    private static void assertVertexEquals(Vertex expected, Vertex actual) {
        assertEquals(expected.x(), actual.x(), 1e-9);
        assertEquals(expected.y(), actual.y(), 1e-9);
    }

    private static void assertCanvasCenteredAndVisible(Editor editor) {
        ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
        Node visibleArea = scroll.lookup(".viewport");
        assertNotNull(visibleArea);
        Point2D topLeft = visibleArea.sceneToLocal(editor.canvas().localToScene(0, 0));
        Point2D bottomRight = visibleArea.sceneToLocal(editor.canvas().localToScene(900, 650));
        double width = scroll.getViewportBounds().getWidth();
        double height = scroll.getViewportBounds().getHeight();
        assertTrue(topLeft.getX() >= -1);
        assertTrue(topLeft.getY() >= -1);
        assertTrue(bottomRight.getX() <= width + 1);
        assertTrue(bottomRight.getY() <= height + 1);
        assertEquals(width / 2, (topLeft.getX() + bottomRight.getX()) / 2, 2);
        assertEquals(height / 2, (topLeft.getY() + bottomRight.getY()) / 2, 2);
    }

    private static Point2D viewportAnchor(Editor editor, double horizontalFraction, double verticalFraction) {
        ScrollPane scroll = editor.node("canvas-scroll", ScrollPane.class);
        return new Point2D(scroll.getViewportBounds().getWidth() * horizontalFraction,
                scroll.getViewportBounds().getHeight() * verticalFraction);
    }

    private static Point2D logicalAt(Editor editor, Point2D anchor) {
        Node visibleArea = editor.node("canvas-scroll", ScrollPane.class).lookup(".viewport");
        assertNotNull(visibleArea);
        return editor.canvas().sceneToLocal(visibleArea.localToScene(anchor));
    }

    private static void assertPointEquals(Point2D expected, Point2D actual) {
        assertEquals(expected.getX(), actual.getX(), 1);
        assertEquals(expected.getY(), actual.getY(), 1);
    }

    private static void wheel(Editor editor, Point2D anchor, double delta,
                              boolean control, boolean command, boolean direct) {
        Point2D logical = logicalAt(editor, anchor);
        Point2D scene = editor.canvas().localToScene(logical);
        Point2D screen = editor.canvas().localToScreen(logical);
        assertNotNull(screen);
        Event.fireEvent(editor.canvas(), new ScrollEvent(ScrollEvent.SCROLL,
                scene.getX(), scene.getY(), screen.getX(), screen.getY(),
                false, control, false, command, direct, false,
                0, delta, 0, delta, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                ScrollEvent.VerticalTextScrollUnits.NONE, 0, direct ? 2 : 0,
                new PickResult(editor.canvas(), new Point3D(logical.getX(), logical.getY(), 0), 0)));
    }

    private static void pinch(Editor editor, Point2D anchor, double factor) {
        Point2D logical = logicalAt(editor, anchor);
        Point2D scene = editor.canvas().localToScene(logical);
        Point2D screen = editor.canvas().localToScreen(logical);
        assertNotNull(screen);
        Event.fireEvent(editor.canvas(), new ZoomEvent(ZoomEvent.ZOOM,
                scene.getX(), scene.getY(), screen.getX(), screen.getY(),
                false, false, false, false, false, false, factor, factor,
                new PickResult(editor.canvas(), new Point3D(logical.getX(), logical.getY(), 0), 0)));
    }

    private static void key(Node target, KeyCode code) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false));
    }

    private static void shortcut(Node target, KeyCode code, boolean shift, boolean command) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                shift, !command, false, command));
    }

    private static void click(DrawingCanvas canvas, double x, double y) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y, true);
        mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y, false);
    }

    private static void mouse(DrawingCanvas canvas, EventType<MouseEvent> type,
                              double x, double y, boolean primaryDown) {
        Point2D screen = canvas.localToScreen(x, y);
        assertNotNull(screen);
        Event.fireEvent(canvas, new MouseEvent(type, x, y, screen.getX(), screen.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false, primaryDown,
                false, false, true, false, type == MouseEvent.MOUSE_PRESSED,
                new PickResult(canvas, new Point3D(x, y, 0), 0)));
    }

    private static void withEditor(Consumer<Editor> assertions) throws Exception {
        withEditor(1250, 850, assertions);
    }

    private static void withEditor(double width, double height, Consumer<Editor> assertions) throws Exception {
        runFx(() -> {
            DrawingModel model = new DrawingModel(900, 650);
            EditorController controller = new EditorController(model);
            EditorPane pane = new EditorPane(model, controller);
            Stage stage = new Stage();
            try {
                Scene scene = new Scene(pane, width, height);
                scene.getStylesheets().add(Objects.requireNonNull(
                        EditorInteractionTest.class.getResource(
                                "/org/example/graphiceditor/css/application.css")).toExternalForm());
                stage.setScene(scene);
                stage.show();
                pane.applyCss();
                pane.layout();
                DrawingCanvas canvas = (DrawingCanvas) pane.lookup("#drawing-canvas");
                assertNotNull(canvas);
                assertions.accept(new Editor(model, controller, pane, canvas));
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private static <T> T runFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private record Editor(DrawingModel model, EditorController controller,
                          EditorPane pane, DrawingCanvas canvas) {
        private <T extends Node> T node(String id, Class<T> type) {
            Node node = pane.lookup("#" + id);
            assertNotNull(node, "Элемент должен быть доступен по ID " + id);
            return type.cast(node);
        }

        private CanvasViewport viewport() {
            return node("canvas-viewport", CanvasViewport.class);
        }

        private Button button(String id) {
            return node(id, Button.class);
        }

        private ToggleButton toggle(String id) {
            return node(id, ToggleButton.class);
        }

        private void resize(double width, double height) {
            pane.resize(width, height);
            pane.applyCss();
            pane.layout();
        }
    }
}
