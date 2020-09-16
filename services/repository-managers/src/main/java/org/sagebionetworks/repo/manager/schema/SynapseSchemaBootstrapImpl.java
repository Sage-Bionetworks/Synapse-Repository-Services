package org.sagebionetworks.repo.manager.schema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.NormalizedJsonSchema;
import org.sagebionetworks.repo.model.schema.SubSchemaIterable;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.semantic.version.SemanticVersion;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class SynapseSchemaBootstrapImpl implements SynapseSchemaBootstrap {

	public static final String ORG_SAGEBIONETWORKS = "org.sagebionetworks";

	/**
	 * The Synapse objects that can be referenced in JSON schemas and therefore must
	 * exist in the repository.
	 */
	public static final List<String> OBJECTS_TO_BOOTSTRAP = Lists.newArrayList(FileEntity.class.getName(),
			Folder.class.getName(), Project.class.getName());

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	SchemaTranslator translator;

	@Override
	public void bootstrapSynapseSchemas() throws RecoverableMessageException {
		// The process is run as the Synapse admin
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		createOrganizationIfDoesNotExist(adminUser);

		List<ObjectSchema> allSchemasToBootstrap = loadAllSchemasAndReferences(OBJECTS_TO_BOOTSTRAP);

		for (ObjectSchema objectSchema : allSchemasToBootstrap) {
			JsonSchema jsonSchema = translator.translate(objectSchema);
			replaceReferencesWithLatestVersion(jsonSchema);
			registerSchemaIfDoesNotExist(adminUser, jsonSchema);
		}
	}

	/**
	 * Create the 'org.sagebionetworks' organization if it does not already exists
	 * @param adminUser
	 */
	void createOrganizationIfDoesNotExist(UserInfo adminUser) {
		try {
			// attempt to get the organization to determine if it exists
			jsonSchemaManager.getOrganizationByName(adminUser, ORG_SAGEBIONETWORKS);
		} catch (NotFoundException e) {
			// Need to create the organization
			CreateOrganizationRequest request = new CreateOrganizationRequest();
			request.setOrganizationName(ORG_SAGEBIONETWORKS);
			jsonSchemaManager.createOrganziation(adminUser, request);
		}
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
	 * @param jsonSHA256Hex
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
		} catch (ParseException e) {
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
		Set<String> visitedId = new HashSet<String>();
		Map<String, ObjectSchemaImpl> loadedSchemas = new LinkedHashMap<String, ObjectSchemaImpl>();
		for (String idToLoad : schemaClassNames) {
			loadAllSchemasRecursive(loadedSchemas, idToLoad, visitedId);
		}
		return loadedSchemas.values().stream().collect(Collectors.toList());
	}

	/**
	 * Depth-first recursive walk of the entire schema hierarchy, so dependencies
	 * are added before the object that depend on them.
	 * 
	 * @param loadedSchemas
	 * @param schemaClassName
	 */
	private void loadAllSchemasRecursive(Map<String, ObjectSchemaImpl> loadedSchemas, String schemaClassName,
			Set<String> visitedId) {
		if (schemaClassName == null || loadedSchemas.containsKey(schemaClassName)) {
			// schema is already loaded.
			return;
		}
		// loop detection
		if (!visitedId.add(schemaClassName)) {
			return;
		}
		ObjectSchemaImpl loaded = translator.loadSchemaFromClasspath(schemaClassName);
		// load all of the references of this schema
		loaded.getSubSchemaIterator().forEachRemaining((ObjectSchema subSchema) -> {
			loadAllSchemasRecursive(loadedSchemas, subSchema.getRef(), visitedId);
		});
		loadedSchemas.put(schemaClassName, loaded);
	}

}
