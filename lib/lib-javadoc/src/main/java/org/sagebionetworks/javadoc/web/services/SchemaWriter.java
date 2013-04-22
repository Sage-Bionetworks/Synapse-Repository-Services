package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sagebionetworks.javadoc.FileUtils;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import com.sun.javadoc.Parameter;

/**
 * Write all of the schema files.
 * @author John
 *
 */
public class SchemaWriter {
	
	
	Set<String> jsonEntityNames;
	
	/**
	 * 
	 * @param root
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws JSONObjectAdapterException 
	 */
	public SchemaWriter(File outputDirectory, RootDoc root) throws IOException, XMLStreamException, JSONObjectAdapterException {
		jsonEntityNames = new HashSet<String>();
		// First find all of the JSONEntity names
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();
        	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(classDoc.methods());
        	while(methodIt.hasNext()){
        		MethodDoc methodDoc = methodIt.next();
        		findSchemaFiles(jsonEntityNames, methodDoc);
        	}
        }
        System.out.println(jsonEntityNames);
        // Render each schema
        Iterator<String> it = jsonEntityNames.iterator();
        while(it.hasNext()){
        	String name = it.next();
        	String json = getEffectiveSchema(name);
        	// Create a file for this schema
        	File schemaFile = FileUtils.createNewFileForClassName(outputDirectory, name, "html");
        	// Write this file
        	SchemaHTMLWriter.write(schemaFile, json, name);
        }
	}

	
	/**
	 * Find the schemas used by the method and add them to the set.
	 * @param set
	 * @param method
	 */
	public static void findSchemaFiles(Set<String> set, MethodDoc method){
		// A schema class can be used for the return type or a parameters
		Type returnType = method.returnType();
		ClassDoc returnClassDoc = returnType.asClassDoc();
		if(returnClassDoc != null){
			// Get the full name
			if(implementsJSONEntity(returnClassDoc)){
				set.add(returnClassDoc.qualifiedName());
			}
		}
		// Apply the same test to all parameters
		Parameter[] params = method.parameters();
		if(params != null){
			for(Parameter param: params){
				ClassDoc paramClass = param.type().asClassDoc();
				if(implementsJSONEntity(paramClass)){
					set.add(paramClass.qualifiedName());
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
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
