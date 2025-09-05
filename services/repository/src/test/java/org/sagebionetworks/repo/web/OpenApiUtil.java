package org.sagebionetworks.repo.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A utility that will extract a single controller from an Open API json file.
 * The resulting Open API file will contain only the methods of the provided
 * controller plus all dependent components.
 */
public class OpenApiUtil {

	public static void main(String[] args) throws IOException {
		if (args == null || args.length != 3) {
			throw new IllegalArgumentException("First argument should be the path to the original Open API file.\n"
					+ " The second argument should be path the destination file.\n"
					+ " The third argument should be the 'displayName' from the controller's info (tags value).");
		}
		String controllerTag = args[2];
		JSONObject source = new JSONObject(Files.readString(Paths.get(args[0]), StandardCharsets.UTF_8));

		JSONObject paths = source.getJSONObject("paths");
		JSONObject newPaths = new JSONObject();

		asStream(paths.keys()).filter(pathKey -> hasMatchingTag(paths.getJSONObject(pathKey), controllerTag))
				.forEach(pathKey -> newPaths.put(pathKey, paths.get(pathKey)));

		Set<String> referencedSchemas = collectAllReferencedSchemas(source, newPaths);
		JSONObject schemas = extractSchemas(source, referencedSchemas);

		JSONObject result = new JSONObject();
		result.put("openapi", "3.0.1");
		JSONObject info = new JSONObject();
		info.put("title", "...");
		info.put("version", "v1");
		
		result.put("info", info);
		
		result.put("paths", newPaths);
		JSONObject components = new JSONObject();
		components.put("schemas", schemas);
		result.put("components", components);

		System.out.println(result.toString(5));
		Files.writeString(Paths.get(args[1]), result.toString(5));
	}

	private static boolean hasMatchingTag(JSONObject pathItem, String controllerTag) {
		String[] httpMethods = { "get", "post", "put", "delete" };

		for (String method : httpMethods) {
			if (pathItem.has(method)) {
				JSONObject operation = pathItem.getJSONObject(method);
				if (operation.has("tags")) {
					JSONArray tags = operation.getJSONArray("tags");
					for (int i = 0; i < tags.length(); i++) {
						if (controllerTag.equals(tags.getString(i))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private static Set<String> collectAllReferencedSchemas(JSONObject source, JSONObject paths) {
		Set<String> allSchemas = new HashSet<>();
		Set<String> directSchemas = new HashSet<>();

		collectSchemasFromObject(paths, directSchemas);

		if (!source.has("components") || !source.getJSONObject("components").has("schemas")) {
			return directSchemas;
		}

		JSONObject sourceSchemas = source.getJSONObject("components").getJSONObject("schemas");
		Set<String> processed = new HashSet<>();

		for (String schemaName : directSchemas) {
			collectSchemaRecursively(sourceSchemas, schemaName, allSchemas, processed);
		}

		return allSchemas;
	}

	private static void collectSchemaRecursively(JSONObject sourceSchemas, String schemaName, Set<String> allSchemas,
			Set<String> processed) {
		if (processed.contains(schemaName) || !sourceSchemas.has(schemaName)) {
			return;
		}

		processed.add(schemaName);
		allSchemas.add(schemaName);

		JSONObject schema = sourceSchemas.getJSONObject(schemaName);
		Set<String> nestedSchemas = new HashSet<>();
		collectSchemasFromObject(schema, nestedSchemas);

		for (String nestedSchema : nestedSchemas) {
			collectSchemaRecursively(sourceSchemas, nestedSchema, allSchemas, processed);
		}
	}

	private static void collectSchemasFromObject(Object obj, Set<String> schemas) {
		if (obj instanceof JSONObject) {
			JSONObject jsonObj = (JSONObject) obj;
			if (jsonObj.has("$ref")) {
				String ref = jsonObj.getString("$ref");
				if (ref.startsWith("#/components/schemas/")) {
					schemas.add(ref.substring("#/components/schemas/".length()));
				}
			}
			for (String key : jsonObj.keySet()) {
				collectSchemasFromObject(jsonObj.get(key), schemas);
			}
		} else if (obj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) obj;
			for (int i = 0; i < jsonArray.length(); i++) {
				collectSchemasFromObject(jsonArray.get(i), schemas);
			}
		}
	}

	private static JSONObject extractSchemas(JSONObject source, Set<String> referencedSchemas) {
		JSONObject schemas = new JSONObject();
		if (source.has("components") && source.getJSONObject("components").has("schemas")) {
			JSONObject sourceSchemas = source.getJSONObject("components").getJSONObject("schemas");
			for (String schemaName : referencedSchemas) {
				if (sourceSchemas.has(schemaName)) {
					schemas.put(schemaName, sourceSchemas.get(schemaName));
				}
			}
		}
		return schemas;
	}

	public static <T> Stream<T> asStream(Iterator<T> iterator) {
		Iterable<T> iterable = () -> iterator;
		return StreamSupport.stream(iterable.spliterator(), false);
	}
}
