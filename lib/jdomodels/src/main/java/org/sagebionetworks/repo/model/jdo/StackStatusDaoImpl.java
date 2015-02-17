package org.sagebionetworks.repo.model.jdo;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_CURRENT_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_PENDING_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STACK_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStackStatus;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class StackStatusDaoImpl implements StackStatusDao, InitializingBean {
	
	public static final String SQL_GET_STATUS = "SELECT "+COL_STACK_STATUS_STATUS+" FROM "+TABLE_STACK_STATUS+" WHERE "+COL_NODE_ID+" = "+DBOStackStatus.STATUS_ID;
	
	public static final String SQL_GET_ALL_STATUS = "SELECT "+COL_STACK_STATUS_STATUS+", "+COL_STACK_STATUS_CURRENT_MESSAGE+", "+COL_STACK_STATUS_PENDING_MESSAGE+" FROM "+TABLE_STACK_STATUS+" WHERE "+COL_NODE_ID+" = "+DBOStackStatus.STATUS_ID;
	
	@Autowired
	DBOBasicDao dboBasicDao;	
	// This is better suited for simple JDBC query.
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	/**
	 * This should always occur in its own transaction.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public void updateStatus(StackStatus dto) {
		if(dto == null) throw new IllegalArgumentException("Status cannot be null");
		if(dto.getStatus() == null) throw new IllegalArgumentException("Cannot set the statu to null");
		try{
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", DBOStackStatus.STATUS_ID);
			DBOStackStatus jdo = dboBasicDao.getObjectByPrimaryKey(DBOStackStatus.class, params);
			jdo.setStatus(getStatusCode(dto.getStatus()));
			jdo.setCurrentMessage(dto.getCurrentMessage());
			jdo.setPendingMessage(dto.getPendingMaintenanceMessage());
			dboBasicDao.update(jdo);
		}catch(NotFoundException e){
			throw new RuntimeException("Failed to get the current status!!!!",e);
		} catch (DatastoreException e) {
			throw new RuntimeException("Failed to get the current status!!!!",e);
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// This is the boot strap.
		try{
			// Try to get the single status row.  If it does not exist yet we will need to create it.
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", DBOStackStatus.STATUS_ID);
			DBOStackStatus jdo = dboBasicDao.getObjectByPrimaryKey(DBOStackStatus.class, params);
		}catch (NotFoundException e){
			// If here then the the single status row does not exist.
			DBOStackStatus status = new DBOStackStatus();
			status.setId(DBOStackStatus.STATUS_ID);
			status.setStatus(getStatusCode(StatusEnum.READ_WRITE));
			status.setCurrentMessage(DBOStackStatus.DEFAULT_MESSAGE);
			dboBasicDao.createNew(status);
		}
	}
	
	/**
	 * Get the id used for this enumeration.
	 * @param enumValue
	 * @return
	 */
	private static short getStatusCode(StatusEnum enumValue){
		for(short i=0; i< StatusEnum.values().length; i++){
			if( StatusEnum.values()[i] == enumValue){
				return i;
			}
		}
		throw new IllegalArgumentException("StatusEnum cannot be null");
	}

	@Override
	public StackStatus getFullCurrentStatus() {
		RowMapper<StackStatus> mapper = new RowMapper<StackStatus>() {
			@Override
			public StackStatus mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				StackStatus status = new StackStatus();
				int index = rs.getInt(1);
				status.setStatus(StatusEnum.values()[index]);
				status.setCurrentMessage(rs.getString(2));
				status.setPendingMaintenanceMessage(rs.getString(3));
				return status;
			}
		};
		// Get the data
		return simpleJdbcTemplate.queryForObject(SQL_GET_ALL_STATUS, mapper);
	}

	@Override
	public StatusEnum getCurrentStatus() {
		int index = simpleJdbcTemplate.queryForInt(SQL_GET_STATUS);
		return StatusEnum.values()[index];
	}

}
