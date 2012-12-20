package org.sagebionetworks.repo.manager.file.preview;

import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * An abstraction for preview generation.
 * 
 * @author John
 *
 */
public interface PreviewManager {
	
	/**
	 * Get the metadata for a given file.
	 * @param id
	 * @return
	 */
	public FileMetadata getFileMetadata(String id) throws NotFoundException;
	
	/**
	 * Generate a preview for the passed file.
	 * @param metadta
	 */
	public void generatePreview(S3FileMetadata metadta);

}
