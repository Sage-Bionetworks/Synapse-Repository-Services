package org.sagebionetworks.repo.manager.file.preview;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class PreviewManagerImpl implements  PreviewManager{
	
	static private Log log = LogFactory.getLog(PreviewManagerImpl.class);
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	List<PreviewGenerator> generatorList;
	

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

}
