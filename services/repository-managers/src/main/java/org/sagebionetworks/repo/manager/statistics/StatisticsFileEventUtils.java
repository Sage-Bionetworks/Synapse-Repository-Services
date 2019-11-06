package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.util.ValidateArgument;

public class StatisticsFileEventUtils {

	public static StatisticsFileEvent buildFileDownloadEvent(Long userId, FileHandleAssociation association) {
		return buildFileEvent(FileEvent.FILE_DOWNLOAD, userId, association);
	}

	public static StatisticsFileEvent buildFileDownloadEvent(Long userId, String fileHandleId, String associateObjectId,
			FileHandleAssociateType associateObjectType) {
		return buildFileEvent(FileEvent.FILE_DOWNLOAD, userId, fileHandleId, associateObjectId,
				associateObjectType);
	}

	public static StatisticsFileEvent buildFileUploadEvent(Long userId, FileHandleAssociation association) {
		return buildFileEvent(FileEvent.FILE_UPLOAD, userId, association);
	}

	public static StatisticsFileEvent buildFileUploadEvent(Long userId, String fileHandleId, String associateObjectId,
			FileHandleAssociateType associateObjectType) {
		return buildFileEvent(FileEvent.FILE_UPLOAD, userId, fileHandleId, associateObjectId,
				associateObjectType);
	}

	private static StatisticsFileEvent buildFileEvent(FileEvent type, Long userId,
			FileHandleAssociation association) {
		ValidateArgument.required(association, "association");

		return buildFileEvent(type, userId, association.getFileHandleId(), association.getAssociateObjectId(),
				association.getAssociateObjectType());
	}

	private static StatisticsFileEvent buildFileEvent(FileEvent type, Long userId, String fileHandleId,
			String associateObjectId, FileHandleAssociateType associateObjectType) {
		ValidateArgument.required(type, "type");
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(fileHandleId, "fileHandleId");
		ValidateArgument.required(associateObjectId, "associateObjectId");
		ValidateArgument.required(associateObjectType, "associateObjectType");
		return new StatisticsFileEvent(type, userId, fileHandleId, associateObjectId, associateObjectType);
	}

}
