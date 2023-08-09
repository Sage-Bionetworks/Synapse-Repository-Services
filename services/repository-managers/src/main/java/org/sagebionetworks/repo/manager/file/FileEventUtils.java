package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileEvent;
import org.sagebionetworks.repo.model.file.FileEventType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.util.ValidateArgument;

import java.time.Instant;
import java.util.Date;

public class FileEventUtils {
    public static FileEvent buildFileEvent(FileEventType fileEventType, Long userId, FileHandleAssociation association,
                                           String stack, String instance) {
        ValidateArgument.required(association, "association");
        return buildFileEvent(fileEventType, userId, association.getFileHandleId(), association.getAssociateObjectId(),
                association.getAssociateObjectType(), stack, instance);
    }

    public static FileEvent buildFileEvent(FileEventType type, Long userId, String fileHandleId, String associateObjectId,
                                           FileHandleAssociateType associateObjectType, String stack, String instance) {
        ValidateArgument.required(type, "type");
        ValidateArgument.required(userId, "userId");
        ValidateArgument.required(fileHandleId, "fileHandleId");
        ValidateArgument.required(associateObjectId, "associateObjectId");
        ValidateArgument.required(associateObjectType, "associateObjectType");
        ValidateArgument.required(stack, "stack");
        ValidateArgument.required(instance, "instance");
        return buildFileEvent(type, userId, fileHandleId, null, associateObjectId, associateObjectType, stack, instance);
    }

    public static FileEvent buildFileEvent(FileEventType type, Long userId, String fileHandleId,
                                           String downloadedFileHandleId, String associateObjectId,
                                           FileHandleAssociateType associateObjectType, String stack, String instance) {
        return new FileEvent()
                .setObjectType(ObjectType.FILE_EVENT)
                .setObjectId(fileHandleId)
                .setTimestamp(Date.from(Instant.now()))
                .setUserId(userId)
                .setFileEventType(type)
                .setDownloadedFileHandleId(downloadedFileHandleId)
                .setFileHandleId(fileHandleId)
                .setAssociateId(associateObjectId)
                .setAssociateType(associateObjectType)
                .setStack(stack)
                .setInstance(instance);
    }
}
