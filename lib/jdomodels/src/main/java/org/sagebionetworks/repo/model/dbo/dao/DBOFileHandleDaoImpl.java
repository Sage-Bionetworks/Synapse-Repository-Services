package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_MD5;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_PREVIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.HasPreviewId;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic JDBC implementation of the FileMetadataDao.
 * 
 * @author John
 *
 */
public class DBOFileHandleDaoImpl implements FileHandleDao {
	
	private static final String SQL_GET_MIGRATION_OBJECT_DATA_PAGE = "SELECT "+COL_FILES_ID+", "+COL_FILES_ETAG+", "+COL_FILES_PREVIEW_ID+" FROM "+TABLE_FILES+" ORDER BY "+COL_FILES_ID+" DESC LIMIT ? OFFSET ?";
	private static final String SQL_COUNT_ALL_FILES = "SELECT COUNT(*) FROM "+TABLE_FILES;
	private static final String SQL_MAX_FILE_ID = "SELECT MAX(ID) FROM " + TABLE_FILES;
	private static final String SQL_SELECT_CREATOR = "SELECT "+COL_FILES_CREATED_BY+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";
	private static final String SQL_SELECT_PREVIEW_ID = "SELECT "+COL_FILES_PREVIEW_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";
	private static final String UPDATE_PREVIEW_AND_ETAG = "UPDATE "+TABLE_FILES+" SET "+COL_FILES_PREVIEW_ID+" = ? ,"+COL_FILES_ETAG+" = ? WHERE "+COL_FILES_ID+" = ?";

	/**
	 * Used to detect if a file object already exists.
	 */
	private static final String SQL_DOES_EXIST = "SELECT "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TagMessenger tagMessenger;
		
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Override
	public FileHandle get(String id) throws DatastoreException, NotFoundException {
		DBOFileHandle dbo = getDBO(id);
		return FileMetadataUtils.createDTOFromDBO(dbo);
	}

