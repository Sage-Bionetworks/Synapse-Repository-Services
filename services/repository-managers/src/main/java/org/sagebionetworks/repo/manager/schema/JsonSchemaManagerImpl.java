package org.sagebionetworks.repo.manager.schema;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.BindSchemaRequest;
import org.sagebionetworks.repo.model.dbo.schema.JsonSchemaDao;
import org.sagebionetworks.repo.model.dbo.schema.NewSchemaVersionRequest;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.schema.SchemaDependency;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.SubSchemaIterable;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
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

	public static final int MAX_ORGANZIATION_NAME_CHARS = 250;
	public static final int MIN_ORGANZIATION_NAME_CHARS = 6;

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

		String processedOrganizationName = processAndValidateOrganizationName(user, request.getOrganizationName());
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
	public static String processAndValidateOrganizationName(UserInfo user, String name) {
		ValidateArgument.required(name, "organizationName");
		ValidateArgument.required(user, "user");
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
		if (!user.isAdmin()) {
			if (processedName.toLowerCase().contains("sagebionetwork")) {
				throw new IllegalArgumentException(SAGEBIONETWORKS_RESERVED_MESSAGE);
			}
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
		String processedOrganizationName = processAndValidateOrganizationName(user, name);
		return organizationDao.getOrganizationByName(processedOrganizationName);
	}

	@WriteTransaction
	@Override
	public CreateSchemaResponse createJsonSchema(UserInfo user, CreateSchemaRequest request) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSchema(), "request.schema");

		AuthorizationUtils.disallowAnonymous(user);

		SchemaId schemaId = SchemaIdParser.parseSchemaId(request.getSchema().get$id());

		String semanticVersionString = null;
		if (schemaId.getSemanticVersion() != null) {
			semanticVersionString = schemaId.getSemanticVersion().toString();
		}
		String schemaNameString = schemaId.getSchemaName().toString();

		// User must have create on the organization.
		Organization organization = organizationDao.getOrganizationByName(schemaId.getOrganizationName().toString());
		aclDao.canAccess(user, organization.getId(), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE)
				.checkAuthorizationOrElseThrow();
		List<SchemaDependency> dependencies = findAllDependencies(request.getSchema());
		NewSchemaVersionRequest newVersionRequest = new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withCreatedBy(user.getId()).withSchemaName(schemaNameString)
				.withSemanticVersion(semanticVersionString).withJsonSchema(request.getSchema())
				.withDependencies(dependencies);
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(newVersionRequest);
		
		// Ensure we can create the validation schema
		JsonSchema validationSchema = getValidationSchema(schemaId.toString());

		CreateSchemaResponse response = new CreateSchemaResponse();
		response.setNewVersionInfo(info);
		response.setValidationSchema(validationSchema);
		return response;
	}

	/**
	 * Find all of the dependencies for the given schema.
	 * 
	 * @param id
	 * @param schema
	 * @return
	 */
	List<SchemaDependency> findAllDependencies(JsonSchema schema) {
		ValidateArgument.required(schema, "schema");
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		for (JsonSchema subSchema : SubSchemaIterable.depthFirstIterable(schema)) {
			if (subSchema.get$ref() != null) {
				JsonSchemaVersionInfo versionInfo = getVersionInfo(subSchema.get$ref());
				SchemaId schemaId = SchemaIdParser.parseSchemaId(subSchema.get$ref());
				String dependsOnVersionId = null;
				if (schemaId.getSemanticVersion() != null) {
					dependsOnVersionId = versionInfo.getVersionId();
				}
				String dependsOnSchemaId = versionInfo.getSchemaId();
				dependencies.add(new SchemaDependency().withDependsOnSchemaId(dependsOnSchemaId)
						.withDependsOnVersionId(dependsOnVersionId));
			}
		}
		return dependencies;
	}

	/**
	 * Get the versionId for the given $id
	 * 
	 * @param $id
	 * @return
	 */
	public String getSchemaVersionId(String $id) {
		ValidateArgument.required($id, "id");
		SchemaId schemaId = SchemaIdParser.parseSchemaId($id);
		String organizationName = schemaId.getOrganizationName().toString();
		String schemaName = schemaId.getSchemaName().toString();
		String semanticVersion = null;
		if (schemaId.getSemanticVersion() != null) {
			semanticVersion = schemaId.getSemanticVersion().toString();
		}
		return getSchemaVersionId(organizationName, schemaName, semanticVersion);
	}

	/**
	 * Get the JsonSchemaVersionInfo for the given $id
	 * 
	 * @param $id
	 * @return
	 */
	JsonSchemaVersionInfo getVersionInfo(String $id) {
		String versionId = getSchemaVersionId($id);
		return jsonSchemaDao.getVersionInfo(versionId);
	}

	@Override
	public JsonSchema getSchema(String organizationName, String schemaName, String semanticVersion) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		String versionId = getSchemaVersionId(organizationName, schemaName, semanticVersion);
		return jsonSchemaDao.getSchema(versionId);
	}

	public String getSchemaVersionId(String organizationName, String schemaName, String semanticVersion) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		organizationName = organizationName.trim();
		schemaName = schemaName.trim();
		semanticVersion = StringUtils.trimToNull(semanticVersion);
		if (semanticVersion == null) {
			return jsonSchemaDao.getLatestVersionId(organizationName, schemaName);
		} else {
			return jsonSchemaDao.getVersionId(organizationName, schemaName, semanticVersion);
		}
	}

	@Override
	public void truncateAll() {
		jsonSchemaDao.truncateAll();
		organizationDao.truncateAll();
	}

	@WriteTransaction
	@Override
	public void deleteSchemaAllVersion(UserInfo user, String organizationName, String schemaName) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		JsonSchemaVersionInfo versionInfo = jsonSchemaDao.getVersionLatestInfo(organizationName, schemaName);
		// Must have delete on the organization
		if (!user.isAdmin()) {
			aclDao.canAccess(user, versionInfo.getOrganizationId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE)
					.checkAuthorizationOrElseThrow();
		}
		jsonSchemaDao.deleteSchema(versionInfo.getSchemaId());
	}

	@WriteTransaction
	@Override
	public void deleteSchemaVersion(UserInfo user, String organizationName, String schemaName, String semanticVersion) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		ValidateArgument.required(semanticVersion, "semanticVersion");
		JsonSchemaVersionInfo versionInfo = jsonSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersion);
		// Must have delete on the organization
		if (!user.isAdmin()) {
			aclDao.canAccess(user, versionInfo.getOrganizationId(), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE)
					.checkAuthorizationOrElseThrow();
		}
		jsonSchemaDao.deleteSchemaVersion(versionInfo.getVersionId());
	}

	@Override
	public JsonSchemaVersionInfo getLatestVersion(String organizationName, String schemaName) {
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(schemaName, "schemaName");
		String versionId = jsonSchemaDao.getLatestVersionId(organizationName, schemaName);
		return jsonSchemaDao.getVersionInfo(versionId);
	}

	@Override
	public ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request) {
		ValidateArgument.required(request, "ListOrganizationsRequest");
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<Organization> page = organizationDao.listOrganizations(nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
		ListOrganizationsResponse response = new ListOrganizationsResponse();
		response.setPage(page);
		response.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
		return response;
	}

	@Override
	public ListJsonSchemaInfoResponse listSchemas(ListJsonSchemaInfoRequest request) {
		ValidateArgument.required(request, "ListJsonSchemaInfoRequest");
		ValidateArgument.required(request.getOrganizationName(), "organizationName");
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<JsonSchemaInfo> page = jsonSchemaDao.listSchemas(request.getOrganizationName(),
				nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		ListJsonSchemaInfoResponse response = new ListJsonSchemaInfoResponse();
		response.setPage(page);
		response.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
		return response;
	}

	@Override
	public ListJsonSchemaVersionInfoResponse listSchemaVersions(ListJsonSchemaVersionInfoRequest request) {
		ValidateArgument.required(request, "ListJsonSchemaVersionInfoRequest");
		ValidateArgument.required(request.getOrganizationName(), "organizationName");
		ValidateArgument.required(request.getSchemaName(), "schemaName");
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<JsonSchemaVersionInfo> page = jsonSchemaDao.listSchemaVersions(request.getOrganizationName(),
				request.getSchemaName(), nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		ListJsonSchemaVersionInfoResponse response = new ListJsonSchemaVersionInfoResponse();
		response.setPage(page);
		response.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
		return response;
	}
	
	/**

	 * 
	 * @param id
	 * @return
	 */
	@Override
	public JsonSchema getValidationSchema(String id) {
		Deque<String> visitedStack = new ArrayDeque<String>();
		return getValidationSchema(visitedStack, id);
	}
	
	/**
	 * Recursively 
	 * @param visitedSchemas
	 * @param id
	 * @return
	 */
	JsonSchema getValidationSchema(Deque<String> visitedStack, String id) {
		// duplicates are allowed but cycles are not
		if(visitedStack.contains(id)) {
			throw new IllegalArgumentException("Schema $id: '"+id+"' has a circular dependency");
		}
		visitedStack.push(id);
		// get the base schema
		JsonSchema baseSchema = getSchema(id);
		if(baseSchema.get$defs() == null) {
			baseSchema.set$defs(new LinkedHashMap<String, JsonSchema>());
		}
		for (JsonSchema subSchema : SubSchemaIterable.depthFirstIterable(baseSchema)) {
			if (subSchema.get$ref() != null) {
				String local$defsId = createLocal$defsId(subSchema.get$ref());
				if (!baseSchema.get$defs().containsKey(local$defsId)) {
					// Load the sub-schema's validation schema
					JsonSchema validationSubSchema = getValidationSchema(visitedStack, subSchema.get$ref());
					// Merge the $defs from the new schema with the current
					if (validationSubSchema.get$defs() != null) {
						baseSchema.get$defs().putAll(validationSubSchema.get$defs());
					}
					validationSubSchema.set$defs(null);
					baseSchema.get$defs().put(local$defsId, validationSubSchema);
				}
				// replace the $ref to the local $def
				subSchema.set$ref(local$defsId);
			}
		}
		if(baseSchema.get$defs().isEmpty()) {
			baseSchema.set$defs(null);
		}
		visitedStack.pop();
		return baseSchema;
	}

	/**
	 * Create a $ref to the a local $refs map given an original $id
	 * 
	 * @param $id
	 * @return
	 */
	public static String createLocal$defsId(String $id) {
		return "#/$defs/" + $id;
	}

	/**
	 * Get a JsonSchema given its $id;
	 * 
	 * @param $id
	 * @return
	 */
	public JsonSchema getSchema(String $id) {
		ValidateArgument.required($id, "id");
		String versionId = getSchemaVersionId($id);
		return jsonSchemaDao.getSchema(versionId);
	}

	@Override
	public JsonSchemaObjectBinding bindSchemaToObject(Long createdBy, String $id, Long objectId, BoundObjectType objectType) {
		ValidateArgument.required(createdBy, "createdBy");
		ValidateArgument.required($id, "$id");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		SchemaId parsedId = SchemaIdParser.parseSchemaId($id);
		String schemaId = jsonSchemaDao.getSchemaId(parsedId.getOrganizationName().toString(), parsedId.getSchemaName().toString());
		// versionId is null unless a semantic version is provided in the $id.
		String versionId = null;
		if(parsedId.getSemanticVersion() != null) {
			versionId = jsonSchemaDao.getVersionId(parsedId.getOrganizationName().toString(), parsedId.getSchemaName().toString(), parsedId.getSemanticVersion().toString());
		}
		return jsonSchemaDao.bindSchemaToObject(new BindSchemaRequest().withCreatedBy(createdBy).withObjectId(objectId)
				.withObjectType(objectType).withSchemaId(schemaId).withVersionId(versionId));
	}

	@Override
	public JsonSchemaObjectBinding getJsonSchemaObjectBinding(Long objectId, BoundObjectType objectType) {
		return jsonSchemaDao.getSchemaBindingForObject(objectId, objectType);
	}

}
