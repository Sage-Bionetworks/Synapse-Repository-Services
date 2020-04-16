package org.sagebionetworks.repo.manager.schema;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.Date;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.JsonSchemaDao;
import org.sagebionetworks.repo.model.dbo.schema.NewVersionRequest;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.SchemaInfo;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.id.OrganizationName;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class JsonSchemaManagerImpl implements JsonSchemaManager {

	public static final String SAGEBIONETWORKS_RESERVED_MESSAGE = "The name 'sagebionetworks' is reserved, and cannot be included in an Organziation's name";

	public static int MAX_ORGANZIATION_NAME_CHARS = 250;
	public static int MIN_ORGANZIATION_NAME_CHARS = 6;
	public static int MAX_SEMANTIC_VERSION_CHARS = 250;

	@Autowired
	private OrganizationDao organizationDao;

	@Autowired
	private AccessControlListDAO aclDao;

	@Autowired
	private JsonSchemaDao jsonSchemaDao;

	public static final Set<ACCESS_TYPE> ADMIN_PERMISSIONS = Sets.newHashSet(READ, CREATE, CHANGE_PERMISSIONS, UPDATE,
			DELETE);

	@WriteTransaction
	@Override
	public Organization createOrganziation(UserInfo user, CreateOrganizationRequest request) {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(request, "OrganizationRequest");

		AuthorizationUtils.disallowAnonymous(user);

		String processedOrganizationName = processAndValidateOrganizationName(request.getOrganizationName());
		Organization org = organizationDao.createOrganization(processedOrganizationName, user.getId());

		// Create an ACL for the
		AccessControlList acl = AccessControlListUtil.createACL(org.getId(), user, ADMIN_PERMISSIONS, new Date());
		aclDao.create(acl, ObjectType.ORGANIZATION);

		return org;
	}

	/**
	 * If the provided organization name is not valid, an IllegalArgumentException
	 * will be thrown. The process name will be trimmed.
	 * 
	 * @param name
	 * @return
	 */
	public static String processAndValidateOrganizationName(String name) {
		ValidateArgument.required(name, "organizationName");
		OrganizationName orgName = SchemaIdParser.parseOrganizationName(name);
		String processedName = orgName.toString();
		if (processedName.length() > MAX_ORGANZIATION_NAME_CHARS) {
			throw new IllegalArgumentException(
					"Organization name must be " + MAX_ORGANZIATION_NAME_CHARS + " characters or less");
		}
		if (processedName.length() < MIN_ORGANZIATION_NAME_CHARS) {
			throw new IllegalArgumentException(
					"Organization name must be at least " + MIN_ORGANZIATION_NAME_CHARS + " characters");
		}
		if (processedName.toLowerCase().contains("sagebionetwork")) {
			throw new IllegalArgumentException(SAGEBIONETWORKS_RESERVED_MESSAGE);
		}
		return processedName;
	}

	@Override
	public AccessControlList getOrganizationAcl(UserInfo user, String organziationId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(organziationId, "organziationId");
		// Validate read access.
		aclDao.canAccess(user, organziationId, ObjectType.ORGANIZATION, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return aclDao.get(organziationId, ObjectType.ORGANIZATION);
	}

	@WriteTransaction
	@Override
	public AccessControlList updateOrganizationAcl(UserInfo user, String organziationId, AccessControlList acl) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(organziationId, "organziationId");
		ValidateArgument.required(acl, "acl");
		Long organziationIdLong;
		try {
			organziationIdLong = Long.parseLong(organziationId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid organziationId: " + organziationId);
		}
		// id must match the value from the URL path.
		acl.setId(organziationId);

		// Ensure the user does not revoke their own access to the ACL.
		PermissionsManagerUtils.validateACLContent(acl, user, organziationIdLong);

		// Validate CHANGE_PERMISSIONS
		aclDao.canAccess(user, organziationId, ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS)
				.checkAuthorizationOrElseThrow();

		aclDao.update(acl, ObjectType.ORGANIZATION);
		return aclDao.get(organziationId, ObjectType.ORGANIZATION);
	}

	@WriteTransaction
	@Override
	public void deleteOrganization(UserInfo user, String id) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(id, "id");

		if (!user.isAdmin()) {
			aclDao.canAccess(user, id, ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();
		}

		organizationDao.deleteOrganization(id);

		aclDao.delete(id, ObjectType.ORGANIZATION);
	}

	@Override
	public Organization getOrganizationByName(UserInfo user, String name) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(name, "name");
		String processedOrganizationName = processAndValidateOrganizationName(name);
		return organizationDao.getOrganizationByName(processedOrganizationName);
	}

	@WriteTransaction
	@Override
	public CreateSchemaResponse createJsonSchema(UserInfo user, CreateSchemaRequest request) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSchema(), "request.schema");
		ValidateArgument.required(request.getSchema(), "request.schema");

		AuthorizationUtils.disallowAnonymous(user);

		SchemaId schemaId = SchemaIdParser.parseSchemaId(request.getSchema().get$id());
		
		String semanticVersion = null;
		if (schemaId.getSemanticVersion() != null) {
			semanticVersion = schemaId.getSemanticVersion().toString();
			if(semanticVersion.length() > MAX_SEMANTIC_VERSION_CHARS) {
				throw new IllegalArgumentException("Semantic version must be "+MAX_SEMANTIC_VERSION_CHARS+" characters or less");
			}
		}
		
		// Does the user have update on the organization
		Organization organization = organizationDao.getOrganizationByName(schemaId.getOrganizationName().toString());
		aclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();

		Date now = new Date();
		SchemaInfo schemaRoot = new SchemaInfo();
		schemaRoot.setOrganizationId(organization.toString());
		schemaRoot.setName(schemaId.getSchemaName().toString());
		schemaRoot.setCreatedBy(user.getId().toString());
		schemaRoot.setCreatedOn(now);
		// Create or get the root schema
		schemaRoot = jsonSchemaDao.createSchemaIfDoesNotExist(schemaRoot);
		// Create or get the JSON blob
		String jsonBlobId = createJsonBlobIfDoesNotExist(request.getSchema());

		// Unconditionally create a new version.
		String versionId = jsonSchemaDao.createNewVersion(
				new NewVersionRequest().withSchemaId(schemaRoot.getNumericId()).withSemanticVersion(semanticVersion)
						.withCreatedBy(user.getId()).withCreatedOn(now).withBlobId(jsonBlobId));

		return null;
	}

	/**
	 * Create a new JSON blob if the one does not already exist for the given
	 * schema.
	 * 
	 * @param schema
	 * @return
	 */
	String createJsonBlobIfDoesNotExist(JsonSchema schema) {
		String schemaJson = null;
		try {
			schemaJson = EntityFactory.createJSONStringForEntity(schema);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
		String sha256hex = DigestUtils.sha256Hex(schemaJson);
		return jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, sha256hex);
	}

}
