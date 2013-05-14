package org.sagebionetworks.repo.manager;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.UserInfo;

public class EvaluationUtil {
	public static boolean canAdminister(Evaluation evaluation, UserInfo userInfo) {
		if (userInfo.isAdmin()) return true;
		if (evaluation.getOwnerId().equals(userInfo.getIndividualGroup().getId())) return true;
		return false;
	}
}
