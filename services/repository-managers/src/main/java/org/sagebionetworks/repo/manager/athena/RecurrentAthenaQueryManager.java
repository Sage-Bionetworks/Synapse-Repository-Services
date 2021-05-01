package org.sagebionetworks.repo.manager.athena;

import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;

import com.amazonaws.services.sqs.model.Message;

public interface RecurrentAthenaQueryManager {

	RecurrentAthenaQueryResult fromSqsMessage(Message message);

	void processRecurrentAthenaQueryResult(RecurrentAthenaQueryResult request, String queueUrl);

}
