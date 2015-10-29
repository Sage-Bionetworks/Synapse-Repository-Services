package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationFileHandleAssociationProvider implements
		FileHandleAssociationProvider {
	
	@Autowired
	private VerificationDAO verificationDao;


	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.VERIFICATION_SUBMISSION;
	}

}
