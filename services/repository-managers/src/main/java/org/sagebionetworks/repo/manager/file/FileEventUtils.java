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
    public static FileEvent buildFileEvent(FileEventType fileEventType, Long userId, FileHandleAssociation association) {
        ValidateArgument.required(association, "association");
        return buildFileEvent(fileEventType, userId, association.getFileHandleId(), association.getAssociateObjectId(),
                association.getAssociateObjectType());
    }

    public static FileEvent buildFileEvent(FileEventType type, Long userId, String fileHandleId,
                                           String associateObjectId, FileHandleAssociateType associateObjectType) {
        ValidateArgument.required(type, "type");
        ValidateArgument.required(userId, "userId");
        ValidateArgument.required(fileHandleId, "fileHandleId");
        ValidateArgument.required(associateObjectId, "associateObjectId");
        ValidateArgument.required(associateObjectType, "associateObjectType");
        return new FileEvent()
                .setObjectType(ObjectType.FILE_EVENT)
                .setObjectId(fileHandleId)
                .setTimestamp(Date.from(Instant.now()))
                .setUserId(userId)
                .setFileEventType(type)
                .setFileHandleId(fileHandleId)
                .setAssociateId(associateObjectId)
                .setAssociateType(associateObjectType);
    }
}
