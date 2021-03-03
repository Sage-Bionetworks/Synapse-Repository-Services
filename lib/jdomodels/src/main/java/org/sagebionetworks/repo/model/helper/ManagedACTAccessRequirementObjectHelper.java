package org.sagebionetworks.repo.model.helper;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManagedACTAccessRequirementObjectHelper implements DaoObjectHelper<ManagedACTAccessRequirement> {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Override
	public ManagedACTAccessRequirement create(Consumer<ManagedACTAccessRequirement> consumer) {
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		ar.setAreOtherAttachmentsRequired(true);
		ar.setCreatedOn(new Date());
		ar.setDescription("Something or another");
		ar.setDucTemplateFileHandleId("123");
		ar.setEtag(UUID.randomUUID().toString());
		ar.setExpirationPeriod(0L);
		ar.setIsCertifiedUserRequired(true);
		ar.setIsDUCRequired(true);
		ar.setIsIDUPublic(false);
		ar.setIsIDURequired(true);
		ar.setIsIRBApprovalRequired(true);
		ar.setModifiedOn(new Date());
		ar.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(null);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(rod));

		// allow the caller a chance to override.
		consumer.accept(ar);
		
		if(ar.getModifiedBy() == null) {
			ar.setModifiedBy(ar.getCreatedBy());
		}
		
		return accessRequirementDAO.create(ar);
	}

}
