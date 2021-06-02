package org.sagebionetworks.repo.manager.file.scanner;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

public class FileHandleScannerUtils {

	/**
	 * @param associationType The type of association
	 * @param scanned The scanned association
	 * @return A (potentially empty) set of {@link FileHandleAssociationRecord} to be sent to kinesis from the given scanned association
	 */
	public static Set<FileHandleAssociationRecord> mapAssociation(FileHandleAssociateType associationType, ScannedFileHandleAssociation scanned, long timestamp) {
		if (scanned.getFileHandleIds() == null || scanned.getFileHandleIds().isEmpty()) {
			return Collections.emptySet();
		}
		
		return scanned.getFileHandleIds().stream()
				.map(fileHandleId -> 
					new FileHandleAssociationRecord()
							.withTimestamp(timestamp)
							.withAssociateType(associationType)
							.withAssociateId(scanned.getObjectId())
							.withFileHandleId(fileHandleId)
							
				).collect(Collectors.toSet());
	}

}
