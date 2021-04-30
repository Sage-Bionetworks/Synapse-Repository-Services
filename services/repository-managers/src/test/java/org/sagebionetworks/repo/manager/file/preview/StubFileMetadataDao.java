package org.sagebionetworks.repo.manager.file.preview;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dao.FileHandleStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Maps;

/**
 * A simple stub implementation of the FileMetadataDao.
 * 
 * @author jmhill
 *
 */
public class StubFileMetadataDao implements FileHandleDao {

	Map<String, FileHandle> map = new HashMap<String, FileHandle>();
	Map<String, FileHandleBackup> backupMap = new HashMap<String, FileHandleBackup>();

	@Override
	public void setPreviewId(String fileId, String previewId)
			throws DatastoreException, NotFoundException {
		// Get the file form the mad
		CloudProviderFileHandleInterface metadata = (CloudProviderFileHandleInterface) map.get(fileId);
		if(metadata == null) throw new NotFoundException();
		metadata.setPreviewId(previewId);
	}

	@Override
	public FileHandle get(String id) throws DatastoreException,
			NotFoundException {
		FileHandle metadata = map.get(id);
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

	@Override
	public String getHandleCreator(String fileHandleId)
			throws NotFoundException {
		return map.get(fileHandleId).getCreatedBy();
	}

	@Override
	public String getPreviewFileHandleId(String handleId)
			throws NotFoundException {
 		return ((CloudProviderFileHandleInterface)map.get(handleId)).getPreviewId();
	}

	@Override
	public FileHandleResults getAllFileHandles(Iterable<String> ids,
			boolean includePreviews) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public long getNumberOfReferencesToFile(FileHandleMetadataType metadataType, String bucketName, String key) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getCount() throws DatastoreException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public long getMaxId() throws DatastoreException {
		return map.size();
	}

	@Override
	public Map<String, FileHandle> getAllFileHandlesBatch(Iterable<String> fileHandleIds) {
		Map<String, FileHandle> result = Maps.newHashMap();
		for (String fileHandleId : fileHandleIds) {
			result.put(fileHandleId, map.get(fileHandleId));
		}
		return result;
	}

	@Override
	public Set<String> getFileHandleIdsCreatedByUser(Long createdById,
			List<String> fileHandleIds) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getFileHandlePreviewIds(List<String> fileHandlePreviewIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createBatch(List<FileHandle> toCreate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileHandle createFile(FileHandle metadata) {
		// Create the metadata
		String id = ""+map.size()+1;
		metadata.setId(id);
		metadata.setCreatedOn(new Date());
		map.put(id, metadata);
		return metadata;
	}
	
	@Override
	public boolean isMatchingMD5(String sourceFileHandleId, String targetFileHandleId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void truncateTable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateBatchStatus(List<Long> ids, FileHandleStatus newStatus, FileHandleStatus currentStatus) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<FileHandle> getFileHandlesBatchByStatus(List<Long> ids, FileHandleStatus status) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<DBOFileHandle> getDBOFileHandlesBatch(List<Long> ids) {
		// TODO Auto-generated method stub
		return null;
	}

}
