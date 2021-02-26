package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES_SCANNER_STATUS;

import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class FilesScannerStatusDaoImpl implements FilesScannerStatusDao {
	
	private static final String SQL_CREATE = "INSERT INTO " + TABLE_FILES_SCANNER_STATUS + " ("
			+ COL_FILES_SCANNER_STATUS_ID + ", "
			+ COL_FILES_SCANNER_STATUS_STARTED_ON + ", "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + ", "
			+ COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT + ", "
			+ COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT+ ") VALUES (?, NOW(), NOW(), ?, 0)";
	
	private static final String SQL_GET_BY_ID = "SELECT * FROM " + TABLE_FILES_SCANNER_STATUS + " WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_GET_LATEST = "SELECT * FROM " + TABLE_FILES_SCANNER_STATUS + " ORDER BY " + COL_FILES_SCANNER_STATUS_ID + " DESC LIMIT 1";
	
	private static final String SQL_INCREASE_JOB_COMPLETED_COUNT = "UPDATE " + TABLE_FILES_SCANNER_STATUS + " SET " 
			+ COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT + " = " + COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT + " + 1, "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + " = NOW() WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_TRUNCATE = "TRUNCATE " + TABLE_FILES_SCANNER_STATUS;
		
	private static final RowMapper<DBOFilesScannerStatus> ROW_MAPPER = DBOFilesScannerStatus.TABLE_MAPPING; 

	private IdGenerator idGenerator;

	private NamedParameterJdbcTemplate jdbcTemplate;
		
	@Autowired
	public FilesScannerStatusDaoImpl(IdGenerator idGenerator, NamedParameterJdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public DBOFilesScannerStatus create(long jobsCount) {
		Long id = idGenerator.generateNewId(IdType.FILES_SCANNER_STATUS_ID);
		
		jdbcTemplate.getJdbcTemplate().update(SQL_CREATE, id, jobsCount);
	
		return get(id);
	}
	
	@Override
	public DBOFilesScannerStatus get(long id) {
		try {
			return jdbcTemplate.getJdbcTemplate().queryForObject(SQL_GET_BY_ID, ROW_MAPPER, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
	}

	@Override
	public Optional<DBOFilesScannerStatus> getLatest() {
		try {
			return Optional.of(jdbcTemplate.getJdbcTemplate().queryForObject(SQL_GET_LATEST, null, ROW_MAPPER));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	@Override
	@WriteTransaction
	public DBOFilesScannerStatus increaseJobCompletedCount(long id) {
		int count = jdbcTemplate.getJdbcTemplate().update(SQL_INCREASE_JOB_COMPLETED_COUNT, id);
		
		if (count < 1) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
		
		return get(id);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.getJdbcTemplate().update(SQL_TRUNCATE);
	}
	
}
