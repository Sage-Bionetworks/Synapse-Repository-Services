package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES_SCANNER_STATUS;

import java.sql.ResultSet;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.files.FilesScannerState;
import org.sagebionetworks.repo.model.files.FilesScannerStatus;
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
			+ COL_FILES_SCANNER_STATUS_ETAG + ", "
			+ COL_FILES_SCANNER_STATUS_STARTED_ON + ", "
			+ COL_FILES_SCANNER_STATUS_STATE + ", "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + ", "
			+ COL_FILES_SCANNER_STATUS_JOBS_COUNT + ") VALUES (?, UUID(), NOW(), '" + FilesScannerState.PROCESSING.name() + "', NOW(), ?)";
	
	private static final String SQL_GET_BY_ID = "SELECT * FROM " + TABLE_FILES_SCANNER_STATUS + " WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_UPDATE_STATE = "UPDATE " 
			+ TABLE_FILES_SCANNER_STATUS + " SET " 
			+ COL_FILES_SCANNER_STATUS_STATE + " = ?, " 
			+ COL_FILES_SCANNER_STATUS_ETAG + " = UUID(), "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + " = NOW() "
			+ "WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_GET_LATEST = "SELECT * FROM " + TABLE_FILES_SCANNER_STATUS + " ORDER BY " + COL_FILES_SCANNER_STATUS_ID + " DESC LIMIT 1";
	
	private static final String SQL_TRUNCATE = "TRUNCATE " + TABLE_FILES_SCANNER_STATUS;
	
	private static final RowMapper<FilesScannerStatus> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		FilesScannerStatus status = new FilesScannerStatus();
		
		status.setId(rs.getLong(COL_FILES_SCANNER_STATUS_ID));
		status.setStartedOn(rs.getTimestamp(COL_FILES_SCANNER_STATUS_STARTED_ON).toInstant());
		status.setUpdatedOn(rs.getTimestamp(COL_FILES_SCANNER_STATUS_UPDATED_ON).toInstant());
		status.setState(FilesScannerState.valueOf(rs.getString(COL_FILES_SCANNER_STATUS_STATE)));
		status.setJobsCount(rs.getLong(COL_FILES_SCANNER_STATUS_JOBS_COUNT));
		
		return status;
	};

	private IdGenerator idGenerator;

	private NamedParameterJdbcTemplate jdbcTemplate;
		
	@Autowired
	public FilesScannerStatusDaoImpl(IdGenerator idGenerator, NamedParameterJdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public FilesScannerStatus create(long jobsCount) {
		Long id = idGenerator.generateNewId(IdType.FILES_SCANNER_STATUS_ID);
		
		jdbcTemplate.getJdbcTemplate().update(SQL_CREATE, id, jobsCount);
	
		return get(id);
	}
	
	@Override
	public FilesScannerStatus get(long id) {
		try {
			return jdbcTemplate.getJdbcTemplate().queryForObject(SQL_GET_BY_ID, ROW_MAPPER, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
	}

	@Override
	public Optional<FilesScannerStatus> getLatest() {
		try {
			return Optional.of(jdbcTemplate.getJdbcTemplate().queryForObject(SQL_GET_LATEST, null, ROW_MAPPER));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	@WriteTransaction
	public FilesScannerStatus setState(long id, FilesScannerState state) {
		int updated = jdbcTemplate.getJdbcTemplate().update(SQL_UPDATE_STATE, state.name(), id);
		
		if (updated < 1) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
		
		return get(id);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.getJdbcTemplate().update(SQL_TRUNCATE);
	}
	
}
