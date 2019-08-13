package org.sagebionetworks.javadoc.velocity.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.javadoc.RootDoc;
import org.apache.velocity.context.Context;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextFactory;
import org.sagebionetworks.schema.ObjectSchema;

/**
 * 
 * @author John
 *
 */
public class SchemaClassContextGenerator implements ClassContextGenerator{

	@Override
	public List<ClassContext> generateContext(ContextFactory factory, RootDoc root) throws Exception {
		Map<String, ObjectSchema> schemaMap = new HashMap<String, ObjectSchema>();
		// First find all of the JSONEntity names
		SchemaUtils.findSchemaFiles(schemaMap, root);
		
		Map<String, List<TypeReference>> knownImplementaions = SchemaUtils.mapImplementationsToIntefaces(schemaMap);
        // Render each schema
        List<ClassContext> results = new ArrayList<>(schemaMap.size());
        for(String name: schemaMap.keySet()){
        	ObjectSchema schema = schemaMap.get(name);
        	List<TypeReference> implementations = knownImplementaions.get(name);
        	// Translate the schema to a model
        	ObjectSchemaModel model = SchemaUtils.translateToModel(schema, implementations);
        	// Create a context and add the model
        	Context context = factory.createNewContext();
        	context.put("model", model);
        	ClassContext classContext = new ClassContext(name, "schemaHTMLTemplate.html", context);
        	results.add(classContext);
        }
        return results;
	}

}
