package org.sagebionetworks.repo.model.drs;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessIdTest {

    @Test
    public void testCreate() {
        final AccessId accessId = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals("syn123.1", accessId.getSynapseIdWithVersion());
        assertEquals("1234", accessId.getFileHandleId());
    }

    @Test
    public void testCreateWithNullSynapseId() {
        try{
            new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                    .setSynapseIdWithVersion(null).setFileHandleId("1234").build();
        }
        catch (IllegalArgumentException exception){
            assertEquals("Required field missing.", exception.getMessage());
        }
    }

    @Test
    public void testCreateWithNullFileHandleAssociationType() {
        try{
            new AccessIdBuilder().setAssociateType(null)
                    .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        }
        catch (IllegalArgumentException exception){
            assertEquals("Required field missing.", exception.getMessage());
        }
    }

    @Test
    public void testEquals() {
       final AccessId one = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        final AccessId two = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        assertTrue(one.equals(two));
    }

    @Test
    public void testEqualsWithDifferentSynapseId() {
        final AccessId one = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn12.1").setFileHandleId("1234").build();
        final AccessId two = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        assertFalse(one.equals(two));
    }

    @Test
    public void testEqualsWithDifferentFileHandleId() {
       final AccessId one = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        final AccessId two = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("123").build();
        assertFalse(one.equals(two));
    }

    @Test
    public void testEqualsWithDifferentAssociationType() {
       final AccessId one = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.TableEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        final AccessId two = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        assertFalse(one.equals(two));
    }

    @Test
    public void testToString() {
        final AccessId accessId = new AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion("syn123.1").setFileHandleId("1234").build();
        assertEquals("FileEntity_syn123.1_1234", accessId.toString());
    }

    @Test
    public void testParse() {
        final AccessId accessId = AccessId.parse("FileEntity_syn123.1_1234");
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals("syn123.1", accessId.getSynapseIdWithVersion());
        assertEquals("1234", accessId.getFileHandleId());
    }
}
