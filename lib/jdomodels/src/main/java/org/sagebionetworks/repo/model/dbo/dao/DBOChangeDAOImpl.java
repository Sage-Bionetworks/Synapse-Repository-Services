package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOChangeDAOImpl implements DBOChangeDAO {
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public DBOChange replaceChange(DBOChange change) {
		if(change == null) throw new IllegalArgumentException("DBOChange cannot be null");
		if(change.getObjectId() == null) throw new IllegalArgumentException("change.getObjectId() cannot be null");
		// First delete the change.
		deleteChange(change.getObjectId());
		// Clear the time stamp so that we get a new one automatically.
		change.setTimeStamp(null);
		change.setChangeNumber(null);
		// Now insert the row with the current value
		return basicDao.createNew(change);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteChange(Long objectId) {
		if(objectId == null) throw new IllegalArgumentException("ObjectId cannot be null");
		// First remove the row for this object.
		simpleJdbcTemplate.update("DELETE FROM "+TABLE_CHANGES+" WHERE "+COL_CHANGES_OBJECT_ID+" = ?", objectId);
	}

	@Override
	public long getCurrentChangeNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

}
