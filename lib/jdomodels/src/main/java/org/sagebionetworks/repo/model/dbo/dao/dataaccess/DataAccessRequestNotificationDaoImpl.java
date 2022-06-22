package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class DataAccessRequestNotificationDaoImpl implements DataAccessRequestNotificationDao {

	@Override
	public Optional<Instant> getSentOn(String dataAccessRequestId, Long principalId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<String> getMessageId(String dataAccessRequestId, Long principalId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public void messageSentOn(Long reviewerPrincialId, String dataAccessRequestId, String messageId, Instant sentOn) {
		// TODO Auto-generated method stub
		
	}

}
