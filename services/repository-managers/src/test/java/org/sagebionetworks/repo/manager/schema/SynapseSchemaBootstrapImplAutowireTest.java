package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynapseSchemaBootstrapImplAutowireTest {
	
	@Autowired
	private SynapseSchemaBootstrap bootstrap;
	
	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private UserManager userManager;

	@BeforeEach
	public void before() {
		jsonSchemaManager.truncateAll();
	}
	
	@Test
	public void testMultipletimes() throws RecoverableMessageException {
		ListJsonSchemaInfoRequest listRequest = new ListJsonSchemaInfoRequest();
		listRequest.setOrganizationName("org.sagebionetworks");
		ListJsonSchemaInfoResponse response = jsonSchemaManager.listSchemas(listRequest);
		assertNotNull(response.getPage());
		assertEquals(0, response.getPage().size());
		
		// call under test
		bootstrap.bootstrapSynapseSchemas();
		
		response = jsonSchemaManager.listSchemas(listRequest);
		assertNotNull(response.getPage());
		assertTrue(response.getPage().size() > 3);
		for(JsonSchemaInfo schemaInfo: response.getPage()) {
			// there should only be one version of each schema
			assertEquals(1, getVersionCount(schemaInfo));
		}
		
		
		// A second call to the bootstrap must not create additional versions.s
		bootstrap.bootstrapSynapseSchemas();
		response = jsonSchemaManager.listSchemas(listRequest);
		assertNotNull(response.getPage());
		assertTrue(response.getPage().size() > 3);
		for(JsonSchemaInfo schemaInfo: response.getPage()) {
			// there should only be one version of each schema
			assertEquals(1, getVersionCount(schemaInfo));
		}
	}
	
	@Test
	public void testBootstrapSynapseSchemasWithAccessRequirementBaseSchema() throws RecoverableMessageException {
		bootstrap.bootstrapSynapseSchemas();

		// The $id of a top level schema is absolute, which is what a client is served.
		JsonSchema registered = jsonSchemaManager
				.getSchema(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID, true);

		assertEquals(Type.object, registered.getType());
		assertEquals(List.of(JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY), registered.getRequired());
		assertEquals(new JsonSchema().setType(Type.string).set_enum(List.of("REQUEST", "RENEWAL")),
				registered.getProperties().get(JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY));
	}

	@Test
	public void testBootstrapSynapseSchemasWithActOrganization() throws RecoverableMessageException {
		bootstrap.bootstrapSynapseSchemas();

		UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		Organization organization = jsonSchemaManager.getOrganizationByName(admin,
				SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT);

		assertEquals(SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT_ID.toString(), organization.getId());

		AccessControlList acl = jsonSchemaManager.getOrganizationAcl(admin, organization.getId());

		assertTrue(acl.getResourceAccess().contains(new ResourceAccess()
				.setPrincipalId(TeamConstants.ACT_TEAM_ID)
				.setAccessType(new HashSet<>(JsonSchemaManagerImpl.ADMIN_PERMISSIONS))));

		// A repeated bootstrap must leave the organization and its ACL exactly as they were.
		bootstrap.bootstrapSynapseSchemas();

		assertEquals(organization, jsonSchemaManager.getOrganizationByName(admin,
				SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT));
		assertEquals(acl, jsonSchemaManager.getOrganizationAcl(admin, organization.getId()));
	}

	@Test
	public void testCreateJsonSchemaInActOrganizationWithActMember() throws RecoverableMessageException {
		bootstrap.bootstrapSynapseSchemas();

		// call under test
		CreateSchemaResponse response = jsonSchemaManager.createJsonSchema(actMember(),
				new CreateSchemaRequest().setSchema(newActSchema()));

		assertEquals(SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT,
				response.getNewVersionInfo().getOrganizationName());
	}

	@Test
	public void testCreateJsonSchemaInActOrganizationWithNonActMember() throws RecoverableMessageException {
		bootstrap.bootstrapSynapseSchemas();

		CreateSchemaRequest request = new CreateSchemaRequest().setSchema(newActSchema());

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			jsonSchemaManager.createJsonSchema(nonActMember(), request);
		});
	}

	/**
	 * A non admin whose only authority over the organization comes from the ACT team entry that the
	 * bootstrap adds to its ACL. Built from the admin principal so that the created schema has a
	 * principal that exists, while the admin short circuit is off.
	 */
	private UserInfo actMember() {
		return newNonAdmin(Set.of(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				TeamConstants.ACT_TEAM_ID));
	}

	private UserInfo nonActMember() {
		return newNonAdmin(Set.of(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
	}

	private UserInfo newNonAdmin(Set<Long> groups) {
		UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		return new UserInfo(false, admin.getId(), admin.getRealmId(), new HashSet<>(groups));
	}

	private static JsonSchema newActSchema() {
		return new JsonSchema()
				.set$id(SynapseSchemaBootstrapImpl.ORG_SAGEBIONETWORKS_ACT + "-TestDataAccessRequest-1.0.0")
				.setType(Type.object)
				.setAllOf(List.of(new JsonSchema().set$ref(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID)));
	}

	/**
	 * Helper to get the number of version for a given schema.s
	 * @param info
	 * @return
	 */
	int getVersionCount(JsonSchemaInfo info){
		ListJsonSchemaVersionInfoRequest request = new ListJsonSchemaVersionInfoRequest();
		request.setOrganizationName(info.getOrganizationName());
		request.setSchemaName(info.getSchemaName());
		ListJsonSchemaVersionInfoResponse response = jsonSchemaManager.listSchemaVersions(request);
		assertNotNull(response.getPage());
		return response.getPage().size();
	}

}
