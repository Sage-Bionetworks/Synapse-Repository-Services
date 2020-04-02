package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.OrganizationRequest;
import org.springframework.beans.factory.annotation.Autowired;

public class JsonSchemaServicesImpl implements JsonSchemaServices {

	@Autowired
	UserManager userManager;
	@Autowired
	JsonSchemaManager schemaManager;

	@Override
	public Organization createOrganization(Long userId, OrganizationRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return schemaManager.createOrganziation(user, request);
	}

	@Override
	public Organization getOrganizationByName(Long userId, String name) {
		UserInfo user = userManager.getUserInfo(userId);
		return schemaManager.getOrganizationByName(user, name);
	}

	@Override
	public void deleteOrganization(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		schemaManager.deleteOrganization(user, id);
	}

	@Override
	public AccessControlList getOrganizationAcl(Long userId, String id) {
		UserInfo user = userManager.getUserInfo(userId);
		return schemaManager.getOrganizationAcl(user, id);
	}

	@Override
	public AccessControlList updateOrganizationAcl(Long userId, String id, AccessControlList acl) {
		UserInfo user = userManager.getUserInfo(userId);
		return schemaManager.updateOrganizationAcl(user, id, acl);
	}

}
