package org.sagebionetworks.evaluation.manager;

import java.util.UUID;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;

public class EvaluationChangeMessageUtil {
	public static void sendEvaluationSubmissionsChangeMessage(
			Long evalId, 
			ChangeType changeType, 
			EvaluationDAO evaluationDAO,
			TransactionalMessenger transactionalMessenger) {
		String evaluationSubmissionsEtag = UUID.randomUUID().toString();
		evaluationDAO.updateSubmissionsEtag(evalId.toString(), evaluationSubmissionsEtag); 
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message.setObjectId(evalId.toString());
		message.setObjectEtag(evaluationSubmissionsEtag);
		transactionalMessenger.sendMessageAfterCommit(message);
	}


}
