package org.sagebionetworks.translator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.ObjectSchema;

import jdk.javadoc.doclet.DocletEnvironment;

public class ControllersToOpenAPIJsonTranslator {
	
	/**
	 * Goes through all controllers found in DocletEnvironment and outputs a JSONObject that
	 * represents all of the controllers and complies to the OpenAPI specification.
	 * 
	 * @param env the doclet environment
	 * @return JSONObject that represents all of the controllers.
	 */
	public JSONObject translate(DocletEnvironment env, Iterator<String> concreteClassNames) {
		ObjectSchemaUtils objectSchemaUtils = new ObjectSchemaUtils();
		
		Map<String, ObjectSchema> classNameToObjectSchema = objectSchemaUtils.getConcreteClasses(concreteClassNames);
		System.out.println("class name to ObjectSchema keys " + classNameToObjectSchema.keySet());
		List<ControllerModel> controllerModels = new ControllerToControllerModelTranslator().extractControllerModels(env, classNameToObjectSchema);
		System.out.println("class name to ObjectSchema keys " + classNameToObjectSchema.keySet());
		System.out.println("controller models " + controllerModels);
		Map<String, JsonSchema> classNameToJsonSchema = objectSchemaUtils.getClassNameToJsonSchema(classNameToObjectSchema);
		System.out.println("class name to JsonSchema keys " + classNameToObjectSchema.keySet());

		return new ControllerModelsToOpenAPIModelTranslator(classNameToJsonSchema).translate(controllerModels).generateJSON();
	}
}
