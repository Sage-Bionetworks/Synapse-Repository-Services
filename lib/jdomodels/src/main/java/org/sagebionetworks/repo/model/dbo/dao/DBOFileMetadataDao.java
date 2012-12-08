package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	/**
	 * Used to detect if a file object already exists.
	 */
	private static final String SQL_DOES_EXIST = "SELECT "+COL_FILES_ID+" FROM "+TABLE_FILES+" WHERE "+COL_FILES_ID+" = ?";

	@Autowired
	private IdGenerator idGenerator;
		
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Override
	public FileMetadata get(Long id) throws DatastoreException, NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		DBOFileMetadata dbo = basicDao.getObjectById(DBOFileMetadata.class, param);
		return FileMetadataUtils.createDTOFromDBO(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(Long id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		// Delete this object
		basicDao.deleteObjectById(DBOFileMetadata.class, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long create(FileMetadata metadata) {
		// Convert to a DBO
		DBOFileMetadata dbo = FileMetadataUtils.createDBOFromDTO(metadata);
		if(metadata.getId() == null){
			dbo.setId(idGenerator.generateNewId(TYPE.FILE_ID));
		}else{
			// If an id was provided then it must not exist
			if(doesExist(metadata.getId())) throw new IllegalArgumentException("A file object already exists with ID: "+metadata.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(metadata.getId(), TYPE.FILE_ID);
		}
		// Save it to the DB
		dbo = basicDao.createNew(dbo);
		// Return the ID of the new object.
		return dbo.getId();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setPreviewId(Long fileId, Long previewId) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Does the given file object exist?
	 * @param id
	 * @return
	 */
	public boolean doesExist(Long id){
		try{
			// Is this in the database.
			simpleJdbcTemplate.queryForLong(SQL_DOES_EXIST, id);
			return true;
		}catch(EmptyResultDataAccessException e){
			return false;
		}

	}
}
