package org.example.graphiceditor.io;

import org.example.graphiceditor.model.DrawingModel;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;

final class SvgProjectReader {
    private SvgProjectReader() { }

    static DrawingModel read(String text) throws IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> { throw new XMLStreamException("Внешние ресурсы запрещены"); });
        XMLStreamReader xml = null;
        try {
            xml = factory.createXMLStreamReader(new StringReader(text));
            int depth = 0;
            String project = null;
            double width = 0, height = 0;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) throw invalid("DTD и сущности запрещены");
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (++depth > 32) throw invalid("слишком большая вложенность XML");
                    if (depth == 1) {
                        if (!"svg".equals(xml.getLocalName()) || !SvgDrawingBuilder.SVG_NAMESPACE.equals(xml.getNamespaceURI())) throw invalid("ожидался SVG");
                        width = Double.parseDouble(xml.getAttributeValue(null, "width"));
                        height = Double.parseDouble(xml.getAttributeValue(null, "height"));
                        ProjectXmlCodec.dimensions(width, height);
                    }
                    if ("metadata".equals(xml.getLocalName()) && PngProjectIO.PROJECT_KEYWORD.equals(xml.getAttributeValue(null, "id"))) {
                        if (project != null || depth != 2) throw invalid("некорректный блок проекта");
                        StringBuilder data = new StringBuilder();
                        while (xml.hasNext()) {
                            int content = xml.next();
                            if (content == XMLStreamConstants.END_ELEMENT) break;
                            if (content != XMLStreamConstants.CHARACTERS && content != XMLStreamConstants.CDATA
                                    && content != XMLStreamConstants.SPACE) throw invalid("некорректные метаданные");
                            if (data.length() + (long) xml.getTextLength() > ProjectXmlCodec.MAX_TEXT_BYTES) throw invalid("слишком большой проект");
                            data.append(xml.getText());
                        }
                        project = data.toString();
                        depth--;
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) depth--;
            }
            if (project == null) throw invalid("нет редактируемого проекта GraphicEditor");
            DrawingModel model = ProjectXmlCodec.decode(project);
            if (model.getWidth() != width || model.getHeight() != height) throw invalid("размер SVG не совпадает с размером проекта");
            return model;
        } catch (XMLStreamException | IllegalArgumentException | NullPointerException exception) {
            throw new IOException("Некорректный SVG-проект", exception);
        } finally {
            if (xml != null) try { xml.close(); } catch (XMLStreamException ignored) { }
        }
    }

    private static IOException invalid(String detail) { return new IOException("Некорректный SVG-проект: " + detail); }
}
