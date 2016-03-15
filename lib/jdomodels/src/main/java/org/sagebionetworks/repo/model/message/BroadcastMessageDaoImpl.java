package org.sagebionetworks.repo.model.message;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class BroadcastMessageDaoImpl implements BroadcastMessageDao {
	
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean hasBroadcast(Long changeNumber) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setBroadcast(Long changeNumber, Long messageId) {
		// TODO Auto-generated method stub
		
	}


}
