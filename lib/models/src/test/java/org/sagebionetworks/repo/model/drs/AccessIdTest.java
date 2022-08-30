package org.sagebionetworks.repo.model.drs;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class AccessIdTest {

    private final IdAndVersion idAndVersion = IdAndVersion.parse("syn123.1");

    @Test
    public void testCreate() {
        final AccessId accessId = new AccessId.AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion(this.idAndVersion).setFileHandleId("1234").build();
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals(this.idAndVersion, accessId.getSynapseIdWithVersion());
        assertEquals("1234", accessId.getFileHandleId());
    }

    @Test
    public void testCreateWithNullSynapseId() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new AccessId.AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                        .setSynapseIdWithVersion(null).setFileHandleId("1234").build());
        assertEquals("synapseIdWithVersion is required.", exception.getMessage());

    }

    @Test
    public void testCreateWithNullFileHandleAssociationType() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new AccessId.AccessIdBuilder().setAssociateType(null)
                        .setSynapseIdWithVersion(this.idAndVersion).setFileHandleId("1234").build());
        assertEquals("fileHandleAssociationType is required.", exception.getMessage());
    }

    @Test
    public void testEquals() {
        final AccessId one = new AccessId.AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion(this.idAndVersion).setFileHandleId("1234").build();
        final AccessId two = new AccessId.AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion(this.idAndVersion).setFileHandleId("1234").build();
        assertEquals(one, two);
    }


    @Test
    public void testEncode() {
        final String encodedId = AccessId.encode(new AccessId.AccessIdBuilder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion(this.idAndVersion).setFileHandleId("1234").build());

        assertEquals("FileEntity_syn123.1_1234", encodedId);
    }

    @Test
    public void testDecode() {
        final AccessId accessId = AccessId.decode("FileEntity_syn123.1_1234");
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals(this.idAndVersion, accessId.getSynapseIdWithVersion());
        assertEquals("1234", accessId.getFileHandleId());
    }

    @Test
    public void testDecodeWithWhiteSpace() {
        final AccessId accessId = AccessId.decode("\t\nFileEntity_syn123.1_1234\t\n");
        assertEquals(FileHandleAssociateType.FileEntity, accessId.getAssociateType());
        assertEquals(this.idAndVersion, accessId.getSynapseIdWithVersion());
        assertEquals("1234", accessId.getFileHandleId());
    }

    @Test
    public void testDecodeWithNullString() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AccessId.decode(null));
        assertEquals("AccessId must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testDecodeWithIncorrectFileHandleAssociationType() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AccessId.decode("fileEntity_syn1.1_123"));
        assertEquals("AccessId must contain a valid file handle association type.",
                exception.getMessage());
    }

    @Test
    public void testDecodeWithSynapseIdWithoutSyn() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AccessId.decode("FileEntity_1.1_123"));
        assertEquals("AccessId must contain syn prefix with id and version.eg FileEntity_syn123.1_12345",
                exception.getMessage());
    }

    @Test
    public void testSynapseIdWithoutVersion() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AccessId.decode("FileEntity_syn1_123"));
        assertEquals("Synapse id should include version. e.g syn123.1",
                exception.getMessage());
    }

    @Test
    public void testDecodeWithIncorrectFileHandleId() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AccessId.decode("FileEntity_syn1.1_123A"));
        assertEquals("AccessId must contain valid file handle id.",
                exception.getMessage());
    }

    @Test
    public void testDecodeWithoutFileHandleId() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AccessId.decode("FileEntity_syn1.1"));
        assertEquals("Invalid accessId",
                exception.getMessage());
    }
}
