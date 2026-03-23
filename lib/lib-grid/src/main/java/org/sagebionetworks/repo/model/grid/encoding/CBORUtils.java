package org.sagebionetworks.repo.model.grid.encoding;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

public class CBORUtils {

    private static final CBORMapper CBOR_MAPPER = new CBORMapper();
    private static final CBORFactory CBOR_FACTORY = CBOR_MAPPER.getFactory();

    private static final int UNDEFINED_BYTE = 0xF7;

    public static CBORMapper getCBORMapper() {
        return CBOR_MAPPER;
    }

    public static CBORFactory getCBORFactory() {
        return CBOR_FACTORY;
    }


    public static JsonNode parseJsonNode(InputStream inputStream) {
        // 1. Create the wrapper to prevent Jackson/Buffering over-reads
        InputStream unbufferedStream = new NonClosingSingleByteInputStream(inputStream);
        // Use Jackson CBOR to decode the value
        try {
            return getCBORMapper().readTree(unbufferedStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode ConValue from CBOR", e);
        }
    }

    /**
     * A FilterInputStream that only allows reading one byte at a time, and does not
     * close the underlying stream when closed.
     *
     * We use this to prevent Jackson from over-reading and closing our shared InputStream when
     * decoding CBOR values.
     */
    static class NonClosingSingleByteInputStream extends FilterInputStream {
        public NonClosingSingleByteInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            // NOTE: Reading only one byte at a time can dramatically slow down Jackson (10-100x), but is the most
            // straightforward way to prevent over-reads.
            // Alternative approaches (e.g. using PushbackInputStream to "unread" excess bytes) add significant
            // complexity and potential for bugs.
            int result = in.read();
            if (result == -1) return -1;
            b[off] = (byte) result;
            return 1; // Force 1 byte at a time
        }

        @Override
        public void close() throws IOException {
            // No-op to protect 'in'
        }
    }

    /**
     * Decode a timestamp ConValue from the input stream.
     *
     * @param inputStream the input stream containing the encoded timestamp
     * @param clockTable the clock table for decoding timestamps
     * @return the decoded timestamp ConValue
     */
    private static ConValue decodeTimestamp(InputStream inputStream, ClockTable clockTable) {
        try {
            LogicalTimestamp ts = clockTable.decodeTimestamp(inputStream);
            return new ConValue(ConType.TIMESTAMP, ts);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode timestamp from binary", e);
        }
    }

    /**
     * Check if the input stream starts with a CBOR undefined byte.
     * If it does, return an UNDEFINED ConValue. Otherwise, unread the byte and return null.
     *
     * @param pushbackStream the pushback input stream to check
     * @return a ConValue with UNDEFINED type if the stream starts with an undefined byte, null otherwise
     */
    private static ConValue checkForUndefined(PushbackInputStream pushbackStream) {
        try {
            int firstByte = pushbackStream.read();
            if (firstByte == UNDEFINED_BYTE) {
                return new ConValue(ConType.UNDEFINED, null);
            }
            pushbackStream.unread(firstByte); // Put it back for Jackson to read
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from stream", e);
        }
    }

    /**
     * Decode a ConValue from CBOR/binary format.
     *
     * @param inputStream the input stream containing the encoded value
     * @param clockTable the clock table for decoding timestamps
     * @param isTimestamp true if the value is a logical timestamp (indicated by e=1 in node header)
     * @return the decoded ConValue
     */
    public static ConValue decodeConValue(InputStream inputStream, ClockTable clockTable, boolean isTimestamp) {
        ValidateArgument.required(inputStream, "inputStream");
        ValidateArgument.required(clockTable, "clockTable");

        // If the node header indicated this is a timestamp (e=1), decode it as such
        if (isTimestamp) {
            return decodeTimestamp(inputStream, clockTable);
        }

        // Check for CBOR undefined before parsing
        // Jackson treats undefined as null, so we need to detect it manually
        PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 1);
        ConValue undefinedValue = checkForUndefined(pushbackStream);
        if (undefinedValue != null) {
            return undefinedValue;
        }

