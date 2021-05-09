package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessApprovalObjectHelper implements DaoObjectHelper<AccessApproval> {
	
	@Autowired
	private AccessApprovalDAO approvalDao;

	@Override
	public AccessApproval create(Consumer<AccessApproval> consumer) {
		AccessApproval a = new AccessApproval();
		a.setAccessorId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		a.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		a.setCreatedOn(new Date());
		a.setModifiedOn(new Date());
		a.setEtag(UUID.randomUUID().toString());
		a.setExpiredOn(null);
		a.setSubmitterId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		a.setState(ApprovalState.APPROVED);
		
		// Caller override
		consumer.accept(a);
		
		if(a.getModifiedBy() == null) {
			a.setModifiedBy(a.getCreatedBy());
		}
		return approvalDao.create(a);
	}

}
