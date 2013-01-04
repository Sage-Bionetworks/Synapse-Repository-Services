package org.sagebionetworks.repo.manager.file.preview;

import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.util.ResourceTracker.ResourceTempoarryUnavailable;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

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
	 * @throws Exception 
	 * @throws ServiceUnavailableException 
	 */
	public PreviewFileMetadata generatePreview(S3FileMetadata metadta) throws ResourceTempoarryUnavailable, ExceedsMaximumResources, Exception;

}
