package org.sagebionetworks.repo.manager.file.preview;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class PreviewManagerImpl implements  PreviewManager{
	
	static private Log log = LogFactory.getLog(PreviewManagerImpl.class);
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	@Autowired
	AmazonS3Client s3Client;
	
	List<PreviewGenerator> generatorList;
	
	/**
	 * Injected.
	 */
	private Integer numberOfPreviewWorkerThreads;
	/**
	 * Injected.
	 */
	private Long maxPreviewMemory;
	
	/**
	 * This is calculated and cached.
	 */
	private Long maxFileSizeForPreview;
	
	
	/**
	 * Injected.
	 * @param numberOfPreviewWorkerThreads
	 */
	public void setNumberOfPreviewWorkerThreads(Integer numberOfPreviewWorkerThreads) {
		this.numberOfPreviewWorkerThreads = numberOfPreviewWorkerThreads;
	}

	/**
	 * Injected
	 * @param maxPreviewMemory
	 */
	public void setMaxPreviewMemory(Long maxPreviewMemory) {
		this.maxPreviewMemory = maxPreviewMemory;
	}

	@Override
	public FileMetadata getFileMetadata(String id) throws NotFoundException {
		return fileMetadataDao.get(id);
	}

	@Override
	public void generatePreview(S3FileMetadata metadata) {
		// Try to find a generator for this type
		PreviewGenerator generator = findPreviewGenerator(metadata.getContentType());
		// there is nothing to do if we do not have a generator for this type
		if(generator == null){
			log.info("No preview generator found for contentType:"+metadata.getContentType());
			return;
		}
		// Download the preview file
		
		
	}

	/**
	 * Find
	 * @param metadta
	 */
	private PreviewGenerator findPreviewGenerator(String contentType) {
		for(PreviewGenerator gen: generatorList){
			if(gen.supportsContentType(contentType)){
				return gen;
			}
		}
		return null;
	}
	
	/**
	 * Since some preview generation techniques need to load the entire file
	 * into memory, we need to limit the size of the files that we attempt to load.
	 * @return
	 */
	public long getMaxFileSizeForPreviewBytes(){
		// Lazy calcuate this.
		if(maxFileSizeForPreview == null){
			// first determine the worst case memory use.
			float maxMemoryNeededAsMultipleOfFileSize = 0.0f;
			for(PreviewGenerator gen: generatorList){
				// Look for the worst case.
				maxMemoryNeededAsMultipleOfFileSize = Math.max(gen.memoryNeededAsMultipleOfFileSize(), maxMemoryNeededAsMultipleOfFileSize);
			}
			// Now if we assume all previews meet the worst case then we can calculate the maximum file size we can support.
			long maxMemoryPerThread = maxPreviewMemory/numberOfPreviewWorkerThreads;
			maxFileSizeForPreview = (long) (maxMemoryPerThread/maxMemoryNeededAsMultipleOfFileSize);
		}
		return maxFileSizeForPreview;
	}

}
