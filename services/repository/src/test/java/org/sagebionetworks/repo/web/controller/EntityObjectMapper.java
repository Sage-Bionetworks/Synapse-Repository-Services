package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.Writer;


import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Simple Object Mapper to replace ObjectMapper.
 * @author John
 *
 */
public class EntityObjectMapper {
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	
	/**
	 * Write the passed entity to the writer
	 * @param w
	 * @param entity
	 * @throws JSONObjectAdapterException
	 * @throws IOException
	 */
	public void writeValue(Writer w, JSONEntity entity) {
		JSONObjectAdapter adapter;
		try {
			adapter = entity.writeToJSONObject(new JSONObjectAdapterImpl());
			w.write(adapter.toJSONString());
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeValue(Writer w, Object entity) throws JsonGenerationException, JsonMappingException, IOException {
		JSONObjectAdapter adapter;
		objectMapper.writeValue(w, entity);
	}
	
	public static boolean isJSONEntity(Class<?> clazz){
		Class[] interfaces = clazz.getInterfaces();
		if(interfaces == null) return false;
		if(interfaces.length < 1) return false;
		for(Class c: interfaces){
			if(JSONEntity.class.equals(c)) return true;
		}
		return false;
	}
	
	/**
	 * Create a new entity from the passed json string.
	 * @param json
	 * @param clazz
	 * @return
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public <T> T readValue(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException{
		if(isJSONEntity(clazz)){
			try {
				JSONEntity entity = (JSONEntity) clazz.newInstance();
				entity.initializeFromJSONObject(new JSONObjectAdapterImpl(json));
				return (T) entity;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}else{
			return objectMapper.readValue(json, clazz);
		}

	}
	

}
