package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;

public interface JsonSchemaServices {

	Organization createOrganization(Long userId, CreateOrganizationRequest request);
	
	Organization getOrganizationByName(Long userId, String name);

	AccessControlList getOrganizationAcl(Long userId, String id);

	void deleteOrganization(Long userId, String id);

	AccessControlList updateOrganizationAcl(Long userId, String id, AccessControlList acl);

	JsonSchema getSchema(String organizationName, String schemaName, String semanticVersion);

	void deleteSchemaAllVersions(Long userId, String organizationName, String schemaName);

	void deleteSchemaVersion(Long userId, String organizationName, String schemaName, String semanticVersion);

}
