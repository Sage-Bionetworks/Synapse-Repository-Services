package org.sagebionetworks.repo.model.jdo;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_CURRENT_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_ID;
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
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class StackStatusDaoImpl implements StackStatusDao, InitializingBean {
	
	public static final String SQL_GET_STATUS = "SELECT "+COL_STACK_STATUS_STATUS+" FROM "+TABLE_STACK_STATUS+" WHERE "+COL_STACK_STATUS_ID+" = "+DBOStackStatus.STATUS_ID;
	
	public static final String SQL_GET_ALL_STATUS = "SELECT "+COL_STACK_STATUS_STATUS+", "+COL_STACK_STATUS_CURRENT_MESSAGE+", "+COL_STACK_STATUS_PENDING_MESSAGE+" FROM "+TABLE_STACK_STATUS+" WHERE "+COL_STACK_STATUS_ID+" = "+DBOStackStatus.STATUS_ID;
	
	@Autowired
	DBOBasicDao dboBasicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * This should always occur in its own transaction.
	 */
	@NewWriteTransaction
	@Override
	public void updateStatus(StackStatus dto) {
		if(dto == null) throw new IllegalArgumentException("Status cannot be null");
		if(dto.getStatus() == null) throw new IllegalArgumentException("Cannot set the statuss to null");
		try{
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", DBOStackStatus.STATUS_ID);
			DBOStackStatus jdo = dboBasicDao.getObjectByPrimaryKey(DBOStackStatus.class, params);
			jdo.setStatus(dto.getStatus().name());
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
		// Try to get the single status row. If it does not exist yet we will need to create it.
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", DBOStackStatus.STATUS_ID);
		if (!dboBasicDao.getObjectByPrimaryKeyIfExists(DBOStackStatus.class, params).isPresent()) {
			// If here then the the single status row does not exist.
			DBOStackStatus status = new DBOStackStatus();
			status.setId(DBOStackStatus.STATUS_ID);
			status.setStatus(StatusEnum.READ_WRITE.name());
			status.setCurrentMessage(DBOStackStatus.DEFAULT_MESSAGE);
			dboBasicDao.createNew(status);
		}
	}

	@Override
	public StackStatus getFullCurrentStatus() {
		RowMapper<StackStatus> mapper = new RowMapper<StackStatus>() {
			@Override
			public StackStatus mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				StackStatus status = new StackStatus();
				status.setStatus(StatusEnum.valueOf(rs.getString(1)));
				status.setCurrentMessage(rs.getString(2));
				status.setPendingMaintenanceMessage(rs.getString(3));
				return status;
			}
		};
		// Get the data
		return jdbcTemplate.queryForObject(SQL_GET_ALL_STATUS, mapper);
	}

	@Override
	public StatusEnum getCurrentStatus() {
		String statusString = jdbcTemplate.queryForObject(SQL_GET_STATUS, String.class);
		return StatusEnum.valueOf(statusString);
	}

	@Override
	public boolean isStackReadWrite() {
		return getCurrentStatus().equals(StatusEnum.READ_WRITE);
	}

}
