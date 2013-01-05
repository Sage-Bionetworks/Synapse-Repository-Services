package org.sagebionetworks.repo.manager.file.preview;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A simple stub implementation of the FileMetadataDao.
 * 
 * @author jmhill
 *
 */
public class StubFileMetadataDao implements FileMetadataDao {

	Map<String, FileMetadata> map = new HashMap<String, FileMetadata>();

	@Override
	public <T extends FileMetadata> T createFile(T metadata) {
		// Create the metadata
		String id = ""+map.size()+1;
		metadata.setId(id);
		metadata.setCreatedOn(new Date());
		map.put(id, metadata);
		return metadata;
	}

	@Override
	public void setPreviewId(String fileId, String previewId)
			throws DatastoreException, NotFoundException {
		// Get the file form the mad
		S3FileMetadata metadata = (S3FileMetadata) map.get(fileId);
		if(metadata == null) throw new NotFoundException();
		metadata.setPreviewId(previewId);
	}

	@Override
	public FileMetadata get(String id) throws DatastoreException,
			NotFoundException {
		FileMetadata metadata = map.get(id);
		if(metadata == null) throw new NotFoundException();
		return metadata;
	}

	@Override
	public void delete(String id) {
		map.remove(id);
	}

	@Override
	public boolean doesExist(String id) {
		return map.keySet().contains(id);
	}

}
