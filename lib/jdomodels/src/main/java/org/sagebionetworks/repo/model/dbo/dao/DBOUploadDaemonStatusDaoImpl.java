package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Simple implementation of the UploadDaemonStatusDao.
 * @author John
 *
 */
public class DBOUploadDaemonStatusDaoImpl implements UploadDaemonStatusDao {
	
	@Autowired
	private DBOBasicDao basicDao;

	@WriteTransaction
	@Override
	public UploadDaemonStatus create(UploadDaemonStatus status) throws DatastoreException {
		validate(status);
		// Convert to a DBO
		DBOUploadDaemonStatus dbo = UploadDaemonStatusUtils.createDBOFromDTO(status);
		if(dbo.getRunTimeMS() == null){
			dbo.setRunTimeMS(0l);
		}
		if(dbo.getStartedOn() == null){
			dbo.setStartedOn(System.currentTimeMillis());
		}
		if(dbo.getPercentComplete() == null){
			dbo.setPercentComplete(0.0);
		}
		dbo = basicDao.createNew(dbo);
		try {
			return get(""+dbo.getId());
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public UploadDaemonStatus get(String id) throws DatastoreException, NotFoundException {
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", id);
		DBOUploadDaemonStatus dbo = basicDao.getObjectByPrimaryKey(DBOUploadDaemonStatus.class, param);
		return UploadDaemonStatusUtils.createDTOFromDBO(dbo);
	}

	@WriteTransaction
	@Override
	public void delete(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", id);
		basicDao.deleteObjectByPrimaryKey(DBOUploadDaemonStatus.class, param);
	}

	@NewWriteTransaction
	@Override
	public boolean update(UploadDaemonStatus status) {
		validate(status);
		if(status.getStartedOn() == null) throw new IllegalArgumentException("UploadDaemonStatus.startedOn cannot be null");
		// Convert to a DBO
		DBOUploadDaemonStatus dbo = UploadDaemonStatusUtils.createDBOFromDTO(status);
		// Calculate the elapse
		long runtime = System.currentTimeMillis() - dbo.getStartedOn();
		dbo.setRunTimeMS(runtime);
		return basicDao.update(dbo);
	}

	private void validate(UploadDaemonStatus status){
		if(status == null) throw new IllegalArgumentException("UploadDaemonStatus cannot be null");
		if(status.getState() == null) throw new IllegalArgumentException("UploadDaemonStatus.state cannot be null");
		if(status.getStartedBy() == null) throw new IllegalArgumentException("UploadDaemonStatus.startedBy cannot be null");
	}

}
