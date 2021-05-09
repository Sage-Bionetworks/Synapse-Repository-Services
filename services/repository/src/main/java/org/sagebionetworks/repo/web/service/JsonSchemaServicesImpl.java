package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
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
	public JsonSchema getSchema(String $id) {
		boolean isTopLevel = true;
		return schemaManager.getSchema($id, isTopLevel);
	}

	@Override
	public void deleteSchemaById(Long userId, String $id) {
		UserInfo user = userManager.getUserInfo(userId);
		schemaManager.deleteSchemaById(user, $id);
	}

	@Override
	public ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request) {
		return schemaManager.listOrganizations(request);
	}

	@Override
	public ListJsonSchemaInfoResponse listSchemas(ListJsonSchemaInfoRequest request) {
		return schemaManager.listSchemas(request);
	}

	@Override
	public ListJsonSchemaVersionInfoResponse listSchemasVersions(ListJsonSchemaVersionInfoRequest request) {
		return schemaManager.listSchemaVersions(request);
	}



}
