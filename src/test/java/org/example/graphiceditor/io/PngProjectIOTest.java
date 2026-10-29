package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.controller.PolygonController;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.ShapeDecorator;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class PngProjectIOTest {
    @TempDir Path temporary;

    @Test
    void roundTripKeepsExactOrderedBrushMarksVerticesAndDecoratorsAndOrdinaryPreview() throws IOException {
        DrawingModel original = example();
        BufferedImage preview = preview();
        preview.setRGB(7, 9, 0x7f123456);
        Path path = temporary.resolve("редактируемый рисунок.png");

        PngProjectIO.write(path, original, preview);
        DrawingModel imported = PngProjectIO.read(path);

        assertDrawingEquals(original, imported);
        BufferedImage ordinaryImage = ImageIO.read(path.toFile());
        assertEquals(96, ordinaryImage.getWidth());
        assertEquals(72, ordinaryImage.getHeight());
        assertEquals(0x7f123456, ordinaryImage.getRGB(7, 9));
        String fileText = new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1);
        assertTrue(fileText.contains("iTXt" + PngProjectIO.PROJECT_KEYWORD));
        assertTrue(fileText.contains("<graphic-editor-project version=\"1\""));
    }

    @Test
    void importedPolygonCanBeSelectedRestyledMovedAndSavedAgain() throws IOException {
        Path first = temporary.resolve("first.png");
        PngProjectIO.write(first, example(), preview());
        DrawingModel imported = PngProjectIO.read(first);
        PolygonController controller = new PolygonController(imported);
        controller.selectShape(0);
        VectorStyle replacement = new VectorStyle(true, Color.ORANGE, true, Color.BLUE, 4.75,
                true, Color.color(.1, .2, .3, .4), -4.25, 7.125, 13.5);
        controller.setStyle(replacement);
        assertTrue(controller.applyStyleToSelected());
        assertTrue(controller.startVertexDrag(10.125, 11.75, 0));
        assertTrue(controller.dragVertexTo(15.5, 22.25));
        controller.endVertexDrag();

        Path second = temporary.resolve("second.png");
        PngProjectIO.write(second, imported, preview());
        DrawingModel edited = PngProjectIO.read(second);

        assertEquals(new Vertex(15.5, 22.25), edited.getShape(0).getVertices().getFirst());
        assertEquals(replacement, VectorStyle.from(edited.getShape(0)));
        assertEquals(imported.getPixels().size(), edited.getPixels().size());
        assertDrawingEquals(imported, edited);
    }

    @Test
    void ordinaryPngClearlyReportsThatItContainsNoEditableProject() throws IOException {
        Path path = temporary.resolve("ordinary.png");
        ImageIO.write(preview(), "png", path.toFile());

        IOException error = assertThrows(IOException.class, () -> PngProjectIO.read(path));

        assertTrue(error.getMessage().contains("нет редактируемого проекта"));
    }

    @Test
    void unsupportedVersionIsRejectedWithoutAttemptingToInterpretItsObjects() throws IOException {
        Path path = withProject(ProjectXmlCodec.encode(example()).replace("version=\"1\"", "version=\"99\""));

        assertTrue(assertThrows(IOException.class, () -> PngProjectIO.read(path)).getMessage()
                .contains("неподдерживаемая версия"));
    }

    @Test
    void malformedAndUnsafeProjectMetadataIsRejected() throws IOException {
        String xml = ProjectXmlCodec.encode(example());
        List<String> invalidProjects = List.of(
                xml.replace("x=\"1.125\"", "x=\"NaN\""),
                xml.replace("x=\"1.125\"", "x=\"200\""),
                xml.replace("<pixels>", "<pixels unknown=\"true\">"),
                xml.replace("<point", "<unknown").replace("</point>", "</unknown>"),
                xml.replace("width=\"96.0\"", "width=\"4097\""),
                xml.replace("r=\"0.125\"", "r=\"1.1\""),
                xml.substring(0, xml.length() / 2),
                "<!DOCTYPE graphic-editor-project [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                        + xml.substring(xml.indexOf("<graphic-editor-project")).replace("<pixels>", "<pixels>&xxe;"));
        for (String invalid : invalidProjects) {
            Path path = withProject(invalid);
            assertThrows(IOException.class, () -> PngProjectIO.read(path), invalid);
        }
    }

    @Test
    void pngCorruptionDuplicateMetadataAndDimensionMismatchAreRejected() throws IOException {
        Path valid = temporary.resolve("valid.png");
        PngProjectIO.write(valid, example(), preview());
        byte[] bytes = Files.readAllBytes(valid);
        bytes[bytes.length - 1] ^= 1; // IEND CRC.
        Path corrupt = temporary.resolve("crc.png");
        Files.write(corrupt, bytes);
        assertTrue(assertThrows(IOException.class, () -> PngProjectIO.read(corrupt)).getMessage().contains("CRC"));
        Files.write(corrupt, new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> PngProjectIO.read(corrupt));
        Files.write(corrupt, java.util.Arrays.copyOf(Files.readAllBytes(valid), 30));
        assertThrows(IOException.class, () -> PngProjectIO.read(corrupt));
        Path duplicate = withProject(ProjectXmlCodec.encode(example()), 2);
        assertTrue(assertThrows(IOException.class, () -> PngProjectIO.read(duplicate)).getMessage().contains("несколько блоков"));
        Path mismatch = withProject(ProjectXmlCodec.encode(example()).replace("width=\"96.0\"", "width=\"95\""));
        assertTrue(assertThrows(IOException.class, () -> PngProjectIO.read(mismatch)).getMessage().contains("размер"));
    }

    @Test
    void previewMismatchDoesNotReplaceAnExistingProject() throws IOException {
        Path destination = temporary.resolve("existing.png");
        PngProjectIO.write(destination, example(), preview());
        byte[] before = Files.readAllBytes(destination);

        assertThrows(IOException.class, () -> PngProjectIO.write(destination, example(), new BufferedImage(2, 2, 2)));

        assertArrayEquals(before, Files.readAllBytes(destination));
        assertEquals(1, fileCount());
    }

    @Test
    void failedImageEncoderLeavesTheOldFileAndCleansUpTemporaryFile() throws IOException {
        Path destination = temporary.resolve("existing.png");
        PngProjectIO.write(destination, example(), preview());
        byte[] before = Files.readAllBytes(destination);
        BufferedImage broken = new BufferedImage(96, 72, BufferedImage.TYPE_INT_ARGB) {
            @Override public Raster getData(Rectangle rectangle) {
                throw new IllegalStateException("simulated image encoding failure");
            }
            @Override public Raster getTile(int tileX, int tileY) {
                throw new IllegalStateException("simulated image encoding failure");
            }
            @Override public WritableRaster getRaster() {
                throw new IllegalStateException("simulated image encoding failure");
            }
        };

        assertThrows(IOException.class, () -> PngProjectIO.write(destination, example(), broken));

        assertArrayEquals(before, Files.readAllBytes(destination));
        assertDrawingEquals(example(), PngProjectIO.read(destination));
        assertEquals(1, fileCount());
    }

    @Test
    void fractionalCanvasDimensionsSurviveUsingCeiledRasterDimensions() throws IOException {
        DrawingModel model = new DrawingModel(95.25, 71.125);
        model.add(new Circle(95.125, 71.0, 2.25, Color.TRANSPARENT));
        Path path = temporary.resolve("fractional.png");

        PngProjectIO.write(path, model, preview());

        assertDrawingEquals(model, PngProjectIO.read(path));
    }

    private long fileCount() throws IOException {
        try (var files = Files.list(temporary)) { return files.count(); }
    }

    private Path withProject(String project) throws IOException { return withProject(project, 1); }

    // Independent PNG fixture builder: inserts the standard iTXt bytes before IDAT.
    private Path withProject(String project, int copies) throws IOException {
        ByteArrayOutputStream ordinary = new ByteArrayOutputStream();
        ImageIO.write(preview(), "png", ordinary);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (DataInputStream input = new DataInputStream(new java.io.ByteArrayInputStream(ordinary.toByteArray()));
             DataOutputStream output = new DataOutputStream(result)) {
            output.write(input.readNBytes(8));
            boolean inserted = false;
            while (input.available() > 0) {
                int length = input.readInt();
                byte[] type = input.readNBytes(4);
                byte[] data = input.readNBytes(length);
                int crc = input.readInt();
                if (!inserted && new String(type, StandardCharsets.US_ASCII).equals("IDAT")) {
                    ByteArrayOutputStream text = new ByteArrayOutputStream();
                    text.write(PngProjectIO.PROJECT_KEYWORD.getBytes(StandardCharsets.ISO_8859_1));
                    text.write(new byte[]{0, 0, 0, 0, 0});
                    text.write(project.getBytes(StandardCharsets.UTF_8));
                    byte[] payload = text.toByteArray();
                    byte[] textType = "iTXt".getBytes(StandardCharsets.US_ASCII);
                    CRC32 checksum = new CRC32();
                    checksum.update(textType);
                    checksum.update(payload);
                    for (int copy = 0; copy < copies; copy++) {
                        output.writeInt(payload.length);
                        output.write(textType);
                        output.write(payload);
                        output.writeInt((int) checksum.getValue());
                    }
                    inserted = true;
                }
                output.writeInt(length);
                output.write(type);
                output.write(data);
                output.writeInt(crc);
            }
        }
        Path path = Files.createTempFile(temporary, "metadata-", ".png");
        Files.write(path, result.toByteArray());
        return path;
    }

    private static BufferedImage preview() { return new BufferedImage(96, 72, BufferedImage.TYPE_INT_ARGB); }

    private static DrawingModel example() {
        DrawingModel model = new DrawingModel(96, 72);
        Color exact = Color.color(.125, .25, .5, .75);
        model.add(new Point(1.125, 2.25, 10, exact));
        model.add(new Square(2.625, 3.875, 7.125, Color.color(.7, .3, .9, .2)));
        model.add(new Circle(4.25, 5.5, 9.375, Color.TRANSPARENT));
        model.add(new Point(1.125, 2.25, 1, Color.BLUE)); // Same location, distinct ordered mark.
        Polygon polygon = new Polygon(List.of(new Vertex(10.125, 11.75), new Vertex(80.5, 14.25), new Vertex(41.25, 60.875)));
        model.addShape(new VectorStyle(true, exact, true, Color.GREEN, 3.125,
                true, Color.color(.2, .4, .6, .8), -6.25, 4.125, 12.5).decorate(polygon));
        // Deliberately nonstandard chain: preserve actual decorator order as well as values.
        model.addShape(new FillDecorator(new BorderDecorator(new FillDecorator(polygon, Color.RED), Color.BLUE, 6.5), exact));
        model.addShape(polygon);
        return model;
    }

    private static void assertDrawingEquals(DrawingModel expected, DrawingModel actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(expected.getPixels().size(), actual.getPixels().size());
        for (int index = 0; index < expected.size(); index++) {
            Pixel before = expected.getPixels().get(index);
            Pixel after = actual.getPixels().get(index);
            assertEquals(before.getClass(), after.getClass());
            assertEquals(before.getX(), after.getX());
            assertEquals(before.getY(), after.getY());
            assertEquals(before.getSize(), after.getSize());
            assertEquals(before.getColor(), after.getColor());
        }
        assertEquals(expected.getShapes().size(), actual.getShapes().size());
        for (int index = 0; index < expected.getShapes().size(); index++) {
            Shape before = expected.getShape(index);
            Shape after = actual.getShape(index);
            assertEquals(before.getVertices(), after.getVertices());
            while (before instanceof ShapeDecorator decorator) {
                assertEquals(before.getClass(), after.getClass());
                switch (decorator) {
                    case FillDecorator fill -> assertEquals(fill.getColor(), ((FillDecorator) after).getColor());
                    case BorderDecorator border -> {
                        assertEquals(border.getColor(), ((BorderDecorator) after).getColor());
                        assertEquals(border.getWidth(), ((BorderDecorator) after).getWidth());
                    }
                    case ShadowDecorator shadow -> {
                        ShadowDecorator other = (ShadowDecorator) after;
                        assertEquals(shadow.getColor(), other.getColor());
                        assertEquals(shadow.getOffsetX(), other.getOffsetX());
                        assertEquals(shadow.getOffsetY(), other.getOffsetY());
                        assertEquals(shadow.getBlur(), other.getBlur());
                    }
                    default -> fail("unexpected decorator");
                }
                before = decorator.getWrapped();
                after = ((ShapeDecorator) after).getWrapped();
            }
            assertEquals(before.getClass(), after.getClass());
        }
    }
}
