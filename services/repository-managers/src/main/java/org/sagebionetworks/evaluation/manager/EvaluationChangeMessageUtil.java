package org.sagebionetworks.evaluation.manager;

import java.util.UUID;

import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
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
		EvaluationSubmissionsObservableEntity observable = new EvaluationSubmissionsObservableEntity(
				evalId.toString(), evaluationSubmissionsEtag);
		transactionalMessenger.sendMessageAfterCommit(observable, changeType);
	}


}
