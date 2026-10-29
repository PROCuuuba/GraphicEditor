package org.example.graphiceditor.io;

import java.io.IOException;
import java.io.Writer;

final class BoundedTextWriter extends Writer {
    private final StringBuilder text = new StringBuilder();

    @Override public void write(char[] characters, int offset, int length) throws IOException {
        if (text.length() + (long) length > ProjectXmlCodec.MAX_TEXT_BYTES) throw new IOException("Текст проекта превышает 8 МиБ");
        text.append(characters, offset, length);
    }

    @Override public void flush() { }
    @Override public void close() { }
    @Override public String toString() { return text.toString(); }
}
