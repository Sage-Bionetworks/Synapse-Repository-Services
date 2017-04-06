package org.sagebionetworks.repo.manager.dataaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessSubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider{

	@Autowired
	DataAccessSubmissionDAO dataAccessSubmissionDao;

	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> associatedIds = new HashSet<String>();
		DataAccessSubmission submission = dataAccessSubmissionDao.getSubmission(objectId);
		if (submission.getAttachments()!= null && !submission.getAttachments().isEmpty()) {
			associatedIds.addAll(submission.getAttachments());
		}
		if (submission.getDucFileHandleId() != null) {
			associatedIds.add(submission.getDucFileHandleId());
		}
		if (submission.getIrbFileHandleId() != null) {
			associatedIds.add(submission.getIrbFileHandleId());
		}
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.DATA_ACCESS_SUBMISSION;
	}

}
