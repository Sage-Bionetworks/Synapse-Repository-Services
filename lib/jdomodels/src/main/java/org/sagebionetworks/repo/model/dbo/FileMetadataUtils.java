package org.sagebionetworks.repo.model.dbo;

import org.sagebionetworks.repo.model.dbo.persistence.DBOFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;

/**
 * Translates between DBOs and DTOs.
 * @author John
 *
 */
public class FileMetadataUtils {
	
	/**
	 * For external data.
	 * @param metadata
	 * @return
	 */
	public static DBOFileMetadata createDBOFromDTO(FileMetadata dto){
		return null;
	}
	
	/**
	 * Create a DTO from the DBO.
	 * @param dbo
	 * @return
	 */
	public static FileMetadata createDTOFromDBO(DBOFileMetadata dbo){
		return null;
	}

}
