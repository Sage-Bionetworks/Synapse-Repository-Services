package org.sagebionetworks.translator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.ObjectSchema;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class ControllersToOpenAPIJsonTranslator {
	
	/**
	 * Goes through all controllers found in DocletEnvironment and outputs a JSONObject that
	 * represents all of the controllers and complies to the OpenAPI specification.
	 * 
	 * @param env the doclet environment
	 * @return JSONObject that represents all of the controllers.
	 */
	public JSONObject translate(DocletEnvironment env, Iterator<String> concreteClassNames, Reporter reporter) {
		ObjectSchemaUtils objectSchemaUtils = new ObjectSchemaUtils();
		
		Map<String, ObjectSchema> classNameToObjectSchema = objectSchemaUtils.getConcreteClasses(concreteClassNames);
		List<ControllerModel> controllerModels = new ControllerToControllerModelTranslator().extractControllerModels(env, classNameToObjectSchema, reporter);
		Map<String, JsonSchema> classNameToJsonSchema = objectSchemaUtils.getClassNameToJsonSchema(classNameToObjectSchema);

		return new ControllerModelsToOpenAPIModelTranslator(classNameToJsonSchema).translate(controllerModels, reporter).generateJSON();
	}
}
