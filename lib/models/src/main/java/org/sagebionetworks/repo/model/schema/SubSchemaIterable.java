package org.sagebionetworks.repo.model.schema;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Provides iterators over the sub-schema of JsonSchemas.
 *
 */
public class SubSchemaIterable {

	/**
	 * Create a depth first iterator for the given schema.
	 * 
	 * @param root
	 * @return
	 */
	public static Iterable<JsonSchema> depthFirstIterable(JsonSchema root) {
		ValidateArgument.required(root, "JsonSchema");
		return () -> createDepthFirstList(root).iterator();
	}

	/**
	 * Create a flat list including only the direct children of the given schema.
	 * This method is not recursive.
	 * 
	 * @param root
	 * @return
	 */
	static List<JsonSchema> createListOfChildren(JsonSchema root) {
		List<JsonSchema> list = new LinkedList<JsonSchema>();
		try {
			Field[] fields = JsonSchema.class.getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}
				// is it a schema
				if (JsonSchema.class == field.getType()) {
					JsonSchema schema = (JsonSchema) field.get(root);
					if (schema != null) {
						list.add(schema);
					}
				}
				// is it a list of schemas?
				if (List.class == field.getType()) {
					ParameterizedType paramType = (ParameterizedType) field.getGenericType();
					if (JsonSchema.class == paramType.getActualTypeArguments()[0]) {
						List<JsonSchema> schemaList = (List<JsonSchema>) field.get(root);
						if (schemaList != null) {
							for (JsonSchema schema : schemaList) {
								if (schema != null) {
									list.add(schema);
								}
							}
						}
					}
				}
				// is it a map of schemas
				if (Map.class == field.getType()) {
					ParameterizedType paramType = (ParameterizedType) field.getGenericType();
					if (String.class == paramType.getActualTypeArguments()[0]
							&& JsonSchema.class == paramType.getActualTypeArguments()[1]) {
						Map<String, JsonSchema> schemaMap = (Map<String, JsonSchema>) field.get(root);
						if (schemaMap != null) {
							for (Entry<String, JsonSchema> entry : schemaMap.entrySet()) {
								if (entry.getValue() != null) {
									list.add(entry.getValue());
								}
							}
						}
					}
				}
			}
			return list;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Recursive method to create a list grandchildren followed by children of the
	 * given root schema.
	 * 
	 * @param root
	 * @return
	 */
	static List<JsonSchema> createDepthFirstList(JsonSchema root) {
		List<JsonSchema> depthFirstResult = new LinkedList<JsonSchema>();
		List<JsonSchema> children = createListOfChildren(root);
		for (JsonSchema child : children) {
			List<JsonSchema> recursiveGrandChildren = createDepthFirstList(child);
			depthFirstResult.addAll(recursiveGrandChildren);
		}
		depthFirstResult.addAll(children);
		return depthFirstResult;
	}

}
