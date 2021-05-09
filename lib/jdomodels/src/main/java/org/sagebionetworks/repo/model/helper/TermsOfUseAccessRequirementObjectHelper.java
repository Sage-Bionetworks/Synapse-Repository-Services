package org.sagebionetworks.repo.model.helper;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

@Service
public class TermsOfUseAccessRequirementObjectHelper implements DaoObjectHelper<TermsOfUseAccessRequirement> {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Override
	public TermsOfUseAccessRequirement create(Consumer<TermsOfUseAccessRequirement> consumer) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(null);
		ar.setModifiedOn(new Date());
		ar.setEtag(UUID.randomUUID().toString());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(null);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(rod));
		ar.setTermsOfUse("Do you agree?");
		// allow the caller to override
		consumer.accept(ar);
		if(ar.getModifiedBy() == null) {
			ar.setModifiedBy(ar.getCreatedBy());
		}
		return accessRequirementDAO.create(ar);
	}

}
