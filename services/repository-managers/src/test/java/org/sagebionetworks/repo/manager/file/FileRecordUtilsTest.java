package org.sagebionetworks.repo.manager.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileRecordUtilsTest {
    @Test
    public void testBuildFileDownloadEvent() {
        FileRecord expectedEvent = new FileRecord().setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setUserId(123L).setFileHandleId("345").setAssociateId("678").setAssociateType(FileHandleAssociateType.FileEntity);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedEvent.getFileHandleId());
        association.setAssociateObjectId(expectedEvent.getAssociateId());
        association.setAssociateObjectType(expectedEvent.getAssociateType());

        // Call under test
        FileRecord result = FileRecordUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, expectedEvent.getUserId(),
                association);

        assertEquals(expectedEvent.getFileEventType(), result.getFileEventType());
        assertEquals(expectedEvent.getAssociateId(), result.getAssociateId());
        assertEquals(expectedEvent.getAssociateType(), result.getAssociateType());
        assertEquals(expectedEvent.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedEvent.getUserId(), result.getUserId());
    }

    @Test
    public void testBuildFileUploadEvent() {
        FileRecord expectedEvent = new FileRecord().setFileEventType(FileEventType.FILE_UPLOAD)
                .setUserId(123L).setFileHandleId("345").setAssociateId("678").setAssociateType(FileHandleAssociateType.FileEntity);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedEvent.getFileHandleId());
        association.setAssociateObjectId(expectedEvent.getAssociateId());
        association.setAssociateObjectType(expectedEvent.getAssociateType());

        // Call under test
        FileRecord result = FileRecordUtils.buildFileEvent(FileEventType.FILE_UPLOAD, expectedEvent.getUserId(),
                association);

        assertEquals(expectedEvent.getFileEventType(), result.getFileEventType());
        assertEquals(expectedEvent.getAssociateId(), result.getAssociateId());
        assertEquals(expectedEvent.getAssociateType(), result.getAssociateType());
        assertEquals(expectedEvent.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedEvent.getUserId(), result.getUserId());
    }

    @Test
    public void testWithNullParams() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileRecordUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();

            association.setFileHandleId(null);
            association.setAssociateObjectId("id");
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);

            FileRecordUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, association);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();

            association.setFileHandleId("id");
            association.setAssociateObjectId(null);
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);

            FileRecordUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, association);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();

            association.setFileHandleId("id");
            association.setAssociateObjectId("id");
            association.setAssociateObjectType(null);

            FileRecordUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, association);
        });


        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileRecordUtils.buildFileEvent(null, 123L, "123", "id", FileHandleAssociateType.FileEntity);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileRecordUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, null, "id", "id", FileHandleAssociateType.FileEntity);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileRecordUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, null, "id", FileHandleAssociateType.FileEntity);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileRecordUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", null, FileHandleAssociateType.FileEntity);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileRecordUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", "id", null);
        });
    }

}
