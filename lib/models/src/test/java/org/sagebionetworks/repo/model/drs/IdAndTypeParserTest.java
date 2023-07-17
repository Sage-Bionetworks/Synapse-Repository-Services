package org.sagebionetworks.repo.model.drs;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class IdAndTypeParserTest {

    private final IdAndVersion idAndVersion = IdAndVersion.parse(ENTITY_OBJECT_ID);
    private static final String FILE_HANDLE_OBJECT_ID = "fh12345";
    private static final String ENTITY_OBJECT_ID = "syn54321.1";

    @Test
    public void testBuildObjectIdParserWithFileHandleId() {
        // call under test
        final IdAndTypeParser idAndTypeParser = IdAndTypeParser.Builder().setObjectId(FILE_HANDLE_OBJECT_ID).build();
        assertEquals(IdAndTypeParser.Type.FILE_HANDLE, idAndTypeParser.getType());
        assertEquals("12345", idAndTypeParser.getIdAndVersion().getId().toString());
    }

    @Test
    public void testBuildObjectIdParserWithEntityId() {
        // call under test
        final IdAndTypeParser idAndTypeParser = new IdAndTypeParser.Builder().setObjectId(ENTITY_OBJECT_ID).build();
        assertEquals(IdAndTypeParser.Type.FILE_ENTITY, idAndTypeParser.getType());
        assertEquals("54321", idAndTypeParser.getIdAndVersion().getId().toString());
    }

    @Test
    public void testBuildObjectIdParserWithNull() {
        // call under test
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new IdAndTypeParser.Builder().setObjectId(null).build());
        assertEquals("objectId is required.", exception.getMessage());
    }

    @Test
    public void testGetIdAndVersion() {
        final IdAndTypeParser idAndTypeParser = new IdAndTypeParser.Builder().setObjectId(ENTITY_OBJECT_ID).build();

        // call under test
        assertEquals(idAndVersion, idAndTypeParser.getIdAndVersion());
    }

    @Test
    public void testBuildObjectIdParserWithInvalidId() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new IdAndTypeParser.Builder().setObjectId("6789").build());
        assertEquals("Invalid Object ID: 6789", exception.getMessage());
    }

    @Test
    public void testBuildObjectIdParserNoEntityIdVersion() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new IdAndTypeParser.Builder().setObjectId("syn6789").build());
        assertEquals("Object id should include version. e.g syn123.1", exception.getMessage());
    }

    @Test
    public void testObjectIdParserWithInvalidFileHandleId() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new IdAndTypeParser.Builder().setObjectId("fh67a89").build());
        assertEquals("Invalid Object ID: fh67a89", exception.getMessage());
    }


}

