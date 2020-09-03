package org.sagebionetworks.dataaccess.workers;

import java.time.Instant;
import java.util.Date;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessTestHelper {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDao;
	
	@Autowired
	private AccessApprovalDAO accessApprovalDao;
	
	public void cleanUp() {
		accessApprovalDao.clear();
		accessRequirementDao.clear();
	}
	
	public AccessApproval getApproval(Long id) {
		return accessApprovalDao.get(id.toString());
	}
	
	public AccessRequirement newAccessRequirement(UserInfo user) {
		AccessRequirement accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setCreatedBy(user.getId().toString());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(user.getId().toString());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setConcreteType(ManagedACTAccessRequirement.class.getName());
		
		return accessRequirementDao.create(accessRequirement);
	}
	
	public AccessApproval newApproval(AccessRequirement accessRequirement, UserInfo user, ApprovalState state, Instant expiresOn) {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setCreatedBy(user.getId().toString());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(user.getId().toString());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setAccessorId(user.getId().toString());
		accessApproval.setRequirementId(accessRequirement.getId());
		accessApproval.setRequirementVersion(accessRequirement.getVersionNumber());
		accessApproval.setSubmitterId(user.getId().toString());
		accessApproval.setExpiredOn(expiresOn == null ? null : Date.from(expiresOn));
		accessApproval.setState(state);
		
		return accessApprovalDao.create(accessApproval);
	}
	
}
