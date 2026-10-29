package org.example.graphiceditor.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AtomicProjectWriter {
    private AtomicProjectWriter() { }

    @FunctionalInterface interface WriteAction { void write(Path temporary) throws IOException; }

    static void write(Path destination, WriteAction action) throws IOException {
        Path absolute = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(absolute.getParent(), ".graphic-editor-", ".tmp");
        try {
            action.write(temporary);
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Файловая система не поддерживает безопасную атомарную замену проекта", exception);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void text(Path destination, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > ProjectXmlCodec.MAX_TEXT_BYTES) throw new IOException("Текст проекта превышает 8 МиБ");
        write(destination, temporary -> Files.write(temporary, bytes));
    }
}
