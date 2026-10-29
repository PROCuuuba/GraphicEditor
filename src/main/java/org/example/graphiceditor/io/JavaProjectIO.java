package org.example.graphiceditor.io;

import org.example.graphiceditor.model.DrawingModel;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class JavaProjectIO {
    private static final long MAX_BYTES = ProjectXmlCodec.MAX_TEXT_BYTES + 4096L;

    private JavaProjectIO() { }

    static void write(Path destination, DrawingModel model) throws IOException {
        SerializedProject project = new SerializedProject(1, ProjectXmlCodec.encode(model).getBytes(StandardCharsets.UTF_8));
        AtomicProjectWriter.write(destination, temporary -> {
            try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeObject(project);
            }
        });
    }

    static DrawingModel read(Path source) throws IOException {
        if (Files.size(source) > MAX_BYTES) throw new IOException("Файл GED превышает допустимый размер");
        ByteArrayInputStream bytes = new ByteArrayInputStream(Files.readAllBytes(source));
        try (ObjectInputStream input = new ObjectInputStream(bytes)) {
            input.setObjectInputFilter(info -> {
                if (info.depth() > 4 || info.references() > 8 || info.streamBytes() > MAX_BYTES
                        || info.arrayLength() > ProjectXmlCodec.MAX_TEXT_BYTES) return ObjectInputFilter.Status.REJECTED;
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                return type == SerializedProject.class || type == byte[].class
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            if (!(input.readObject() instanceof SerializedProject project)) throw new IOException("В GED отсутствует проект GraphicEditor");
            if (project.version() != 1) throw new IOException("Неподдерживаемая версия GED-проекта: " + project.version());
            byte[] data = project.xml();
            if (data == null || data.length == 0 || data.length > ProjectXmlCodec.MAX_TEXT_BYTES) throw new IOException("Некорректные данные GED-проекта");
            if (bytes.available() != 0) throw new IOException("Лишние данные после GED-проекта");
            String xml = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
            return ProjectXmlCodec.decode(xml);
        } catch (ClassNotFoundException | RuntimeException exception) {
            throw new IOException("Некорректный GED-проект", exception);
        }
    }
}
