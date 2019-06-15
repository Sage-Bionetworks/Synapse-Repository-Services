package org.sagebionetworks.repo.model.message;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BROADCAST_MESSAGE_CHANGE_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_BROADCAST_MESSAGE;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class BroadcastMessageDaoImpl implements BroadcastMessageDao {
	
	public static final String BROAD_CHANGE_NUM_FK = "BROAD_CHANGE_NUM_FK";
	public static final String BROAD_MESSAGE_ID_FK = "BROAD_MESSAGE_ID_FK";
	
	private static final String SQL_COUNT_CHANGE_NUMBER = "SELECT COUNT("+COL_BROADCAST_MESSAGE_CHANGE_NUMBER+") FROM "+TABLE_BROADCAST_MESSAGE+" WHERE "+COL_BROADCAST_MESSAGE_CHANGE_NUMBER+" = ?";
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean wasBroadcast(Long changeNumber) {
		long count = jdbcTemplate.queryForObject(SQL_COUNT_CHANGE_NUMBER, Long.class, changeNumber);
		return count == 1;
	}

	@WriteTransaction
	@Override
	public void setBroadcast(Long changeNumber) {
		ValidateArgument.required(changeNumber, "changeNumber");
		long now = System.currentTimeMillis();
		DBOBroadcastMessage dbo = new DBOBroadcastMessage();
		dbo.setChangeNumber(changeNumber);
		dbo.setSentOn(now);
		try {
			basicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			if(e.getMessage().contains(BROAD_CHANGE_NUM_FK)){
				throw new NotFoundException("Change number does not exist: "+changeNumber);
			}
			throw e;
		}
	}


}
