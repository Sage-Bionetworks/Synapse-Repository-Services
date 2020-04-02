package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.OrganizationRequest;

public interface JsonSchemaServices {

	Organization createOrganization(Long userId, OrganizationRequest request);
	
	Organization getOrganizationByName(Long userId, String name);

	AccessControlList getOrganizationAcl(Long userId, String id);

	void deleteOrganization(Long userId, String id);

	AccessControlList updateOrganizationAcl(Long userId, String id, AccessControlList acl);

}
