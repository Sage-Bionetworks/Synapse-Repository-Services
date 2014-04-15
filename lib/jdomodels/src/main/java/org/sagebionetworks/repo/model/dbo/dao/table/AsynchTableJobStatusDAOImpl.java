package org.sagebionetworks.repo.model.dbo.dao.table;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.AsynchTableJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.table.AsynchTableJobStatusUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOAsynchTableJobStatus;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOAsynchTableJobStatus.JobState;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOAsynchTableJobStatus.JobType;
import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AsynchTableJobStatusDAOImpl implements AsynchTableJobStatusDAO {
	
	private static final String SQL_UPDATE_PROGRESS = "UPDATE "+ASYNCH_TABLE_JOB_STATUS+" SET "+COL_ASYNCH_TABLE_JOB_PROGRESS_CURRENT+" = ?, "+COL_ASYNCH_TABLE_JOB_PROGRESS_TOTAL+" = ?, "+COL_ASYNCH_TABLE_JOB_PROGRESS_MESSAGE+" = ?, "+COL_ASYNCH_TABLE_JOB_ETAG+" = ? WHERE "+COL_ASYNCH_TABLE_JOB_ID+" = ?";

	private static final String TRUNCATE_ALL = "DELETE FROM "+ASYNCH_TABLE_JOB_STATUS+" WHERE "+COL_ASYNCH_TABLE_JOB_ID+" > -1";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;


	@Override
	public AsynchTableJobStatus getJobStatus(String jobId) throws DatastoreException, NotFoundException {
		DBOAsynchTableJobStatus dbo =  basicDao.getObjectByPrimaryKey(DBOAsynchTableJobStatus.class, new SinglePrimaryKeySqlParameterSource(jobId));
		return AsynchTableJobStatusUtils.createDTOFromDBO(dbo);
	}

	@Override
	public void truncateAllAsynchTableJobStatus() {
		jdbcTemplate.update(TRUNCATE_ALL);
	}

	@Override
	public AsynchTableJobStatus starteNewUploadJobStatus(Long userId, Long fileHandleId, String tableId) {
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(fileHandleId == null) throw new IllegalArgumentException("FileHandleId cannot be null");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		// Issue a new ID
		long now = System.currentTimeMillis();
		DBOAsynchTableJobStatus dbo = new DBOAsynchTableJobStatus();
		dbo.setJobId(idGenerator.generateNewId(TYPE.ASYNCH_TABLE_JOB_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setChangedOn(now);
		dbo.setStartedByUserId(userId);
		dbo.setStartedOn(now);
		dbo.setJobState(JobState.PROCESSING);
		dbo.setUploadFileHandleId(fileHandleId);
		dbo.setJobType(JobType.UPLOAD);
		dbo.setTableId(tableId);
		// Create the row
		dbo = basicDao.createNew(dbo);
		return AsynchTableJobStatusUtils.createDTOFromDBO(dbo);
	}

	@Override
	public String updateProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage) {
		String newEtag = UUID.randomUUID().toString();
		progressMessage = AsynchTableJobStatusUtils.truncateMessageStringIfNeeded(progressMessage);
		jdbcTemplate.update(SQL_UPDATE_PROGRESS, progressCurrent, progressTotal, progressMessage, newEtag, jobId);
		return newEtag;
	}

}
