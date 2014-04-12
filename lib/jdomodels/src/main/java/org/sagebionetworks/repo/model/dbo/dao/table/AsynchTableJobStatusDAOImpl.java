package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.dao.table.AsynchTableJobStatusDAO;
import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AsynchTableJobStatusDAOImpl implements AsynchTableJobStatusDAO {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public AsynchTableJobStatus crateJobStatus(AsynchTableJobStatus status) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsynchTableJobStatus getJobStatus(String jobId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void truncateAllAsynchTableJobStatus() {
		// TODO Auto-generated method stub
		
	}

}
