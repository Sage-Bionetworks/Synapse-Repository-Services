package org.sagebionetworks.repo.model.drs;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DrsObjectIdTest {

    private IdAndVersion idAndVersion = IdAndVersion.parse(ENTITY_OBJECT_ID);
    private static final String ENTITY_OBJECT_ID = "syn54321.1";

    @Test
    public void testCreateDrsObjectIdWithFileHandleId() {
        // call under test
        DrsObjectId drsObjectId = DrsObjectId.parse("fh12345");
        assertEquals(DrsObjectId.Type.FILE_HANDLE, drsObjectId.getType());
        assertEquals("12345", drsObjectId.getFileHandleId().toString());
    }

    @Test
    public void testCreateDrsObjectIdWithEntityId() {
        // call under test
        DrsObjectId drsObjectId = DrsObjectId.parse("syn54321.1");
        assertEquals(DrsObjectId.Type.ENTITY, drsObjectId.getType());
        assertEquals("54321", drsObjectId.getEntityId().getId().toString());
    }

    @Test
    public void testCreateDrsObjectIdWithNull() {
        // call under test
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DrsObjectId.parse(null));
        assertEquals("objectId is required.", exception.getMessage());
    }

    @Test
    public void testGetEntityId() {
        DrsObjectId drsObjectId = DrsObjectId.parse("syn54321.1");

        // call under test
        assertEquals(idAndVersion, drsObjectId.getEntityId());
    }

    @Test
    public void testCreateDrsObjectIdWithInvalidId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DrsObjectId.parse("6789"));
        assertEquals("Object Id must be entity ID with version (e.g syn32132536.1), or the file handle ID prepended with the string \"fh\" (e.g. fh123)", exception.getMessage());
    }

    @Test
    public void testCreateDrsObjectIdNoEntityIdVersion() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DrsObjectId.parse("syn6789"));
        assertEquals("Entity ID must include version. e.g syn123.1", exception.getMessage());
    }

    @Test
    public void testCreateDrsObjectIdWithInvalidFileHandleId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DrsObjectId.parse("fh67a89"));
        assertEquals("File Handle ID must contain prefix \"fh\" followed by a Long", exception.getMessage());
    }

    @Test
    public void testGetFileHandleIdWithTypeEntity() {
        DrsObjectId drsObjectId = DrsObjectId.parse("syn54321.1");

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                drsObjectId.getFileHandleId());
        assertEquals("File handle ID can only be accessed for type FILE_HANDLE", exception.getMessage());
    }

    @Test
    public void testGetEntityIdWithTypeFileHandle() {
        DrsObjectId drsObjectId = DrsObjectId.parse("fh12345");

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                drsObjectId.getEntityId());
        assertEquals("IdAndVersion can only be accessed for type ENTITY", exception.getMessage());
    }


}

