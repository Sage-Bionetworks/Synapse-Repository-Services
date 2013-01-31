package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_PREVIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
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
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		DBOFileHandle dbo = basicDao.getObjectById(DBOFileHandle.class, param);
		return FileMetadataUtils.createDTOFromDBO(dbo);
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
		basicDao.deleteObjectById(DBOFileHandle.class, param);
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
		if(previewId == null) throw new IllegalArgumentException("PreviewId cannot be null");
		if(!doesExist(fileId)){
			throw new NotFoundException("The fileId: "+fileId+" does not exist");
		}
		if(!doesExist(previewId)){
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
}
