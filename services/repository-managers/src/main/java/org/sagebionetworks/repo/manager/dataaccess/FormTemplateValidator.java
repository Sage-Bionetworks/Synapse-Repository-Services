package org.sagebionetworks.repo.manager.dataaccess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaProperties;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Validates a form template against the JSON Schema that it renders.
 */
@Service
public class FormTemplateValidator {

	/**
	 * A property with this format holds the id of a file uploaded to Synapse.
	 */
	static final String FILE_HANDLE_FORMAT = "synapse-filehandle-id";

	/**
	 * The react-jsonschema-form widget that uploads a file.
	 */
	static final String FILE_WIDGET = "file";

	private static final String UI_WIDGET_KEY = "ui:widget";
	private static final String LOCAL_REF_PREFIX = "#/definitions/";

	private final JsonSchemaManager jsonSchemaManager;

	public FormTemplateValidator(JsonSchemaManager jsonSchemaManager) {
		this.jsonSchemaManager = jsonSchemaManager;
	}

	/**
	 * Validate that the given template can render the schema it is bound to.
	 *
	 * @param template The template to validate.
	 * @throws IllegalArgumentException If the template is bound to a schema that is not an exact
	 *                                  registered version, or if any of its fields do not agree with
	 *                                  that schema. The message reports every problem found.
	 */
	public void validate(FormTemplate template) {
		ValidateArgument.required(template, "template");
		ValidateArgument.requiredNotBlank(template.getName(), "template.name");
		ValidateArgument.requiredNotBlank(template.getSchema$id(), "template.schema$id");
		ValidateArgument.requiredNotEmpty(template.getSteps(), "template.steps");

		JsonSchema schema = getSchemaForExactVersion(template.getSchema$id());

		List<String> problems = new ArrayList<>();
		Set<String> coveredPaths = new HashSet<>();

		for (int stepIndex = 0; stepIndex < template.getSteps().size(); stepIndex++) {
			validateStep(template.getSteps().get(stepIndex), stepIndex, schema, coveredPaths, problems);
		}
		// Nothing may be left for the user to fill in outside of the form, so every property the
		// schema demands must be claimed by a field. Duplicate paths are reported per field, which
		// leaves each required property covered exactly once.
		collectRequiredProperties(schema).stream().map(FormTemplateValidator::asJsonPointer)
				.filter(pointer -> !coveredPaths.contains(pointer))
				.forEach(pointer -> problems.add("No field covers the required property '" + pointer + "'."));

		if (!problems.isEmpty()) {
			throw new IllegalArgumentException("The form template is invalid: " + String.join(" ", problems));
		}
	}

	/**
	 * Loads the schema that the template renders, with every reference expanded so that field paths
	 * can be followed through referenced schemas.
	 */
	private JsonSchema getSchemaForExactVersion(String schema$id) {
		SchemaId parsedId = SchemaIdParser.parseSchemaId(schema$id);
		if (parsedId.getSemanticVersion() == null) {
			throw new IllegalArgumentException("The schema$id '" + schema$id
					+ "' must include the semantic version of the schema, for example 'org.example-Schema-1.0.2'.");
		}
		try {
			return jsonSchemaManager.getValidationSchema(schema$id);
		} catch (NotFoundException e) {
			throw new IllegalArgumentException("The schema '" + schema$id + "' does not exist.", e);
		}
	}

	private void validateStep(FormTemplateStep step, int stepIndex, JsonSchema schema, Set<String> coveredPaths,
			List<String> problems) {
		ValidateArgument.required(step, "steps[" + stepIndex + "]");
		ValidateArgument.requiredNotBlank(step.getTitle(), "steps[" + stepIndex + "].title");
		ValidateArgument.requiredNotEmpty(step.getFields(), "steps[" + stepIndex + "].fields");

		for (int fieldIndex = 0; fieldIndex < step.getFields().size(); fieldIndex++) {
			FormTemplateField field = step.getFields().get(fieldIndex);
			ValidateArgument.required(field, "steps[" + stepIndex + "].fields[" + fieldIndex + "]");
			ValidateArgument.requiredNotBlank(field.getSchemaPath(),
					"steps[" + stepIndex + "].fields[" + fieldIndex + "].schemaPath");
			validateField(field, schema, coveredPaths, problems);
		}
	}

	private void validateField(FormTemplateField field, JsonSchema schema, Set<String> coveredPaths,
			List<String> problems) {
		String schemaPath = field.getSchemaPath();
		if (!coveredPaths.add(schemaPath)) {
			problems.add("More than one field targets the property '" + schemaPath + "'.");
			return;
		}
		Optional<JsonSchema> target = resolveJsonPointer(schema, schemaPath);
		if (target.isEmpty()) {
			problems.add("The path '" + schemaPath + "' does not resolve to a property of the schema.");
			return;
		}
		JsonSchema targetProperty = target.get();
		if (Type.object.equals(targetProperty.getType())
				|| !collectProperties(targetProperty, schema.getDefinitions()).isEmpty()) {
			problems.add("The path '" + schemaPath + "' resolves to an object rather than a single value.");
			return;
		}
		validateUiDefinition(field, targetProperty, schemaPath, problems);
	}

