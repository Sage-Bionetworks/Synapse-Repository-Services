package org.sagebionetworks.repo.manager.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileEventUtilsTest {
    @Test
    public void testBuildFileDownloadEvent() {
        FileEvent expectedEvent = new FileEvent().setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setUserId(123L).setFileHandleId("345").setAssociateId("678").setAssociateType(FileHandleAssociateType.FileEntity);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedEvent.getFileHandleId());
        association.setAssociateObjectId(expectedEvent.getAssociateId());
        association.setAssociateObjectType(expectedEvent.getAssociateType());

        // Call under test
        FileEvent result = FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, expectedEvent.getUserId(),
                association);

        assertEquals(expectedEvent.getFileEventType(), result.getFileEventType());
        assertEquals(expectedEvent.getAssociateId(), result.getAssociateId());
        assertEquals(expectedEvent.getAssociateType(), result.getAssociateType());
        assertEquals(expectedEvent.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedEvent.getUserId(), result.getUserId());
    }

    @Test
    public void testBuildFileUploadEvent() {
        FileEvent expectedEvent = new FileEvent().setFileEventType(FileEventType.FILE_UPLOAD)
                .setUserId(123L).setFileHandleId("345").setAssociateId("678").setAssociateType(FileHandleAssociateType.FileEntity);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedEvent.getFileHandleId());
        association.setAssociateObjectId(expectedEvent.getAssociateId());
        association.setAssociateObjectType(expectedEvent.getAssociateType());

        // Call under test
        FileEvent result = FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, expectedEvent.getUserId(),
                association);

        assertEquals(expectedEvent.getFileEventType(), result.getFileEventType());
        assertEquals(expectedEvent.getAssociateId(), result.getAssociateId());
        assertEquals(expectedEvent.getAssociateType(), result.getAssociateType());
        assertEquals(expectedEvent.getFileHandleId(), result.getFileHandleId());
        assertEquals(expectedEvent.getUserId(), result.getUserId());
    }

    @Test
    public void testWithNullAssociation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, null);
        });
    }

    @Test
    public void testWithNullFileHandleIdInAssociation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId(null);
            association.setAssociateObjectId("id");
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, association);
        });
    }

    @Test
    public void testWithNullAssociateObjectId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId("id");
            association.setAssociateObjectId(null);
            association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, association);
        });
    }

    @Test
    public void testWithNullAssociateObjectType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileHandleAssociation association = new FileHandleAssociation();
            association.setFileHandleId("id");
            association.setAssociateObjectId("id");
            association.setAssociateObjectType(null);
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, association);
        });
    }

    @Test
    public void testWithNullEventType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(null, 123L, "123", "id", FileHandleAssociateType.FileEntity);
        });
    }

    @Test
    public void testWithNullUserId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, null, "id", "id",
                    FileHandleAssociateType.FileEntity);
        });
    }

    @Test
    public void testWithNullFileHandleId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, null, "id",
                    FileHandleAssociateType.FileEntity);
        });
    }

    @Test
    public void testWithNullAssociationObjectIdParams() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", null,
                    FileHandleAssociateType.FileEntity);
        });
    }

    @Test
    public void testWithNullAssociationObjectTypeParams() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", "id", null);
        });
    }
}
