package org.sagebionetworks.repo.manager.file.preview;

import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

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
	public PreviewFileMetadata generatePreview(S3FileMetadata metadta) throws TemporarilyUnavailableException, ExceedsMaximumResources, Exception;
	
	/**
	 * Get the maximum memory (bytes) that can be used for generating previews.
	 * 
	 * @return
	 */
	public long getMaxPreivewMemoryBytes();

}
