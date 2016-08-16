package org.sagebionetworks.repo.model.dao;

import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;

public interface PermissionDao {
	
	/*
	 * Return true if and only if the given entity is in a Submission under an Evaluation 
	 * in which the given user (represented by a list of principalIds) has the given access type.
	 */
	boolean isEntityInEvaluationWithAccess(String entityId, List<Long> principalIds, ACCESS_TYPE accessType);


}
