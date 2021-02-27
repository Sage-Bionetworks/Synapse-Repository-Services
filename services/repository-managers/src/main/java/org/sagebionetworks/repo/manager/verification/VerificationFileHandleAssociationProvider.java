package org.sagebionetworks.repo.manager.verification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private VerificationDAO verificationDao;
	
	@Autowired
	public VerificationFileHandleAssociationProvider(VerificationDAO verificationDao) {
		this.verificationDao = verificationDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.VerificationSubmission;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		List<Long> associatedIds = verificationDao.listFileHandleIds(Long.parseLong(objectId));
		Set<String> result = new HashSet<String>();
		for (String id : fileHandleIds) {
			if (associatedIds.contains(Long.parseLong(id))) result.add(id);
		}
		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.VERIFICATION_SUBMISSION;
	}
}
