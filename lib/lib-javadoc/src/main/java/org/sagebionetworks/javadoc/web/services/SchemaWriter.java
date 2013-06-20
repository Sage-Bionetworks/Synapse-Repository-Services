package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.javadoc.BasicFileUtils;
import org.sagebionetworks.javadoc.linker.FileLink;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

/**
 * Write all of the schema files.
 * @author John
 *
 */
public class SchemaWriter implements FileWriter {
	
	
	/**
	 * Find the schemas used by the method and add them to the set.
	 * @param set
	 * @param method
	 */
	public static void findSchemaFiles(Map<String, ObjectSchema> schemaMap, MethodDoc method){
		// A schema class can be used for the return type or a parameters
		Type returnType = method.returnType();
		ClassDoc returnClassDoc = returnType.asClassDoc();
		if(returnClassDoc != null){
			// Get the full name
			recursiveAddSubTypes(schemaMap, returnClassDoc);
		}
		// Apply the same test to all parameters
		Parameter[] params = method.parameters();
		if(params != null){
			for(Parameter param: params){
				ClassDoc paramClass = param.type().asClassDoc();
				recursiveAddSubTypes(schemaMap, paramClass);
			}
		}
	}

	private static void recursiveAddSubTypes(Map<String, ObjectSchema> schemaMap, ClassDoc paramClass) {
		if(implementsJSONEntity(paramClass)){
			// Lookup the schema and add sub types.
			recursiveAddTypes(schemaMap, paramClass.qualifiedName(), null);
		}
	}
	
	private static void recursiveAddTypes(Map<String, ObjectSchema> schemaMap,	String id, ObjectSchema schema) {
		if(!schemaMap.containsKey(id)){
			if(schema == null){
				schema = getSchema(id);
			}
			schemaMap.put(id, schema);
			Iterator<ObjectSchema> it = schema.getSubSchemaIterator();
			while (it.hasNext()) {
				ObjectSchema sub = it.next();
				if (sub.getId() != null) {
					recursiveAddTypes(schemaMap, sub.getId(), sub);
				}
			}
		}
	}
	
	/**
	 * Does the given class implement JSONEntity.
	 * @param classDoc
	 * @return
	 */
	public static boolean implementsJSONEntity(ClassDoc classDoc){
		// primitives will not have a class and do not implement JSONEntity
		if(classDoc == null) return false;
		ClassDoc[] interfaces = classDoc.interfaces();
		if(interfaces != null){
			for(ClassDoc doc: interfaces){
				if(JSONEntity.class.getName().equals(doc.qualifiedName())){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Get the effective schema for a class.
	 * @param name
	 * @return
	 */
	public static String getEffectiveSchema(String name) {
		Class<JSONEntity> clazz;
		try {
			clazz = (Class<JSONEntity>) Class.forName(name);
			JSONEntity entity = clazz.newInstance();
			return entity.getJSONSchema();
		} catch (Exception e) {
			return null;
		} 
	}
	
	/**
	 * Get the schema for a class.
	 * @param name
	 * @return
	 */
	public static ObjectSchema getSchema(String name){
		try {
			String json = getEffectiveSchema(name);
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl(json);
			ObjectSchema schema = new ObjectSchema(adpater);
			return schema;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}


	@Override
	public List<FileLink> writeAllFiles(File outputDirectory, RootDoc root) throws Exception {
		Map<String, ObjectSchema> schemaMap = new HashMap<String, ObjectSchema>();
		// First find all of the JSONEntity names
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();
        	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(classDoc.methods());
        	while(methodIt.hasNext()){
        		MethodDoc methodDoc = methodIt.next();
        		findSchemaFiles(schemaMap, methodDoc);
        	}
        }
        // Render each schema
        Iterator<String> it = schemaMap.keySet().iterator();
        List<FileLink> results = new LinkedList<FileLink>();
        while(it.hasNext()){
        	String name = it.next();
        	ObjectSchema schema = schemaMap.get(name);
        	// Create a file for this schema
        	File schemaFile = BasicFileUtils.createNewFileForClassName(outputDirectory, name, "html");
        	FileLink link = new FileLink(schemaFile, name);
        	results.add(link);
        	// Write this file
        	SchemaHTMLWriter.write(schemaFile, schema, name);
        }
        return results;
	}
}
