package org.sagebionetworks.repo.manager.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileRecordUtilsTest {
    @Test
    public void testBuildFileRecord() {
        FileRecord expectedRecord = new FileRecord().setUserId(123L).setFileHandleId("345").setAssociateId("678")
                .setAssociateType(FileHandleAssociateType.FileEntity).setProjectId(1L);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedRecord.getFileHandleId());
        association.setAssociateObjectId(expectedRecord.getAssociateId());
        association.setAssociateObjectType(expectedRecord.getAssociateType());

        // Call under test
        FileRecord result = FileRecordUtils.buildFileRecord(expectedRecord.getUserId(), expectedRecord.getFileHandleId(),
                expectedRecord.getAssociateId(), expectedRecord.getAssociateType(), expectedRecord.getProjectId());

        assertEquals(expectedRecord.getAssociateId(), result.getAssociateId());
        assertEquals(expectedRecord.getAssociateType(), result.getAssociateType());
        assertEquals(expectedRecord.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedRecord.getUserId(), result.getUserId());
        assertEquals(expectedRecord.getProjectId(), result.getProjectId());
    }

    @Test
    public void testWithNullUserId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId("123");
            association.setAssociateObjectId("id");
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileRecordUtils.buildFileRecord(null, association.getFileHandleId(), association.getAssociateObjectId(),
                    association.getAssociateObjectType(), 1L);
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
            FileRecordUtils.buildFileRecord(1L, association.getFileHandleId(), association.getAssociateObjectId(),
                    association.getAssociateObjectType(), 1L);
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
            FileRecordUtils.buildFileRecord(null, association.getFileHandleId(), association.getAssociateObjectId(),
                    association.getAssociateObjectType(), 1L);
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
            FileRecordUtils.buildFileRecord(null, association.getFileHandleId(), association.getAssociateObjectId(),
                    association.getAssociateObjectType(), 1L);
        });
    }

    @Test
    public void testWithNullProjectId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId(null);
            association.setAssociateObjectId("syn123");
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileRecordUtils.buildFileRecord(null, association.getFileHandleId(), association.getAssociateObjectId(),
                    association.getAssociateObjectType(), 1L);
        });
    }
}