	/**
	 * A field that uploads a file can only be bound to a property that holds a file handle id.
	 * Every other aspect of the ui definition is passed through to the client untouched.
	 */
	private void validateUiDefinition(FormTemplateField field, JsonSchema targetProperty, String schemaPath,
			List<String> problems) {
		Object uiDefinition = field.getUiDefinition();
		if (!(uiDefinition instanceof JSONObjectAdapter)) {
			problems.add("The uiDefinition of the field '" + schemaPath + "' must be a JSON object.");
			return;
		}
		boolean uploadsFile = field.getTemplateFileHandleId() != null
				|| FILE_WIDGET.equals(readWidget((JSONObjectAdapter) uiDefinition));
		boolean holdsFileHandleId = FILE_HANDLE_FORMAT.equals(targetProperty.getFormat())
				&& (Type.integer.equals(targetProperty.getType()) || Type.number.equals(targetProperty.getType()));

		if (uploadsFile && !holdsFileHandleId) {
			problems.add("The field '" + schemaPath + "' uploads a file, so the property it targets must be a"
					+ " number with the format '" + FILE_HANDLE_FORMAT + "'.");
		}
	}

	private static String readWidget(JSONObjectAdapter uiDefinition) {
		try {
			return uiDefinition.has(UI_WIDGET_KEY) ? uiDefinition.getString(UI_WIDGET_KEY) : null;
		} catch (JSONObjectAdapterException e) {
			// A widget that is not a string cannot name the file widget.
			return null;
		}
	}

	/**
	 * Follows an RFC 6901 pointer through the properties of the given schema.
	 *
	 * @return Empty if any token of the pointer does not name a property.
	 */
	private static Optional<JsonSchema> resolveJsonPointer(JsonSchema schema, String pointer) {
		if (!pointer.startsWith("/")) {
			return Optional.empty();
		}
		JsonSchema current = schema;
		for (String token : pointer.substring(1).split("/", -1)) {
			current = collectProperties(current, schema.getDefinitions()).get(unescapeJsonPointerToken(token));
			if (current == null) {
				return Optional.empty();
			}
		}
		return Optional.of(current);
	}

	/**
	 * The properties an instance of the given node may carry, with references resolved.
	 */
	private static Map<String, JsonSchema> collectProperties(JsonSchema node, Map<String, JsonSchema> definitions) {
		// A validation schema keeps a copy of every referenced schema in the definitions of its root,
		// so a nested node needs those definitions attached before its references can be followed.
		return JsonSchemaProperties
				.collectTopLevelProperties(new JsonSchema().setAllOf(List.of(node)).setDefinitions(definitions));
	}

	/**
	 * The names of the properties that an instance of the given schema must carry. Only the
	 * unconditional composition of the root is walked, since a property that a schema requires
	 * conditionally cannot be known to be needed by every request.
	 */
	private static Set<String> collectRequiredProperties(JsonSchema root) {
		Set<String> required = new LinkedHashSet<>();
		Set<String> followed = new HashSet<>();
		Deque<JsonSchema> toVisit = new ArrayDeque<>(List.of(root));
		while (!toVisit.isEmpty()) {
			JsonSchema node = toVisit.poll();
			if (node.get$ref() != null && followed.add(node.get$ref())) {
				resolveLocalReference(root, node.get$ref()).ifPresent(toVisit::add);
			}
			if (node.getRequired() != null) {
				required.addAll(node.getRequired());
			}
			if (node.getAllOf() != null) {
				toVisit.addAll(node.getAllOf());
			}
		}
		return required;
	}

	private static Optional<JsonSchema> resolveLocalReference(JsonSchema root, String reference) {
		if (!reference.startsWith(LOCAL_REF_PREFIX) || root.getDefinitions() == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(root.getDefinitions().get(reference.substring(LOCAL_REF_PREFIX.length())));
	}

	/**
	 * Renders a top level property name as the RFC 6901 pointer that addresses it, so that a name
	 * containing the pointer delimiters can still be compared against the schemaPath of a field. The
	 * property named 'a/b' is addressed by the pointer '/a~1b'.
	 */
	private static String asJsonPointer(String propertyName) {
		// '~' is escaped before '/' because escaping '/' introduces a '~' that must be left alone.
		return "/" + propertyName.replace("~", "~0").replace("/", "~1");
	}

	/**
	 * Recovers the property name that a single token of an RFC 6901 pointer addresses. The token
	 * '~01' addresses the property named '~1'.
	 */
	private static String unescapeJsonPointerToken(String token) {
		// '~1' is unescaped before '~0' because doing it the other way round would reduce the '~01'
		// that encodes a literal '~1' to a '/'.
		return token.replace("~1", "/").replace("~0", "~");
	}
}
