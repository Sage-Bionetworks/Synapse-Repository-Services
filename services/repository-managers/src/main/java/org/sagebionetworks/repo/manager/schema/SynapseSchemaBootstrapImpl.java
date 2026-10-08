package org.sagebionetworks.repo.manager.schema;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.AccessControlListManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.schema.*;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetCollection;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.parser.TokenMgrError;
import org.sagebionetworks.schema.semantic.version.SemanticVersion;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class SynapseSchemaBootstrapImpl implements SynapseSchemaBootstrap {

	public static final String ORG_SAGEBIONETWORKS = "org.sagebionetworks";
	public static final Long ORG_SAGEBIONETWORKS_ID = 7L;

	/**
	 * The organization that the ACT authors access requirement schemas under.
	 */
	public static final String ORG_SAGEBIONETWORKS_ACT = "org.sagebionetworks.act";

	/**
	 * Pinned for the same reason as {@link #ORG_SAGEBIONETWORKS_ID}: ORGANIZATION migrates between
	 * stacks with a unique name, so a bootstrapped organization must land on the same id on every
	 * stack. Values below the starting id of {@link org.sagebionetworks.ids.IdType#ORGANIZATION_ID}
	 * are never handed out by the id generator, so they are available to pin.
	 */
	public static final Long ORG_SAGEBIONETWORKS_ACT_ID = 8L;

	public static final String ACCESS_REQUIREMENT_BASE_SCHEMA_FILE = "schema/bootstrap/AccessRequirementBaseSchema.json";

	/**
	 * The Synapse objects that can be referenced in JSON schemas and therefore must
	 * exist in the repository.
	 */
	public static final List<String> OBJECTS_TO_BOOTSTRAP = Lists.newArrayList(
			FileEntity.class.getName(),
			Folder.class.getName(),
			Project.class.getName(),
			TableEntity.class.getName(),
			EntityView.class.getName(),
			Dataset.class.getName(),
			DatasetCollection.class.getName(),
			SubmissionView.class.getName(),
			MaterializedView.class.getName(),
			VirtualTable.class.getName(),
			DockerRepository.class.getName(),
			Link.class.getName(),
			RecordSet.class.getName(),
			SearchIndex.class.getName()
	);

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private AccessControlListManager aclManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	SchemaTranslator translator;

	@WriteTransaction
	@Override
	public void bootstrapSynapseSchemas() throws RecoverableMessageException {
		// The process is run as the Synapse admin
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		createOrganizationIfDoesNotExist(adminUser);
		// Reconciles what the startup bootstrapper already did, so that a stack whose startup
		// predates a change to the base schema still picks it up.
		bootstrapAccessRequirementBaseSchema(adminUser);

		List<ObjectSchema> allSchemasToBootstrap = loadAllSchemasAndReferences(OBJECTS_TO_BOOTSTRAP);

		for (ObjectSchema objectSchema : allSchemasToBootstrap) {
			JsonSchema jsonSchema = translator.translate(objectSchema);
			replaceReferencesWithLatestVersion(jsonSchema);
			registerSchemaIfDoesNotExist(adminUser, jsonSchema);
		}
	}

	@WriteTransaction
	@Override
	public void bootstrapAccessRequirementBaseSchema(UserInfo adminUser) {
		// The base schema belongs to 'org.sagebionetworks', and registering a schema requires CREATE
		// on its organization, so that organization and its ACL must be in place first. It is
		// created here rather than relying on whichever other bootstrapper also creates it, because
		// the order the context instantiates beans in is not defined.
		createOrganizationIfDoesNotExist(adminUser);
		createActOrganizationIfDoesNotExist(adminUser);
		registerSchemaIfDoesNotExist(adminUser, loadAccessRequirementBaseSchema());
	}

	/**
	 * Create the 'org.sagebionetworks' organization if it does not already exists
	 * @param adminUser
	 */
	@Override
	public Organization createOrganizationIfDoesNotExist(UserInfo adminUser) {
		return createOrganizationIfDoesNotExist(adminUser, ORG_SAGEBIONETWORKS, ORG_SAGEBIONETWORKS_ID);
	}

	/**
	 * Create the 'org.sagebionetworks.act' organization if it does not already exist, and grant the
	 * ACT team the permissions it needs to author schemas under it.
	 *
	 * @param adminUser
	 */
	Organization createActOrganizationIfDoesNotExist(UserInfo adminUser) {
		// The name is reserved, so only the admin running the bootstrap can create it.
		Organization organization = createOrganizationIfDoesNotExist(adminUser, ORG_SAGEBIONETWORKS_ACT,
				ORG_SAGEBIONETWORKS_ACT_ID);
		grantActTeamAccess(adminUser, organization);
		return organization;
	}

	private Organization createOrganizationIfDoesNotExist(UserInfo adminUser, String name, Long id) {
		try {
			// attempt to get the organization to determine if it exists
			return jsonSchemaManager.getOrganizationByName(adminUser, name);
		} catch (NotFoundException e) {
			// Need to create the organization
			try {
				CreateOrganizationRequest request = new CreateOrganizationRequest();
				request.setOrganizationName(name);
				return jsonSchemaManager.createOrganziation(adminUser, request, id);
			} catch (IllegalArgumentException ex) {
				// The organization was created between our check and insert attempt
				return jsonSchemaManager.getOrganizationByName(adminUser, name);
			}
		}
	}

	/**
	 * Replace the ACT team's entry on the given organization's ACL with the full set of
	 * permissions, leaving every other entry alone. Creates the ACL if the organization does not
	 * have one. Does nothing if the entry is already in place.
	 */
	void grantActTeamAccess(UserInfo adminUser, Organization organization) {
		// The ACL is reached directly rather than through the permission checked JsonSchemaManager
		// methods, because this runs as the stack starts and must not be able to fail: the ACT team
		// holds CHANGE_PERMISSIONS here and so can revoke the admin's own entry, and the ACL may be
		// missing entirely. Either case would otherwise abort the bootstrap.
		Long createdBy = Long.parseLong(organization.getCreatedBy());
		ResourceAccess actEntry = AccessControlListUtil.createResourceAccess(TeamConstants.ACT_TEAM_ID,
				JsonSchemaManagerImpl.ADMIN_PERMISSIONS.toArray(new ACCESS_TYPE[0]));

		Optional<AccessControlList> existingAcl = aclManager.getAcl(organization.getId(), ObjectType.ORGANIZATION);
		if (existingAcl.isEmpty()) {
			AccessControlList acl = AccessControlListUtil.createACL(organization.getId(), adminUser,
					JsonSchemaManagerImpl.ADMIN_PERMISSIONS, new Date());
			acl.getResourceAccess().add(actEntry);
			aclManager.create(adminUser, acl, ObjectType.ORGANIZATION, createdBy);
			return;
		}

		AccessControlList acl = existingAcl.get();
		if (acl.getResourceAccess().contains(actEntry)) {
			return;
		}
		Set<ResourceAccess> resourceAccess = new HashSet<>(acl.getResourceAccess());
		resourceAccess.removeIf(entry -> TeamConstants.ACT_TEAM_ID.equals(entry.getPrincipalId()));
		resourceAccess.add(actEntry);
		aclManager.update(adminUser, acl.setResourceAccess(resourceAccess), ObjectType.ORGANIZATION, createdBy);
	}

	/**
	 * Load the schema that every access requirement schema extends.
	 */
	JsonSchema loadAccessRequirementBaseSchema() {
		// The file is authored as draft-07 and registered verbatim rather than being translated from
		// an ObjectSchema, because the translation cannot express 'required' or 'enum' and injects a
		// 'concreteType' const. This schema depends on the first two, and the third would be
		// inherited by every schema that extends it.
		return translator.loadJsonSchemaFromClasspath(ACCESS_REQUIREMENT_BASE_SCHEMA_FILE);
	}

	/**
	 * Register the given JsonSchema if it does not already exist.
	 * @param admin
	 * @param schema
	 * @throws RecoverableMessageException
	 */
	void registerSchemaIfDoesNotExist(UserInfo admin, JsonSchema schema) throws RecoverableMessageException {
		SchemaId id = SchemaIdParser.parseSchemaId(schema.get$id());
		String organizationName = id.getOrganizationName().toString();
		String schemaName = id.getSchemaName().toString();
		// If we get a patch number then we need to create a new version.
		Optional<Long> optionalPatchNumber = getNextPatchNumberIfNeeded(organizationName, schemaName,
				schema);
		if(!optionalPatchNumber.isPresent()) {
			// the schema is already registered.
			return;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(organizationName);
		builder.append(JsonSchemaConstants.PATH_DELIMITER);
		builder.append(schemaName);
		builder.append(JsonSchemaConstants.VERSION_PRFIX);
		builder.append("1.0.");
		builder.append(optionalPatchNumber.get());
		CreateSchemaRequest request = new CreateSchemaRequest();
		schema.set$id(builder.toString());
		request.setSchema(schema);
		jsonSchemaManager.createJsonSchema(admin, request);
	}

	/**
	 * Use the latest version of each referenced schema.
	 * @param schema
	 */
	void replaceReferencesWithLatestVersion(JsonSchema schema) {
		for (JsonSchema subSchema : SubSchemaIterable.depthFirstIterable(schema)) {
			if (subSchema.get$ref() != null) {
				SchemaId refId = SchemaIdParser.parseSchemaId(subSchema.get$ref());
				if (refId.getSemanticVersion() == null) {
					JsonSchemaVersionInfo versionInfo = jsonSchemaManager
							.getLatestVersion(refId.getOrganizationName().toString(), refId.getSchemaName().toString());
					subSchema.set$ref(versionInfo.get$id());
				}
			}
		}
	}

	/**
	 * Lookup the current version of this schema. If the current version exists, and
	 * has the same SHA256 then there is no need to create a new version and an
	 * empty Optional will be returned. If a version exists but the SHA256 does not
	 * match the patch number of the current version plus one will be returned. If
	 * the schema does not exist at all a patch number of zero will be returned.
	 *
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	Optional<Long> getNextPatchNumberIfNeeded(String organizationName, String schemaName, JsonSchema testSchema) {
		try {
			JsonSchemaVersionInfo currentVersion = jsonSchemaManager.getLatestVersion(organizationName, schemaName);
			// would the two schemas match if they had the same id?
			testSchema.set$id(currentVersion.get$id());
			NormalizedJsonSchema normalizedJsonSchema = new NormalizedJsonSchema(testSchema);
			if (currentVersion.getJsonSHA256Hex().equals(normalizedJsonSchema.getSha256Hex())) {
				// this schema has already been registered. Empty patch number to signal no new
				// version needed.
				return Optional.empty();
			}
			SemanticVersion currentSemanticVersion = new SchemaIdParser(currentVersion.getSemanticVersion())
					.semanticVersion();
			// bump the patch version by one.
			Long patchNumber = currentSemanticVersion.getCore().getPatch().getValue() + 1L;
			return Optional.of(patchNumber);
		} catch (NotFoundException e) {
			// This will be the first version of this schema, so start at patch zero.
			return Optional.of(0L);
		} catch (ParseException | TokenMgrError e) {
			// This parser is invoked directly rather than through SchemaIdParser.parseSchemaId, so it
			// must guard against TokenMgrError itself.
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Load all of the given schemas and their dependencies. Dependencies will be
	 * listed before the objects that depend on them.
	 *
	 * @param schemaClassNames
	 * @return
	 */
	List<ObjectSchema> loadAllSchemasAndReferences(List<String> schemaClassNames) {
		Set<String> visitedIds = new HashSet<String>();
		Map<String, ObjectSchema> loadedSchemas = new LinkedHashMap<>();
		for (String idToLoad : schemaClassNames) {
			ObjectSchema schema = translator.loadSchemaFromClasspath(idToLoad);
			loadAllSchemasRecursive(loadedSchemas, visitedIds, schema);
		}
		return loadedSchemas.values().stream().collect(Collectors.toList());
	}


	/**
	 * Depth-first recursive walk of the entire schema hierarchy, so dependencies
	 * are added before the object that depend on them.
	 *
	 * @param loadedSchemas a mapping of loaded schema class names to the ObjectSchema
	 * @param visitedIds    the set of visited schema classes to prevent infinite loops
	 * @param schema        the schema to walk through
	 */
	private void loadAllSchemasRecursive(Map<String, ObjectSchema> loadedSchemas, Set<String> visitedIds, ObjectSchema schema) {
		String schemaId = schema.getId();
		if (schemaId != null) {
			if (loadedSchemas.containsKey(schemaId)) {
				// schema is already loaded.
				return;
			}
			// loop detection
			if (!visitedIds.add(schemaId)) {
				return;
			}
		}

		// If this schema has a ref, then load the referenced schema recursively.
		if (schema.getRef() != null) {
			ObjectSchema referencedSchema = translator.loadSchemaFromClasspath(schema.getRef());
			loadAllSchemasRecursive(loadedSchemas, visitedIds, referencedSchema);
		}

		// Walk through the schema to continue loading all references
		schema.getSubSchemaIterator().forEachRemaining((ObjectSchema subSchema) ->
				loadAllSchemasRecursive(loadedSchemas, visitedIds, subSchema)
		);


		// Add the schema to the end of the insertion-ordered loaded schemas map (ensure the references are added first)
		if (schemaId != null) {
			loadedSchemas.put(schemaId, schema);
		}
	}

}
