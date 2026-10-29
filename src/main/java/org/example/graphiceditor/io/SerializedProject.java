package org.example.graphiceditor.io;

import java.io.Serial;
import java.io.Serializable;

record SerializedProject(int version, byte[] xml) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    SerializedProject {
        if (xml != null) xml = xml.clone();
    }

    @Override public byte[] xml() { return xml == null ? null : xml.clone(); }
}
