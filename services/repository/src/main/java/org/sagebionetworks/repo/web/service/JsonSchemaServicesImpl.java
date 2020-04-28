package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaServicesImpl implements JsonSchemaServices {

	@Autowired
	UserManager userManager;
	@Autowired
	JsonSchemaManager schemaManager;

	@Override
	public Organization createOrganization(Long userId, CreateOrganizationRequest request) {
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

	@Override
	public JsonSchema getSchema(String organizationName, String schemaName, String semanticVersion) {
		return schemaManager.getSchema(organizationName, schemaName, semanticVersion);
	}

	@Override
	public void deleteSchemaAllVersions(Long userId, String organizationName, String schemaName) {
		UserInfo user = userManager.getUserInfo(userId);
		schemaManager.deleteSchemaAllVersion(user, organizationName, schemaName);
	}

	@Override
	public void deleteSchemaVersion(Long userId, String organizationName, String schemaName, String semanticVersion) {
		UserInfo user = userManager.getUserInfo(userId);
		schemaManager.deleteSchemaVersion(user, organizationName, schemaName, semanticVersion);
	}

}
