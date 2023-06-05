package org.sagebionetworks.repo.manager.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FileEventUtilsTest {
    private static final String STACK = "stack";
    private static final String INSTANCE = "instance";

    @Test
    public void testBuildFileDownloadEvent() {
        FileEvent expectedEvent = new FileEvent().setFileEventType(FileEventType.FILE_DOWNLOAD)
                .setObjectType(ObjectType.FILE_EVENT).setObjectId("345")
                .setUserId(123L).setFileHandleId("345").setAssociateId("678")
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack(STACK).setInstance(INSTANCE);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedEvent.getFileHandleId());
        association.setAssociateObjectId(expectedEvent.getAssociateId());
        association.setAssociateObjectType(expectedEvent.getAssociateType());

        // Call under test
        FileEvent result = FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, expectedEvent.getUserId(),
                association, STACK, INSTANCE);
        assertNotNull(result.getTimestamp());
        expectedEvent.setTimestamp(result.getTimestamp());
        assertEquals(expectedEvent, result);
    }

    @Test
    public void testBuildFileUploadEvent() {
        FileEvent expectedEvent = new FileEvent().setFileEventType(FileEventType.FILE_UPLOAD)
                .setObjectType(ObjectType.FILE_EVENT).setObjectId("345")
                .setUserId(123L).setFileHandleId("345").setAssociateId("678")
                .setAssociateType(FileHandleAssociateType.FileEntity).setStack(STACK).setInstance(INSTANCE);

        FileHandleAssociation association = new FileHandleAssociation();

        association.setFileHandleId(expectedEvent.getFileHandleId());
        association.setAssociateObjectId(expectedEvent.getAssociateId());
        association.setAssociateObjectType(expectedEvent.getAssociateType());

        // Call under test
        FileEvent result = FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, expectedEvent.getUserId(),
                association, STACK, INSTANCE);
        assertNotNull(result.getTimestamp());
        expectedEvent.setTimestamp(result.getTimestamp());
        assertEquals(expectedEvent, result);
    }

    @Test
    public void testWithNullAssociation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, null, STACK, INSTANCE);
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
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, association, STACK, INSTANCE);
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
            FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, association, STACK, INSTANCE);
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
            FileEventUtils.buildFileEvent(FileEventType.FILE_UPLOAD, 123L, association, STACK, INSTANCE);
        });
    }

    @Test
    public void testWithNullEventType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(null, 123L, "123", "id",
                    FileHandleAssociateType.FileEntity, STACK, INSTANCE);
        });
    }

    @Test
    public void testWithNullUserId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, null, "id", "id",
                    FileHandleAssociateType.FileEntity, STACK, INSTANCE);
        });
    }

    @Test
    public void testWithNullFileHandleId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, null, "id",
                    FileHandleAssociateType.FileEntity, STACK, INSTANCE);
        });
    }

    @Test
    public void testWithNullAssociationObjectId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", null,
                    FileHandleAssociateType.FileEntity, STACK, INSTANCE);
        });
    }

    @Test
    public void testWithNullAssociationObjectType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", "id",
                    null, STACK, INSTANCE);
        });
    }

    @Test
    public void testWithNullStack() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", "id",
                    FileHandleAssociateType.FileEntity, null, INSTANCE);
        });
    }

    @Test
    public void testWithNullInstance() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            FileEventUtils.buildFileEvent(FileEventType.FILE_DOWNLOAD, 123L, "id", "id",
                    FileHandleAssociateType.FileEntity, STACK, null);
        });
    }
}
