package org.sagebionetworks.repo.manager;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.UserInfo;

public class EvaluationUtil {
	
	public static boolean isEvalAdmin(UserInfo userInfo, Evaluation eval) {
		// check if user is a Synapse admin
		if (userInfo.isAdmin()) return true;
		
		// check if user is the owner of the Evaluation
		String userId = userInfo.getIndividualGroup().getId();
		return userId.equals(eval.getOwnerId());
		
		// TODO: check if user is an authorized admin of the Evaluation
	}

}
