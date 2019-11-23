/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.io;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * XML encoder.
 */
public class XMLEncoder extends Encoder<Iterable<? extends Map<String, ?>>> {
    /**
     * Constructs a new XML encoder.
     */
    public XMLEncoder() {
        super(StandardCharsets.UTF_8);
    }

    @Override
    public void write(Iterable<? extends Map<String, ?>> values, Writer writer) throws IOException {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);

            streamWriter.writeStartDocument();
            streamWriter.writeStartElement("root");

            writeValues(values, streamWriter);

            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
        } catch (XMLStreamException exception) {
            throw new IOException(exception);
        }

        writer.flush();
    }

    private void writeValues(Iterable<?> values, XMLStreamWriter streamWriter) throws XMLStreamException {
        for (Object value : values) {
            if (value instanceof Map<?, ?>) {
                streamWriter.writeStartElement("item");

                writeMap((Map<?, ?>)value, streamWriter);

                streamWriter.writeEndElement();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeMap(Map<?, ?> map, XMLStreamWriter streamWriter) throws XMLStreamException {
        HashMap<Object, Map<?, ?>> maps = new HashMap<>();
        HashMap<Object, Iterable<?>> sequences = new HashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();

            if (key == null) {
                continue;
            }

            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            if (value instanceof Map<?, ?>) {
                maps.put(key, (Map<?, ?>)value);
            } else if (value instanceof Iterable<?>) {
                sequences.put(key, (Iterable<? extends Map<String, ?>>)value);
            } else {
                encode(key.toString(), value, streamWriter);
            }
        }

        for (Map.Entry<Object, Map<?, ?>> entry : maps.entrySet()) {
            streamWriter.writeStartElement(entry.getKey().toString());

            writeMap(entry.getValue(), streamWriter);

            streamWriter.writeEndElement();
        }

        for (Map.Entry<Object, Iterable<?>> entry : sequences.entrySet()) {
            streamWriter.writeStartElement(entry.getKey().toString());

            writeValues(entry.getValue(), streamWriter);

            streamWriter.writeEndElement();
        }
    }

    private void encode(String key, Object value, XMLStreamWriter streamWriter) throws XMLStreamException {
        if (value instanceof Enum<?>) {
            encode(key, ((Enum<?>)value).ordinal(), streamWriter);
        } else if (value instanceof Date) {
            encode(key, ((Date)value).getTime(), streamWriter);
        } else {
            streamWriter.writeAttribute(key, value.toString());
        }
    }
}
