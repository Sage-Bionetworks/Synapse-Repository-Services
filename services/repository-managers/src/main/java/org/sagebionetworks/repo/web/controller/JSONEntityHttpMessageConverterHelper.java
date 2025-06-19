package org.sagebionetworks.repo.web.controller;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.converter.HttpMessageNotReadableException;

public class JSONEntityHttpMessageConverterHelper {
	private static final String CONCRETE_TYPE = "concreteType";
	private static final String ENTITY_TYPE = "entityType";
	private static final String VALIDATION_ERROR = "JSON Element in Entity is Unsupported: %s";
	private static final String MISSING_ELEMENT_ERROR = "Missing element in child array of %s element on conversion";
	
	// This is specified by HTTP 1.1
	public static final Charset HTTP_1_1_DEFAULT_CHARSET = Charset.forName("ISO-8859-1");
	
	public static JSONEntity read(String jsonString, Charset charset, Class<? extends JSONEntity> clazz, Set<Class <? extends JSONEntity>> classesToValidateConversion)
			throws IOException, HttpMessageNotReadableException {
		try {
			JSONEntity entity = EntityFactory.createEntityFromJSONString(jsonString, clazz);
			// validate the entity if its class is one which we should validate
			if (classesToValidateConversion.contains(clazz)) {
				validateJSONEntity(entity, jsonString);
			}
			return entity;
		} catch (JSONObjectAdapterException e) {
			// Try to convert entity type to a concrete type and try again. See PLFM-2079.
			try {
				JSONObject jsonObject = new JSONObject(jsonString);
				if(jsonObject.has(ENTITY_TYPE)){
					// get the entity type so we can replace it with concrete type
					String type = jsonObject.getString(ENTITY_TYPE);
				jsonObject.remove(ENTITY_TYPE);
					jsonObject.put(CONCRETE_TYPE, type);
					jsonString = jsonObject.toString();
					// try again
					return EntityFactory.createEntityFromJSONString(jsonString, clazz);
				}else{
					// Something else went wrong
					throw new HttpMessageNotReadableException(e.getMessage(), e);
				}
			} catch (JSONException e1) {
				throw new HttpMessageNotReadableException(e1.getMessage(), e);
			} catch (JSONObjectAdapterException e2) {
				throw new HttpMessageNotReadableException(e2.getMessage(), e);
			}
		}
	}

	/**
	 * Read a string from an input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(InputStream in, Charset charSet)
			throws IOException {
		if(in == null) throw new IllegalArgumentException("No content to map to Object due to end of input");
		try {
			if(charSet == null) {
				charSet = HTTP_1_1_DEFAULT_CHARSET;
			}
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, charSet));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
	public static void validateJSONEntity(JSONEntity parsedEntity, String originalJsonString) 
			throws JSONObjectAdapterException {
		// Validating: throws an IllegalArgumentException if the parsedEntity is missing
		// an element from the originalJsonString
		JSONObject parsedEntityJsonObject = EntityFactory.createJSONObjectForEntity(parsedEntity);
		JSONObjectAdapter parsedObject = new JSONObjectAdapterImpl(parsedEntityJsonObject);
		JSONObjectAdapter originalObject = new JSONObjectAdapterImpl(originalJsonString);
		validateJSONEntityRecursive(parsedObject, originalObject);
	}
	
	public static void validateJSONEntityRecursive(JSONObjectAdapter parsedObject, 
			JSONObjectAdapter originalObject) throws JSONObjectAdapterException {
		// throws an IllegalArgumentException if the parsedObject is missing a key that
		// the original object has.
		for (String key : originalObject.keySet()) {
			Object object = originalObject.get(key);
			if (!parsedObject.has(key)) {
				// element is missing, therefore unsupported
				throw new IllegalArgumentException(String.format(VALIDATION_ERROR, key));
			} else if (object instanceof JSONObjectAdapterImpl) {
				// JSON object, so we recurse
				JSONObjectAdapter objectAdapter = (JSONObjectAdapterImpl) object;
				JSONObjectAdapter parsedObjectAdapter = parsedObject.getJSONObject(key);
				validateJSONEntityRecursive(parsedObjectAdapter, objectAdapter);
			} else if (object instanceof JSONArrayAdapterImpl) {
				// if array object, recursively handle it
				JSONArrayAdapter originalArray = (JSONArrayAdapterImpl) object;
				JSONArrayAdapter parsedArray = parsedObject.getJSONArray(key);
				validateJSONArrayRecursive(parsedArray, originalArray, key);
			}
		}
	}

	public static void validateJSONArrayRecursive(JSONArrayAdapter parsedArray, 
			JSONArrayAdapter originalArray, String key) throws JSONObjectAdapterException {
		/*
		 * NOTE: we pass in the key as well to indicate the closest parent key of an invalid array
		 * conversion (throws exception on unequal array sizes). this is because we can have a key 
		 * mapping to an array of arrays of arrays, in which the embedded arrays do not have an 
		 * immediate key mapping that we can report. So we should report the closest parent key.
		 */
		if (originalArray.length() != parsedArray.length()) {
			throw new IllegalArgumentException(String.format(MISSING_ELEMENT_ERROR, key));
		}
		for (int i = 0; i < originalArray.length(); i++) {
			// get each element, and recurse accordingly if they are JSONObject or JSONArray
			Object parsedElement = parsedArray.get(i);
			Object originalElement = originalArray.get(i);
			if (parsedElement instanceof JSONObject && originalElement instanceof JSONObject) {
				JSONObjectAdapter originalObjectElement = new JSONObjectAdapterImpl((JSONObject) originalElement);
				JSONObjectAdapter parsedObjectElement = new JSONObjectAdapterImpl((JSONObject) parsedElement);
				validateJSONEntityRecursive(parsedObjectElement, originalObjectElement);
			} else if (parsedElement instanceof JSONArray && originalElement instanceof JSONArray) {
				JSONArrayAdapter nextParsedArray = new JSONArrayAdapterImpl((JSONArray) parsedElement);
				JSONArrayAdapter nextOriginalArray = new JSONArrayAdapterImpl((JSONArray) originalElement);
				validateJSONArrayRecursive(nextParsedArray, nextOriginalArray, key);
			}
		}
	}

	/**
	 * Read a string from an input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readToString(Reader reader) throws IOException {
		if(reader == null) throw new IllegalArgumentException("Reader cannot be null");
		try {
			char[] buffer = new char[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, index);
			}
			return builder.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Read an entity from the reader.
	 * @param reader
	 * @return
	 * @throws IOException 
	 * @throws JSONObjectAdapterException 
	 */
	public static Entity readEntity(Reader reader) throws IOException, JSONObjectAdapterException {
		// First read in the string
		String jsonString = readToString(reader);
		// Read it into an adapter
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonString);
		return createEntityFromAdapter(adapter);
	}

	/**
	 * There are many things that can go wrong with this and we want to make sure the error messages
	 * are always meaningful.
	 * @param adapter
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static Entity createEntityFromAdapter(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException {
		// Get the entity type
		String typeClassName = adapter.getString("concreteType");
		if(typeClassName==null){
			throw new IllegalArgumentException("Cannot determine the entity type.  The entityType property is null");
		}
		// Create a new instance using the full class name
		Entity newInstance = null;
		try {
			// 
			Class<? extends Entity> entityClass = (Class<? extends Entity>) Class.forName(typeClassName);
			newInstance = entityClass.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unknown entity type: "+typeClassName+". Message: "+e.getMessage());
		}
		// Populate the new instance with the JSON.
		newInstance.initializeFromJSONObject(adapter);
		return newInstance;
	}

}