	private DBOFileHandle getDBO(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		DBOFileHandle dbo = basicDao.getObjectByPrimaryKey(DBOFileHandle.class, param);
		return dbo;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		// Send the delete message
		tagMessenger.sendDeleteMessage(id, ObjectType.FILE);
		// Delete this object
		try{
			basicDao.deleteObjectByPrimaryKey(DBOFileHandle.class, param);
		}catch (DataIntegrityViolationException e){
			// This occurs when we try to delete a handle that is in use.
			new DataIntegrityViolationException("Cannot delete a file handle that has been assigned to an owner object. FileHandle id: "+id);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends FileHandle> T createFile(T fileHandle) {
		if(fileHandle == null) throw new IllegalArgumentException("fileHandle cannot be null");
		if(fileHandle.getFileName() == null) throw new IllegalArgumentException("fileHandle.getFileName cannot be null");
		// Convert to a DBO
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(fileHandle);
		if(fileHandle.getId() == null){
			dbo.setId(idGenerator.generateNewId(TYPE.FILE_IDS));
		}else{
			// If an id was provided then it must not exist
			if(doesExist(fileHandle.getId())) throw new IllegalArgumentException("A file object already exists with ID: "+fileHandle.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(new Long(fileHandle.getId()), TYPE.FILE_IDS);
		}
		// When we migrate we keep the original etag.  When it is null we set it.
		if(dbo.getEtag() == null){
			dbo.setEtag(UUID.randomUUID().toString());
		}
		// Save it to the DB
		dbo = basicDao.createNew(dbo);
		// Send the create message
		tagMessenger.sendMessage(dbo.getId().toString(), dbo.getEtag(), ObjectType.FILE, ChangeType.CREATE);
		try {
			return (T) get(dbo.getId().toString());
		} catch (NotFoundException e) {
			// This should not occur.
			throw new RuntimeException(e);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setPreviewId(String fileId, String previewId) throws NotFoundException {
		if(fileId == null) throw new IllegalArgumentException("FileId cannot be null");
		if(!doesExist(fileId)){
			throw new NotFoundException("The fileId: "+fileId+" does not exist");
		}
		//if preview ID is set, then it must exist to continue update
		if(previewId != null && !doesExist(previewId)){
			throw new NotFoundException("The previewId: "+previewId+" does not exist");
		}
		try{
			// Change the etag
			String newEtag = UUID.randomUUID().toString();
			simpleJdbcTemplate.update(UPDATE_PREVIEW_AND_ETAG, previewId, newEtag, fileId);
			// Send the update message
			tagMessenger.sendMessage(fileId, newEtag, ObjectType.FILE, ChangeType.UPDATE);
		} catch (DataIntegrityViolationException e){
			throw new NotFoundException(e.getMessage());
		}
	}

	/**
	 * Does the given file object exist?
	 * @param id
	 * @return
	 */
	public boolean doesExist(String id){
		if(id == null) throw new IllegalArgumentException("FileId cannot be null");
		try{
			// Is this in the database.
			simpleJdbcTemplate.queryForLong(SQL_DOES_EXIST, id);
			return true;
		}catch(EmptyResultDataAccessException e){
			return false;
		}

	}

	@Override
	public String getHandleCreator(String fileHandleId) throws NotFoundException {
		if(fileHandleId == null) throw new IllegalArgumentException("fileHandleId cannot be null");
		try{
			// Lookup the creator.
			Long creator = simpleJdbcTemplate.queryForLong(SQL_SELECT_CREATOR, Long.parseLong(fileHandleId));
			return creator.toString();
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("The FileHandle does not exist: "+fileHandleId);
		}
	}

	@Override
	public String getPreviewFileHandleId(String fileHandleId)
			throws NotFoundException {
		if(fileHandleId == null) throw new IllegalArgumentException("fileHandleId cannot be null");
		try{
			// Lookup the creator.
			long previewId = simpleJdbcTemplate.queryForLong(SQL_SELECT_PREVIEW_ID, Long.parseLong(fileHandleId));
			if(previewId > 0){
				return Long.toString(previewId);
			}else{
				throw new NotFoundException("A preview does not exist for: "+fileHandleId);
			}
		}catch(EmptyResultDataAccessException e){
			// This occurs when the file handle does not exist
			throw new NotFoundException("The FileHandle does not exist: "+fileHandleId);
		}
	}

	@Override
	public FileHandleResults getAllFileHandles(List<String> ids, boolean includePreviews) throws DatastoreException, NotFoundException {
		List<FileHandle> handles = new LinkedList<FileHandle>();
		if(ids != null){
			for(String handleId: ids){
				// Look up each handle
				FileHandle handle = get(handleId);
				handles.add(handle);
				// If this handle has a preview then we fetch that as well.
				if(includePreviews && handle instanceof HasPreviewId){
					String previewId = ((HasPreviewId)handle).getPreviewId();
					if(previewId != null){
						FileHandle preview = get(previewId);
						handles.add(preview);
					}
				}
			}
		}
		FileHandleResults results = new FileHandleResults();
		results.setList(handles);
		return results;
	}

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_ALL_FILES);
	}
	
	@Override
	public long getMaxId() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SQL_MAX_FILE_ID);
	}

	@Override
	public FileHandleBackup getFileHandleBackup(String id) throws NotFoundException {
		DBOFileHandle dbo = getDBO(id);
		return FileMetadataUtils.createBackupFromDBO(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean createOrUpdateFromBackup(FileHandleBackup backup) {
		if(backup == null) throw new IllegalArgumentException("Backup cannot be null");
		// Convert to a DBO
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromBackup(backup);
		ChangeType changeType = null;
		// Does this already exist?
		if(doesExist(dbo.getId().toString())){
			basicDao.update(dbo);
			changeType = ChangeType.UPDATE;
		}else{
			basicDao.createNew(dbo);
			changeType = ChangeType.CREATE;
		}
		// Send a message
		tagMessenger.sendMessage(dbo.getId().toString(), dbo.getEtag(), ObjectType.FILE, changeType);
		return changeType == ChangeType.CREATE;
	}

	@Override
	public List<String> findFileHandleWithKeyAndMD5(String key, String md5) {
		return simpleJdbcTemplate.query("SELECT "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE `"+COL_FILES_KEY+"` = ? AND "+COL_FILES_CONTENT_MD5+" = ?", new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return ""+rs.getLong(COL_FILES_ID);
			}
		}, key, md5);
	}
	
}
