package org.sagebionetworks.javadoc.velocity.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.velocity.context.Context;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextFactory;
import org.sagebionetworks.javadoc.web.services.FilterUtils;
import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

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
//		AutoGenFactory autoGen = new AutoGenFactory();
//		Iterator<String> keySet = autoGen.getKeySetIterator();
//		while(keySet.hasNext()){
//			String name = keySet.next();
//			ObjectSchema schema = SchemaUtils.getSchema(name);
//			SchemaUtils.recursiveAddTypes(schemaMap, name, schema);
//		}
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();
        	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(classDoc.methods());
        	while(methodIt.hasNext()){
        		MethodDoc methodDoc = methodIt.next();
        		SchemaUtils.findSchemaFiles(schemaMap, methodDoc);
        	}
        }
        // Render each schema
        Iterator<String> it = schemaMap.keySet().iterator();
        List<ClassContext> results = new LinkedList<ClassContext>();
        while(it.hasNext()){
        	String name = it.next();
        	ObjectSchema schema = schemaMap.get(name);
        	// Translate the schema to a model
        	ObjectSchemaModel model = SchemaUtils.translateToModel(schema);
        	// Create a context and add the model
        	Context context = factory.createNewContext();
        	context.put("model", model);
        	ClassContext classContext = new ClassContext(name, "schemaHTMLTemplate.html", context);
        	results.add(classContext);
        }
        return results;
	}

}
