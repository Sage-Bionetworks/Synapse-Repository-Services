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

public class PreviewManagerImpl implements  PreviewManager {
	
	static private Log log = LogFactory.getLog(PreviewManagerImpl.class);
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	@Autowired
	AmazonS3Client s3Client;
	
	List<PreviewGenerator> generatorList;
	
	/**
	 * Injected.
	 */
	private Long maxPreviewMemory;

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
		if(metadata == null) throw new IllegalArgumentException("metadata cannot be null");
		if(metadata.getContentType() == null) throw new IllegalArgumentException("metadata.getContentType() cannot be null");
		if(metadata.getContentSize() == null) throw new IllegalArgumentException("metadata.getContentSize() cannot be null");
		// Try to find a generator for this type
		PreviewGenerator generator = findPreviewGenerator(metadata.getContentType());
		// there is nothing to do if we do not have a generator for this type
		if(generator == null){
			log.info("No preview generator found for contentType:"+metadata.getContentType());
			return;
		}
		// First determine how much memory will be need to generate this preview
		double multiper = generator.getMemoryMultiplierForContentType(metadata.getContentType());
		long memoryNeededBytes = (long) (((double)metadata.getContentSize())*multiper);
		if(memoryNeededBytes > maxPreviewMemory){
			log.info(String.format("Preveiw cannot be generated.  Memory needed: '%1$s' (bytes) exceed preview memory pool size: '%2$s' (bytes). Metadata: %3$s", memoryNeededBytes, maxPreviewMemory, metadata.toString())); ;
			return;
		}
		// If here then the preview memory pool size is large enough for this file.
		// Attempt to generate a preview
		
		
		
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
	 * Called after all dependencies are allocated.
	 */
	public void initialize(){
		
	}

}
