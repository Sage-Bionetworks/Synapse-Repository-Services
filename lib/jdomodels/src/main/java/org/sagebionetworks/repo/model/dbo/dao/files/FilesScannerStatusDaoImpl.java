package org.sagebionetworks.repo.model.dbo.dao.files;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;


@Repository
public class FilesScannerStatusDaoImpl implements FilesScannerStatusDao {
	
	private static final String SQL_CREATE = "INSERT INTO " + TABLE_FILES_SCANNER_STATUS + " ("
			+ COL_FILES_SCANNER_STATUS_ID + ", "
			+ COL_FILES_SCANNER_STATUS_STARTED_ON + ", "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + ", "
			+ COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT + ", "
			+ COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT + ", "
			+ COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT + ") VALUES (?, NOW(), NOW(), 0, 0, 0)";
	
	private static final String SQL_GET_BY_ID = "SELECT * FROM " + TABLE_FILES_SCANNER_STATUS + " WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_DELETE_BY_ID = "DELETE FROM " +TABLE_FILES_SCANNER_STATUS + " WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_EXISTS = "SELECT EXISTS(SELECT " + COL_FILES_SCANNER_STATUS_ID + " FROM " + TABLE_FILES_SCANNER_STATUS + " WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?)";
	
	private static final String SQL_GET_LATEST = "SELECT * FROM " + TABLE_FILES_SCANNER_STATUS + " ORDER BY " + COL_FILES_SCANNER_STATUS_ID + " DESC LIMIT 1";
	
	private static final String SQL_EXISTS_BY_LAST_UPDATED_ON = "SELECT EXISTS(SELECT " + COL_FILES_SCANNER_STATUS_ID + " FROM " + TABLE_FILES_SCANNER_STATUS 
			+ " WHERE " + COL_FILES_SCANNER_STATUS_UPDATED_ON + " > (NOW() - INTERVAL ? DAY))";
	
	private static final String SQL_INCREASE_JOB_COMPLETED_COUNT = "UPDATE " + TABLE_FILES_SCANNER_STATUS + " SET " 
			+ COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT + " = " + COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT + " + 1, "
			+ COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT + " = " + COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT + " + ?,"
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + " = NOW() WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_SET_JOB_STARTED_COUNT = "UPDATE " + TABLE_FILES_SCANNER_STATUS + " SET "
			+ COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT + " = ?, "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + " = NOW() WHERE " + COL_FILES_SCANNER_STATUS_ID + " = ?";
	
	private static final String SQL_SET_UPDATED_ON = "UPDATE " + TABLE_FILES_SCANNER_STATUS + " SET "
			+ COL_FILES_SCANNER_STATUS_UPDATED_ON + " = (" + COL_FILES_SCANNER_STATUS_UPDATED_ON + " - INTERVAL ? DAY)";
	
	private static final String SQL_TRUNCATE = "TRUNCATE " + TABLE_FILES_SCANNER_STATUS;
		
	private static final RowMapper<DBOFilesScannerStatus> ROW_MAPPER = DBOFilesScannerStatus.TABLE_MAPPING; 

	private IdGenerator idGenerator;

	private JdbcTemplate jdbcTemplate;
		
	@Autowired
	public FilesScannerStatusDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public DBOFilesScannerStatus create() {
		Long id = idGenerator.generateNewId(IdType.FILES_SCANNER_STATUS_ID);
		
		jdbcTemplate.update(SQL_CREATE, id);
	
		return get(id);
	}
	
	@Override
	public boolean exist(long id) {
		return jdbcTemplate.queryForObject(SQL_EXISTS, boolean.class, id);
	}
	
	@Override
	public DBOFilesScannerStatus get(long id) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_BY_ID, ROW_MAPPER, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
	}
	
	@Override
	@WriteTransaction
	public void delete(long id) {
		jdbcTemplate.update(SQL_DELETE_BY_ID, id);
	}
	
	@Override
	@WriteTransaction
	public DBOFilesScannerStatus increaseJobCompletedCount(long id, int scannedAssociations) {
		int updatedCount = jdbcTemplate.update(SQL_INCREASE_JOB_COMPLETED_COUNT, scannedAssociations, id);
		
		if (updatedCount < 1) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
		
		return get(id);
	}
	
	@Override
	@WriteTransaction
	public DBOFilesScannerStatus setStartedJobsCount(long id, long jobsCount) {
		int updatedCount = jdbcTemplate.update(SQL_SET_JOB_STARTED_COUNT, jobsCount, id);
		
		if (updatedCount < 1) {
			throw new NotFoundException("Could not find a job with id " + id);
		}
		
		return get(id);
	}
	
	@Override
	public Optional<DBOFilesScannerStatus> getLatest() {
		try {
			return Optional.of(jdbcTemplate.queryForObject(SQL_GET_LATEST, null, ROW_MAPPER));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public boolean existsWithinLast(int lastModifiedDaysInterval) {
		return jdbcTemplate.queryForObject(SQL_EXISTS_BY_LAST_UPDATED_ON, boolean.class, lastModifiedDaysInterval);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE);
	}

	@Override
	public void reduceUpdatedOnOfNumberOfDays(long id, int daysInThePast) {
		jdbcTemplate.update(SQL_SET_UPDATED_ON, daysInThePast);
		
	}
	
}
