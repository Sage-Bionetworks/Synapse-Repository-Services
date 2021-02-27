package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubmissionFileHandleAssociationProvider implements FileHandleAssociationProvider {
		
	private SubmissionDAO submissionDao;
	
	@Autowired
	public SubmissionFileHandleAssociationProvider(SubmissionDAO submissionDao) {
		this.submissionDao = submissionDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.DataAccessSubmissionAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Submission submission = submissionDao.getSubmission(objectId);
		Set<String> associatedIds = SubmissionUtils.extractAllFileHandleIds(submission);
		associatedIds.retainAll(fileHandleIds);
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.DATA_ACCESS_SUBMISSION;
	}

}
