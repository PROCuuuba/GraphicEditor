package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.controller.PolygonController;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectFilesTest {
    @TempDir Path directory;

    @Test
    void allFourFormatsKeepMixedObjectsExactStylesAndBackgroundAndAreDetectedByContent() throws IOException {
        DrawingModel original = example();
        for (ProjectFiles.Format format : ProjectFiles.Format.values()) {
            Path misleadingExtension = directory.resolve(format.name() + ".not-the-real-extension");

            ProjectFiles.write(misleadingExtension, original, preview(), format);
            DrawingModel restored = ProjectFiles.read(misleadingExtension);

            assertEquals(ProjectXmlCodec.encode(original), ProjectXmlCodec.encode(restored), format.name());
            assertEquals(original.getBackgroundColor(), restored.getBackgroundColor());
            PolygonController controller = new PolygonController(restored);
            controller.selectShape(0);
            assertTrue(controller.startVertexDrag(10.125, 11.75, 0));
            assertTrue(controller.dragVertexTo(18.5, 28.25));
            controller.endVertexDrag();
            assertEquals(new Vertex(18.5, 28.25), restored.getShape(0).getVertices().getFirst());
            Path edited = directory.resolve("edited-" + format.name());
            ProjectFiles.write(edited, restored, preview(), format);
            assertEquals(ProjectXmlCodec.encode(restored), ProjectXmlCodec.encode(ProjectFiles.read(edited)));
        }
    }

    @Test
    void changingFormatDoesNotFlattenPolygonsOrLoseDecoratorChainOrder() throws IOException {
        DrawingModel drawing = example();
        String original = ProjectXmlCodec.encode(drawing);
        for (ProjectFiles.Format format : List.of(ProjectFiles.Format.PNG, ProjectFiles.Format.JAVA,
                ProjectFiles.Format.JSON, ProjectFiles.Format.SVG, ProjectFiles.Format.PNG)) {
            Path file = directory.resolve("converted." + format.extension());
            ProjectFiles.write(file, drawing, preview(), format);
            drawing = ProjectFiles.read(file);
            assertEquals(original, ProjectXmlCodec.encode(drawing));
        }
    }

    @Test
    void legacyVersionOneWithoutBackgroundStillOpensWithWhiteBackground() throws IOException {
        String legacy = ProjectXmlCodec.encode(example()).replaceAll(" background-[rgba]=\"[^\"]*\"", "");

        DrawingModel drawing = ProjectXmlCodec.decode(legacy);

        assertEquals(Color.WHITE, drawing.getBackgroundColor());
        assertEquals(3, drawing.size());
        assertEquals(2, drawing.getShapes().size());
    }

    @Test
    void svgContainsActualVisualPrimitivesBackgroundAlphaAndIndependentShadow() throws IOException {
        SvgFile svg = DrawingExporter.export(example(), new SvgDrawingBuilder());

        assertTrue(svg.content().contains("xmlns=\"http://www.w3.org/2000/svg\""));
        assertTrue(svg.content().contains("<circle"));
        assertTrue(svg.content().contains("<polygon"));
        assertTrue(svg.content().contains("stroke-width=\"3.125\""));
        assertTrue(svg.content().contains("<feGaussianBlur"));
        assertTrue(svg.content().contains("fill-opacity=\"0.75\""));
        assertTrue(svg.content().contains("<metadata id=\"GraphicEditor.Project\""));
        assertEquals(ProjectXmlCodec.encode(example()), ProjectXmlCodec.encode(SvgProjectReader.read(svg.content())));
    }

    @Test
    void invalidJsonVersionsGeometryPropertiesDuplicatesAndTruncationAreRejected() throws IOException {
        String valid = DrawingExporter.export(example(), new JsonDrawingBuilder()).content();
        for (String text : List.of(valid.replace("\"version\" : 1", "\"version\" : 99"),
                valid.replace("\"x\" : 1.125", "\"x\" : 9999"),
                valid.replace("\"size\" : 9.375", "\"size\" : -1"),
                valid.replace("\"type\" : \"point\"", "\"type\" : \"triangle\""),
                valid.replace("\"format\" : \"GraphicEditor\"", "\"format\" : \"GraphicEditor\", \"format\" : \"GraphicEditor\""),
                valid.replace("\"background\"", "\"unknown\""), valid.substring(0, valid.length() / 2), valid + "{}")) {
            Path path = directory.resolve("bad.json");
            Files.writeString(path, text);
            assertThrows(IOException.class, () -> ProjectFiles.read(path), text);
        }
    }

    @Test
    void svgWithoutProjectOrWithUnsafeXmlAndDifferentDimensionsIsRejected() throws IOException {
        String svg = DrawingExporter.export(example(), new SvgDrawingBuilder()).content();
        String unsafe = "<!DOCTYPE svg [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" + svg.substring(svg.indexOf("<svg"));
        for (String text : List.of("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"96\" height=\"72\"/>",
                unsafe, svg.replaceFirst("width=\"96.0\"", "width=\"95.0\""), svg.substring(0, svg.length() / 2))) {
            Path path = directory.resolve("bad.svg");
            Files.writeString(path, text);
            assertThrows(IOException.class, () -> ProjectFiles.read(path));
        }
    }

    @Test
    void javaSerializationRejectsUnknownObjectsBeforeTheirReadObjectCanExecute() throws IOException {
        DangerousObject.executed = false;
        Path file = directory.resolve("unknown.ged");
        serialize(file, new DangerousObject());

        assertThrows(IOException.class, () -> ProjectFiles.read(file));

        assertFalse(DangerousObject.executed);
    }

    @Test
    void javaSerializationRejectsUnsupportedVersionInvalidPayloadTruncationAndTrailingObjects() throws IOException {
        byte[] xml = ProjectXmlCodec.encode(example()).getBytes(StandardCharsets.UTF_8);
        Path file = directory.resolve("bad.ged");
        for (Object object : List.of(new SerializedProject(99, xml), new SerializedProject(1, new byte[]{1, 2, 3}),
                new SerializedProject(1, null), "not a project")) {
            serialize(file, object);
            assertThrows(IOException.class, () -> ProjectFiles.read(file));
        }
        ProjectFiles.write(file, example(), null, ProjectFiles.Format.JAVA);
        byte[] valid = Files.readAllBytes(file);
        Files.write(file, Arrays.copyOf(valid, valid.length / 2));
        assertThrows(IOException.class, () -> ProjectFiles.read(file));
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(file))) {
            output.writeObject(new SerializedProject(1, xml)); output.writeObject("trailing object");
        }
        assertThrows(IOException.class, () -> ProjectFiles.read(file));
    }

    @Test
    void failedTextOrJavaExportPreservesExistingFileAndLeavesNoTemporaryFiles() throws IOException {
        Path destination = directory.resolve("old-project");
        byte[] old = "old file content".getBytes(StandardCharsets.UTF_8);
        Files.write(destination, old);
        DrawingModel oversized = new DrawingModel(5000, 100);
        for (ProjectFiles.Format format : ProjectFiles.Format.values()) {
            assertThrows(IOException.class, () -> ProjectFiles.write(destination, oversized, preview(), format));
            assertArrayEquals(old, Files.readAllBytes(destination));
        }
        assertThrows(IOException.class, () -> AtomicProjectWriter.write(destination, temporary -> {
            Files.writeString(temporary, "partial content"); throw new IOException("simulated disk failure");
        }));
        assertArrayEquals(old, Files.readAllBytes(destination));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    private static void serialize(Path file, Object object) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(file))) { output.writeObject(object); }
    }

    private static BufferedImage preview() { return new BufferedImage(96, 72, BufferedImage.TYPE_INT_ARGB); }

    private static DrawingModel example() {
        DrawingModel drawing = new DrawingModel(96, 72);
        drawing.setBackgroundColor(Color.color(.125, .25, .5, .75));
        drawing.add(new Point(1.125, 2.25, 1, Color.color(.125, .25, .5, .75)));
        drawing.add(new Circle(4.25, 5.5, 9.375, Color.TRANSPARENT));
        drawing.add(new Square(7.125, 8.25, 10.5, Color.DARKBLUE));
        Polygon polygon = new Polygon(List.of(new Vertex(10.125, 11.75), new Vertex(80.5, 14.25), new Vertex(41.25, 60.875)));
        Shape decorated = new ShadowDecorator(new BorderDecorator(new FillDecorator(polygon, Color.color(.4, .2, .8, .5)),
                Color.GREEN, 3.125), Color.color(.2, .3, .4, .75), -4.25, 7.125, 12.5);
        drawing.addShape(decorated);
        drawing.addShape(new FillDecorator(new BorderDecorator(new FillDecorator(polygon, Color.RED), Color.BLUE, 6.5), Color.CORAL));
        return drawing;
    }

    private static final class DangerousObject implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        static boolean executed;
        @Serial private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            executed = true; input.defaultReadObject();
        }
    }
}
