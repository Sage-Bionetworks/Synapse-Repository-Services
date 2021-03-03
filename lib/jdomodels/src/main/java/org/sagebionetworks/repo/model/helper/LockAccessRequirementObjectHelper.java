package org.sagebionetworks.repo.model.helper;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LockAccessRequirementObjectHelper implements DaoObjectHelper<LockAccessRequirement> {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Override
	public LockAccessRequirement create(Consumer<LockAccessRequirement> consumer) {
		LockAccessRequirement ar = new LockAccessRequirement();
		ar.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		ar.setCreatedOn(new Date());
		ar.setModifiedOn(new Date());
		ar.setEtag(UUID.randomUUID().toString());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(null);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(rod));
		ar.setJiraKey("some-key");
		
		consumer.accept(ar);
		
		if(ar.getModifiedBy() == null) {
			ar.setModifiedBy(ar.getCreatedBy());
		}
		return accessRequirementDAO.create(ar);
	}

}
