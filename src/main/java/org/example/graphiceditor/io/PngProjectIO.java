package org.example.graphiceditor.io;

import org.example.graphiceditor.model.DrawingModel;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.CRC32;

public final class PngProjectIO {
    public static final String PROJECT_KEYWORD = "GraphicEditor.Project";
    private static final String PNG_METADATA_FORMAT = "javax_imageio_png_1.0";
    private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final long MAX_FILE_BYTES = 128L * 1024 * 1024;

    private PngProjectIO() { }

    public static void write(Path destination, DrawingModel model, BufferedImage preview) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(preview, "preview");
        String project = ProjectXmlCodec.encode(model);
        if (preview.getWidth() != (int) Math.ceil(model.getWidth())
                || preview.getHeight() != (int) Math.ceil(model.getHeight())) {
            throw new IOException("Размер PNG-превью не совпадает с размером холста проекта");
        }
        Path absolute = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(absolute.getParent(), ".graphic-editor-", ".png.tmp");
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) throw new IOException("В системе отсутствует PNG-кодировщик");
            ImageWriter writer = writers.next();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(temporary.toFile())) {
                if (output == null) throw new IOException("Не удалось открыть временный PNG-файл");
                writer.setOutput(output);
                IIOMetadata metadata = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(preview), writer.getDefaultWriteParam());
                IIOMetadataNode root = new IIOMetadataNode(PNG_METADATA_FORMAT);
                IIOMetadataNode text = new IIOMetadataNode("iTXt");
                IIOMetadataNode entry = new IIOMetadataNode("iTXtEntry");
                entry.setAttribute("keyword", PROJECT_KEYWORD);
                entry.setAttribute("compressionFlag", "FALSE");
                entry.setAttribute("compressionMethod", "0");
                entry.setAttribute("languageTag", "");
                entry.setAttribute("translatedKeyword", "");
                entry.setAttribute("text", project);
                text.appendChild(entry);
                root.appendChild(text);
                metadata.mergeTree(PNG_METADATA_FORMAT, root);
                writer.write(null, new IIOImage(preview, null, metadata), writer.getDefaultWriteParam());
            } finally {
                writer.dispose();
            }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Файловая система не поддерживает безопасную атомарную замену PNG", exception);
            }
        } catch (RuntimeException exception) {
            throw new IOException("Не удалось сохранить PNG-проект: " + exception.getMessage(), exception);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static DrawingModel read(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        long size = Files.size(source);
        if (size < 8 || size > MAX_FILE_BYTES) throw invalid("недопустимый размер файла (максимум 128 МиБ)");
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            if (!Arrays.equals(input.readNBytes(8), SIGNATURE)) throw invalid("файл не является PNG");
            long consumed = 8;
            int width = 0;
            int height = 0;
            String project = null;
            boolean hasImage = false;
            boolean endedImage = false;
            while (true) {
                long length = Integer.toUnsignedLong(input.readInt());
                byte[] typeBytes = input.readNBytes(4);
                if (typeBytes.length != 4 || length > MAX_FILE_BYTES || consumed + length + 12 > size) {
                    throw invalid("обрезанный или слишком большой PNG-блок");
                }
                for (byte value : typeBytes) {
                    if (!(value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z')) {
                        throw invalid("некорректный тип PNG-блока");
                    }
                }
                String type = new String(typeBytes, StandardCharsets.US_ASCII);
                if ((typeBytes[2] & 32) != 0) throw invalid("некорректный тип PNG-блока");
                if (consumed == 8 && !type.equals("IHDR")) throw invalid("отсутствует заголовок IHDR");
                if (type.equals("IHDR") && (consumed != 8 || length != 13)) throw invalid("некорректный IHDR");
                if (type.equals("iTXt") && length > ProjectXmlCodec.MAX_TEXT_BYTES + 1024L) {
                    throw invalid("текстовые данные превышают 8 МиБ");
                }
                boolean keep = type.equals("IHDR") || type.equals("iTXt");
                byte[] data = keep ? new byte[(int) length] : null;
                CRC32 checksum = new CRC32();
                checksum.update(typeBytes);
                byte[] buffer = keep ? data : new byte[8192];
                int offset = 0;
                long remaining = length;
                while (remaining > 0) {
                    int count = input.read(buffer, keep ? offset : 0, (int) Math.min(remaining, keep ? length : buffer.length));
                    if (count < 0) throw invalid("обрезанный PNG-блок");
                    checksum.update(buffer, keep ? offset : 0, count);
                    offset += count;
                    remaining -= count;
                }
                if (Integer.toUnsignedLong(input.readInt()) != checksum.getValue()) throw invalid("не совпадает CRC PNG-блока " + type);
                consumed += length + 12;
                switch (type) {
                    case "IHDR" -> {
                        ByteBuffer header = ByteBuffer.wrap(data);
                        width = header.getInt();
                        height = header.getInt();
                        ProjectXmlCodec.dimensions(width, height);
                        int depth = Byte.toUnsignedInt(data[8]);
                        int color = Byte.toUnsignedInt(data[9]);
                        boolean validDepth = switch (color) {
                            case 0 -> depth == 1 || depth == 2 || depth == 4 || depth == 8 || depth == 16;
                            case 2, 4, 6 -> depth == 8 || depth == 16;
                            case 3 -> depth == 1 || depth == 2 || depth == 4 || depth == 8;
                            default -> false;
                        };
                        if (!validDepth || data[10] != 0 || data[11] != 0 || data[12] < 0 || data[12] > 1) {
                            throw invalid("неподдерживаемый заголовок PNG");
                        }
                    }
                    case "iTXt" -> {
                        String extracted = projectText(data);
                        if (extracted != null) {
                            if (project != null) throw invalid("несколько блоков проекта в одном PNG");
                            project = extracted;
                        }
                        if (hasImage) endedImage = true;
                    }
                    case "IDAT" -> {
                        if (endedImage) throw invalid("несмежные блоки IDAT");
                        hasImage = true;
                    }
                    case "IEND" -> {
                        if (length != 0 || !hasImage || consumed != size) throw invalid("некорректное завершение PNG");
                        if (project == null) {
                            throw new IOException("В PNG нет редактируемого проекта GraphicEditor. Откройте PNG, созданный через «Сохранить PNG…» этого редактора.");
                        }
                        DrawingModel model = ProjectXmlCodec.decode(project);
                        if (Math.ceil(model.getWidth()) != width || Math.ceil(model.getHeight()) != height) {
                            throw invalid("размер изображения не совпадает с размером проекта");
                        }
                        return model;
                    }
                    case "PLTE" -> {
                        if (hasImage || length == 0 || length > 768 || length % 3 != 0) throw invalid("некорректная палитра PNG");
                    }
                    default -> {
                        if ((typeBytes[0] & 32) == 0) throw invalid("неизвестный обязательный PNG-блок " + type);
                        if (hasImage) endedImage = true;
                    }
                }
            }
        } catch (EOFException exception) {
            throw invalid("файл PNG обрезан", exception);
        }
    }

    private static String projectText(byte[] data) throws IOException {
        int separator = zero(data, 0);
        if (separator < 1 || separator > 79) throw invalid("некорректный ключ iTXt");
        String keyword = new String(data, 0, separator, StandardCharsets.ISO_8859_1);
        if (!keyword.equals(PROJECT_KEYWORD)) return null;
        if (separator + 3 >= data.length || data[separator + 1] != 0 || data[separator + 2] != 0) {
            throw invalid("проект должен храниться в несжатом UTF-8 iTXt");
        }
        int languageEnd = zero(data, separator + 3);
        int translatedEnd = languageEnd < 0 ? -1 : zero(data, languageEnd + 1);
        if (translatedEnd < 0) throw invalid("повреждены поля iTXt");
        int textStart = translatedEnd + 1;
        if (data.length - textStart > ProjectXmlCodec.MAX_TEXT_BYTES) throw invalid("данные проекта превышают 8 МиБ");
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data, textStart, data.length - textStart)).toString();
        } catch (CharacterCodingException exception) {
            throw invalid("некорректный UTF-8 проекта", exception);
        }
    }

    private static int zero(byte[] data, int start) {
        for (int index = start; index < data.length; index++) if (data[index] == 0) return index;
        return -1;
    }

    private static IOException invalid(String detail) { return invalid(detail, null); }

    private static IOException invalid(String detail, Throwable cause) {
        return new IOException("Некорректный PNG-проект: " + detail, cause);
    }
}
