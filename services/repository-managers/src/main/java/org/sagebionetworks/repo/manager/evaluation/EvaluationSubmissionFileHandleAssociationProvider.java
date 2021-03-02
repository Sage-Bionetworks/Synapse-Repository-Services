package org.sagebionetworks.repo.manager.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluationSubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider{

	private SubmissionFileHandleDAO submissionFileHandleDao;
	
	@Autowired
	public EvaluationSubmissionFileHandleAssociationProvider(SubmissionFileHandleDAO submissionFileHandleDao) {
		this.submissionFileHandleDao = submissionFileHandleDao;
	}

	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.SubmissionAttachment;
	}

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
