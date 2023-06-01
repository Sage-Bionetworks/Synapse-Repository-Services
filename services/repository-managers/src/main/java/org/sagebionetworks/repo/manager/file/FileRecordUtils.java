package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileRecord;
import org.sagebionetworks.util.ValidateArgument;

import java.time.Instant;
import java.util.Date;

public class FileRecordUtils {
    public static FileRecord buildFileRecord(Long userId, String fileHandleId,
                                             String associateObjectId, FileHandleAssociateType associateObjectType,
                                             long projectId) {
        ValidateArgument.required(userId, "userId");
        ValidateArgument.required(fileHandleId, "fileHandleId");
        ValidateArgument.required(associateObjectId, "associateObjectId");
        ValidateArgument.required(associateObjectType, "associateObjectType");
        ValidateArgument.required(projectId, "projectId");
        return new FileRecord()
                .setTimestamp(Date.from(Instant.now()).getTime())
                .setUserId(userId)
                .setFileHandleId(fileHandleId)
                .setAssociateId(associateObjectId)
                .setAssociateType(associateObjectType)
                .setProjectId(projectId);
    }
}
