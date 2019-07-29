package org.sagebionetworks.evaluation.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.evaluation.SubmissionFileHandleDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class SubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider{

	@Autowired
	private SubmissionFileHandleDAO submissionFileHandleDao;

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		List<String> associatedIds = submissionFileHandleDao.getAllBySubmission(objectId);
		Set<String> result = new HashSet<String>();
		for (String fileHandleId : fileHandleIds) {
			if (associatedIds.contains(fileHandleId)) {
				result.add(fileHandleId);
			}
		}
		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.EVALUATION_SUBMISSIONS;
	}

}
