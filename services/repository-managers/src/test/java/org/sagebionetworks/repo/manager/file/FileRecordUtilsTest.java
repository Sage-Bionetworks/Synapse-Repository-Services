package org.sagebionetworks.repo.manager.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileRecordUtilsTest {
    @Test
    public void testBuildFileDownloadRecord() {
        FileRecord expectedRecord = new FileRecord()
                .setUserId(123L).setFileHandleId("345").setAssociateId("678").setAssociateType(FileHandleAssociateType.FileEntity);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedRecord.getFileHandleId());
        association.setAssociateObjectId(expectedRecord.getAssociateId());
        association.setAssociateObjectType(expectedRecord.getAssociateType());

        // Call under test
        FileRecord result = FileRecordUtils.buildFileRecord(expectedRecord.getUserId(), association);

        assertEquals(expectedRecord.getAssociateId(), result.getAssociateId());
        assertEquals(expectedRecord.getAssociateType(), result.getAssociateType());
        assertEquals(expectedRecord.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedRecord.getUserId(), result.getUserId());
    }

    @Test
    public void testBuildFileUploadRecord() {
        FileRecord expectedRecord = new FileRecord()
                .setUserId(123L).setFileHandleId("345").setAssociateId("678").setAssociateType(FileHandleAssociateType.FileEntity);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedRecord.getFileHandleId());
        association.setAssociateObjectId(expectedRecord.getAssociateId());
        association.setAssociateObjectType(expectedRecord.getAssociateType());

        // Call under test
        FileRecord result = FileRecordUtils.buildFileRecord(expectedRecord.getUserId(),
                association);

        assertEquals(expectedRecord.getAssociateId(), result.getAssociateId());
        assertEquals(expectedRecord.getAssociateType(), result.getAssociateType());
        assertEquals(expectedRecord.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedRecord.getUserId(), result.getUserId());
    }

    @Test
    public void testWithNullAssociation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileRecordUtils.buildFileRecord(123L, null);
        });
    }

    @Test
    public void testWithNullUserId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId(null);
            association.setAssociateObjectId("id");
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileRecordUtils.buildFileRecord(null, association);
        });
    }

    @Test
    public void testWithNullAssociationId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId("123");
            association.setAssociateObjectId(null);
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileRecordUtils.buildFileRecord(123L, association);
        });
    }

    @Test
    public void testWithNullAssociationObjectType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId("123");
            association.setAssociateObjectId("syn123");
            association.setAssociateObjectType(null);
            // Call under test
            FileRecordUtils.buildFileRecord(123L, association);
        });
    }

    @Test
    public void testWithNullFileHandleId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId(null);
            association.setAssociateObjectId("syn123");
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileRecordUtils.buildFileRecord(123L, association);
        });
    }

}
