package org.sagebionetworks.evaluation.dao;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionFileHandleDBO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.query.SQLConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SubmissionFileHandleDAOImpl implements SubmissionFileHandleDAO {
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	private static final String SELECT_ALL = "SELECT *";
	
	private static final String BY_SUBMISSION_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBFILE +
			" WHERE "+ SQLConstants.COL_SUBFILE_SUBMISSION_ID + "=:"+ DBOConstants.PARAM_SUBFILE_SUBMISSION_ID;
	
	private static final String SELECT_BY_SUBMISSION_SQL = 
			SELECT_ALL + BY_SUBMISSION_SQL;
	
	private static final RowMapper<SubmissionFileHandleDBO> rowMapper = ((new SubmissionFileHandleDBO()).getTableMapping());

	@Override
	@WriteTransaction
	public void create(String submissionId, String fileHandleId) {
		ValidateArgument.required(submissionId, "Submission ID");
		ValidateArgument.required(fileHandleId, "FileHandle ID");

		// Convert to DBO
		SubmissionFileHandleDBO dbo = new SubmissionFileHandleDBO();
		dbo.setSubmissionId(Long.parseLong(submissionId));
		dbo.setFileHandleId(Long.parseLong(fileHandleId));
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException(e.getMessage() + dbo.toString());
		}
	}
	
	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return basicDao.getCount(SubmissionFileHandleDBO.class);
	}
	
	@Override
	public List<String> getAllBySubmission(String submissionId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_SUBFILE_SUBMISSION_ID, submissionId);		
		List<SubmissionFileHandleDBO> dbos = namedJdbcTemplate.query(SELECT_BY_SUBMISSION_SQL, param, rowMapper);
		List<String> fileHandleIds = new ArrayList<String>(dbos.size());
		for (SubmissionFileHandleDBO dbo : dbos) {
			fileHandleIds.add(dbo.getFileHandleId().toString());
		}
		return fileHandleIds;
	}
	
	@Override
	@WriteTransaction
	public void delete(String submissionId, String fileHandleId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_SUBFILE_SUBMISSION_ID, submissionId);
		param.addValue(DBOConstants.PARAM_SUBFILE_FILE_HANDLE_ID, fileHandleId);		
		basicDao.deleteObjectByPrimaryKey(SubmissionFileHandleDBO.class, param);		
	}	
}
