package org.sagebionetworks.repo.manager.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class SynapseSchemaBootstrapImpl implements SynapseSchemaBootstrap {

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
	public void bootstrapSynapseSchemas() {
		// The process is run as the Synapse admin
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		List<ObjectSchemaImpl> allSchemasToBootstrap = loadAllSchemasAndReferences(OBJECTS_TO_BOOTSTRAP);
		
	}

	/**
	 * Load all of the given schemas and their dependencies. Dependencies will be
	 * listed before the objects that depend on them.
	 * 
	 * @param schemaClassNames
	 * @return
	 */
	List<ObjectSchemaImpl> loadAllSchemasAndReferences(List<String> schemaClassNames) {
		Map<String, ObjectSchemaImpl> loadedSchemas = new LinkedHashMap<String, ObjectSchemaImpl>();
		for (String idToLoad : schemaClassNames) {
			loadAllSchemasRecursive(loadedSchemas, idToLoad);
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
	private void loadAllSchemasRecursive(Map<String, ObjectSchemaImpl> loadedSchemas, String schemaClassName) {
		if (schemaClassName == null || loadedSchemas.containsKey(schemaClassName)) {
			// schema is already loaded.
			return;
		}
		ObjectSchemaImpl loaded = translator.loadSchemaFromClasspath(schemaClassName);
		// load all of the references of this schema
		loaded.getSubSchemaIterator().forEachRemaining((ObjectSchema subSchema) -> {
			loadAllSchemasRecursive(loadedSchemas, subSchema.getRef());
		});
		loadedSchemas.put(schemaClassName, loaded);
	}

}
