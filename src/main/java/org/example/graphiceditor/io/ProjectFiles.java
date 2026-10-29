package org.example.graphiceditor.io;

import org.example.graphiceditor.model.DrawingModel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public final class ProjectFiles {
    public enum Format {
        PNG("png", "Редактируемый PNG (*.png)"),
        JSON("json", "Проект JSON (*.json)"), SVG("svg", "Редактируемый SVG (*.svg)");

        private final String extension;
        private final String description;
        Format(String extension, String description) { this.extension = extension; this.description = description; }
        public String extension() { return extension; }
        public String description() { return description; }
    }

    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private ProjectFiles() { }

    public static void write(Path path, DrawingModel model, BufferedImage preview, Format format) throws IOException {
        Objects.requireNonNull(path, "path"); Objects.requireNonNull(model, "model"); Objects.requireNonNull(format, "format");
        switch (format) {
            case PNG -> PngProjectIO.write(path, model, preview);
            case JSON -> AtomicProjectWriter.text(path, DrawingExporter.export(model, new JsonDrawingBuilder()).content());
            case SVG -> AtomicProjectWriter.text(path, DrawingExporter.export(model, new SvgDrawingBuilder()).content());
        }
    }

    public static DrawingModel read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        byte[] signature;
        try (InputStream input = Files.newInputStream(path)) { signature = input.readNBytes(8); }
        if (Arrays.equals(signature, PNG_SIGNATURE)) return PngProjectIO.read(path);
        if (Files.size(path) > ProjectXmlCodec.MAX_TEXT_BYTES) throw new IOException("Текст проекта превышает 8 МиБ");
        String text = Files.readString(path, StandardCharsets.UTF_8).strip();
        if (text.startsWith("\ufeff")) text = text.substring(1).stripLeading();
        if (text.startsWith("{")) return JsonProjectReader.read(text);
        if (text.startsWith("<")) return SvgProjectReader.read(text);
        throw new IOException("Неизвестный формат проекта. Поддерживаются PNG, JSON и SVG редактора GraphicEditor.");
    }
}
