package org.sagebionetworks.repo.model.dbo.dao;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileMetadata;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
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
public class DBOFileMetadataDao implements FileMetadataDao {
	
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
		DBOFileMetadata dbo = basicDao.getObjectById(DBOFileMetadata.class, param);
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
		basicDao.deleteObjectById(DBOFileMetadata.class, param);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends FileHandle> T createFile(T metadata){
		if(metadata == null) throw new IllegalArgumentException("FileMetadata cannot be null");
		// Convert to a DBO
		DBOFileMetadata dbo = FileMetadataUtils.createDBOFromDTO(metadata);
		if(metadata.getId() == null){
			dbo.setId(idGenerator.generateNewId(TYPE.FILE_ID));
		}else{
			// If an id was provided then it must not exist
			if(doesExist(metadata.getId())) throw new IllegalArgumentException("A file object already exists with ID: "+metadata.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(new Long(metadata.getId()), TYPE.FILE_ID);
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
