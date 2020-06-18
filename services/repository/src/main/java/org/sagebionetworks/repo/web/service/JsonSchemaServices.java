package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.Organization;

public interface JsonSchemaServices {

	Organization createOrganization(Long userId, CreateOrganizationRequest request);
	
	Organization getOrganizationByName(Long userId, String name);

	AccessControlList getOrganizationAcl(Long userId, String id);

	void deleteOrganization(Long userId, String id);

	AccessControlList updateOrganizationAcl(Long userId, String id, AccessControlList acl);

	JsonSchema getSchema(String $id);

	ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request);

	ListJsonSchemaInfoResponse listSchemas(ListJsonSchemaInfoRequest request);

	ListJsonSchemaVersionInfoResponse listSchemasVersions(ListJsonSchemaVersionInfoRequest request);

	void deleteSchemaById(Long userId, String $id);

}
