package org.sagebionetworks.repo.model;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccessRecordExtractorUtilTest {

    @Test
    public void testGetObjectId() {
        FileEntity ob = new FileEntity();
        ob.setId("syn123");
        Optional<String> id = AccessRecordExtractorUtil.getObjectFieldValue(ob, "id");
        assertTrue(id.isPresent());
        assertEquals("syn123", id.get());
    }

    @Test
    public void testGetObjectIdHavingNullID() {
        FileEntity ob = new FileEntity();
        ob.setId(null);
        Optional<String> id = AccessRecordExtractorUtil.getObjectFieldValue(ob, "id");
        assertFalse(id.isPresent());
    }

    @Test
    public void testGetObjectConcreteType() {
        FileEntity ob = new FileEntity();
        ob.setConcreteType("concreteType");
        Optional<String> concreteType = AccessRecordExtractorUtil.getObjectFieldValue(ob, "concreteType");
        assertTrue(concreteType.isPresent());
        assertEquals("concreteType", concreteType.get());
    }

    @Test
    public void testGetConcreteTypeWithImplicitConcreteType() {
        S3FileHandle ob = new S3FileHandle();
        Optional<String> concreteType = AccessRecordExtractorUtil.getObjectFieldValue(ob, "concreteType");
        assertTrue(concreteType.isPresent());
        assertEquals("org.sagebionetworks.repo.model.file.S3FileHandle", concreteType.get());
    }

    @Test
    public void testEntityWithoutConcreteType() {
        UserGroup ob = new UserGroup();
        ob.setId("syn123");
        Optional<String> concreteType = AccessRecordExtractorUtil.getObjectFieldValue(ob, "concreteType");
        assertFalse(concreteType.isPresent());
    }
}