        // Use Jackson CBOR to decode the value
        JsonNode jsonNode = parseJsonNode(pushbackStream);
        return convertJsonNodeToConValue(jsonNode);
    }

    /**
     * Convert a Jackson JsonNode to a ConValue with the appropriate type.
     *
     * @param jsonNode the JsonNode to convert
     * @return the corresponding ConValue
     */
    private static ConValue convertJsonNodeToConValue(JsonNode jsonNode) {
        if (jsonNode.isNull()) {
            return new ConValue(ConType.NULL, null);
        } else if (jsonNode.isBoolean()) {
            return new ConValue(ConType.BOOLEAN, jsonNode.asBoolean());
        } else if (jsonNode.isIntegralNumber()) {
            return new ConValue(ConType.LONG, jsonNode.asLong());
        } else if (jsonNode.isFloatingPointNumber()) {
            return new ConValue(ConType.DOUBLE, jsonNode.asDouble());
        } else if (jsonNode.isTextual()) {
            return new ConValue(ConType.STRING, jsonNode.asText());
        } else if (jsonNode.isArray()) {
            JSONArray jsonArray = new JSONArray();
            for (JsonNode element : jsonNode) {
                jsonArray.put(convertJsonNodeToOrgJsonCompatible(element));
            }
            return new ConValue(ConType.JSON_ARRAY, jsonArray);
        } else if (jsonNode.isObject()) {
            JSONObject jsonObject = new JSONObject();
            jsonNode.properties().forEach(entry -> {
                jsonObject.put(entry.getKey(), convertJsonNodeToOrgJsonCompatible(entry.getValue()));
            });
            return new ConValue(ConType.JSON_OBJECT, jsonObject);
        } else {
            throw new IllegalArgumentException("Unsupported CBOR node type: " + jsonNode.getNodeType());
        }
    }

    /**
     * Convert a Jackson JsonNode to a Java object suitable for org.json types.
     */
    private static Object convertJsonNodeToOrgJsonCompatible(JsonNode node) {
        if (node.isNull()) {
            return JSONObject.NULL;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isIntegralNumber()) {
            // NOTE: `org.json` creates `Integer` values by default when parsing an array or object (though Long is
            // likely more correct for JavaScript integers). `asInt` here ensures behavior matches (and equality checks
            // work as expected).
            return node.asInt();
        } else if (node.isFloatingPointNumber()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray()) {
            JSONArray arr = new JSONArray();
            for (JsonNode element : node) {
                arr.put(convertJsonNodeToOrgJsonCompatible(element));
            }
            return arr;
        } else if (node.isObject()) {
            JSONObject obj = new JSONObject();
            node.properties().forEach(entry -> {
                obj.put(entry.getKey(), convertJsonNodeToOrgJsonCompatible(entry.getValue()));
            });
            return obj;
        }
        return node.asText();
    }


    /**
     * Convert this ConValue to binary representation using CBOR encoding.
     * For TIMESTAMP types, the LogicalTimestamp's binary format is used.
     * For other types, CBOR encoding is used.
     *
     * @return the binary representation of this value
     */
    public static byte[] encodeConValue(ConValue conValue, ClockTable clockTable) {
        ConType type = conValue.getType();
        Object value = conValue.getValue();
        try {
            if (ConType.TIMESTAMP.equals(type)) {
                return clockTable.encodeTimestamp((LogicalTimestamp) value);
            } else if (ConType.UNDEFINED.equals(type)) {
                return new byte[] { (byte) UNDEFINED_BYTE };
            } else {
                // Use CBOR for other types
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    try (CBORGenerator generator = getCBORFactory().createGenerator(baos)) {
                        if (ConType.NULL.equals(type)) {
                            generator.writeNull();
                        } else {
                            writeJsonValueToCbor(generator, value);
                        }
                    }
                    return baos.toByteArray();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode ConValue to binary", e);
        }
    }

    /**
     * Helper method to write a JSON value to CBOR.
     */
    private static void writeJsonValueToCbor(CBORGenerator generator, Object value) throws IOException {
        if (value == null || value == JSONObject.NULL) {
            generator.writeNull();
        } else if (value instanceof Boolean) {
            generator.writeBoolean((Boolean) value);
        } else if (value instanceof Long) {
            generator.writeNumber((Long) value);
        } else if (value instanceof Integer) {
            generator.writeNumber((Integer) value);
        } else if (value instanceof Double) {
            generator.writeNumber((Double) value);
        } else if (value instanceof String) {
            generator.writeString((String) value);
        } else if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            generator.writeStartArray(arr, arr.length());
            for (int i = 0; i < arr.length(); i++) {
                writeJsonValueToCbor(generator, arr.get(i));
            }
            generator.writeEndArray();
        } else if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            generator.writeStartObject();
            for (String key : obj.keySet()) {
                generator.writeFieldName(key);
                writeJsonValueToCbor(generator, obj.get(key));
            }
            generator.writeEndObject();
        } else {
            generator.writeString(value.toString());
        }
    }


}