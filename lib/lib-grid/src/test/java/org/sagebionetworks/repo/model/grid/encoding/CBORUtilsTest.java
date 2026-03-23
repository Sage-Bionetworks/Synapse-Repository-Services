package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class CBORUtilsTest {

    static final long replicaId = 123;


    public enum ConValueEncodingTestCase {
        NULL(ConType.NULL, null),
        UNDEFINED(ConType.UNDEFINED, null),
        TIMESTAMP(ConType.TIMESTAMP, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(456L)),
        EMPTY_STRING(ConType.STRING, ""),
        STRING(ConType.STRING, "hello"),
        LONG_ZERO(ConType.LONG, 0L),
        LONG(ConType.LONG, 123L),
        LONG_NEGATIVE(ConType.LONG, -123L),
        DOUBLE(ConType.DOUBLE, 1.25),
        DOUBLE_ZERO(ConType.DOUBLE, 0.0),
        BOOLEAN_TRUE(ConType.BOOLEAN, true),
        BOOLEAN_FALSE(ConType.BOOLEAN, false),
        ARRAY(ConType.JSON_ARRAY, new JSONArray("[1,2,3]")),
        ARRAY_EMPTY(ConType.JSON_ARRAY, new JSONArray("[]")),
        ARRAY_VARIOUS_INTERNAL_TYPES(ConType.JSON_ARRAY, new JSONArray("[\"str\", 42, 1.5, true, null, [1,2], {\"a\": \"b\"}]")),
        OBJECT(ConType.JSON_OBJECT, new JSONObject("{\"key\":99}")),
        OBJECT_EMPTY(ConType.JSON_OBJECT, new JSONObject("{}")),
        OBJECT_VARIOUS_INTERNAL_TYPES(ConType.JSON_OBJECT, new JSONObject("{\"str\": \"foo\", \"int\": 42, \"float\": 1.5, \"bool\": true, \"null\": null, \"arr\": [1,2], \"obj\": {\"a\": \"b\"}}"));


        ConType type;
        Object value;

        ConValueEncodingTestCase(ConType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
    @ParameterizedTest
    @EnumSource(ConValueEncodingTestCase.class)
    public void testEncodeDecodeConValue(ConValueEncodingTestCase testCase) {
        ClockTable clockTable = new ClockTable(List.of(
                // ensure the replica ID in the clock table matches that in the test case
                new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(500L))
        );

        // create a representative value for each supported class
        ConValue original = new ConValue(testCase.type, testCase.value);

        byte[] asCbor = CBORUtils.encodeConValue(original, clockTable);
        boolean isTimestamp = ConType.TIMESTAMP.equals(testCase.type);
        ConValue decoded = CBORUtils.decodeConValue(new ByteArrayInputStream(asCbor), clockTable, isTimestamp);

        assertEquals(original, decoded);
    }

}
