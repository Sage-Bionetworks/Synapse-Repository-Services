package org.sagebionetworks.javadoc.velocity.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.context.Context;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextInput;
import org.sagebionetworks.schema.ObjectSchema;

/**
 * 
 * @author John
 *
 */
public class SchemaClassContextGenerator implements ClassContextGenerator{

	@Override
	public List<ClassContext> generateContext(ContextInput input) throws Exception {
		Map<String, ObjectSchema> schemaMap = new HashMap<String, ObjectSchema>();
		// First find all of the JSONEntity names
		SchemaUtils.findSchemaFiles(schemaMap, input.getDocletEnvironment());
		
		Map<String, List<TypeReference>> knownImplementaions = SchemaUtils.mapImplementationsToIntefaces(schemaMap);
        // Render each schema
        List<ClassContext> results = new ArrayList<>(schemaMap.size());
        for(String name: schemaMap.keySet()){
        	ObjectSchema schema = schemaMap.get(name);
        	List<TypeReference> implementations = knownImplementaions.get(name);
        	// Translate the schema to a model
        	ObjectSchemaModel model = SchemaUtils.translateToModel(schema, implementations);
        	// Create a context and add the model
        	Context context = input.getContextFactory().createNewContext();
        	context.put("model", model);
        	ClassContext classContext = new ClassContext(name, "schemaHTMLTemplate.html", context);
        	results.add(classContext);
        }
        return results;
	}


}
