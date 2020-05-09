package org.sagebionetworks.repo.model.schema;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides an iterator over all sub-schemas for the given schema.
 *
 */
public class SubSchemaIterable implements Iterable<JsonSchema> {

	private final JsonSchema root;

	public SubSchemaIterable(JsonSchema root) {
		this.root = root;
	}

	@Override
	public Iterator<JsonSchema> iterator() {
		List<JsonSchema> list = new LinkedList<JsonSchema>();
		try {
			Field[] fields = JsonSchema.class.getDeclaredFields();
			for (Field field : fields) {
				if(!field.isAccessible()){
					field.setAccessible(true);
				}
				// is it a schema
				if(JsonSchema.class == field.getType()) {
					JsonSchema schema = (JsonSchema) field.get(root);
					if(schema != null) {
						list.add(schema);
					}
				}
				// is it a list of schemas?
				if(List.class == field.getType()) {
					List<JsonSchema> schemaList = (List<JsonSchema>) field.get(root);
					if(schemaList != null) {
						for(JsonSchema schema: schemaList) {
							if(schema != null) {
								list.add(schema);
							}
						}
					}
				}
				// is it a map of schemas
				if(Map.class == field.getType()) {
					Map<String, JsonSchema> schemaMap = (Map<String, JsonSchema>) field.get(root);
					if(schemaMap != null) {
						for(Entry<String, JsonSchema> entry: schemaMap.entrySet()) {
							if(entry.getValue() != null) {
								list.add(entry.getValue());
							}
						}
					}
				}
			}
			return list.iterator();
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} 
	}
}
