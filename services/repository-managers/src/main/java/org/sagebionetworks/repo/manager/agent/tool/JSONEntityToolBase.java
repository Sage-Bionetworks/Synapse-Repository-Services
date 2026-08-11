package org.sagebionetworks.repo.manager.agent.tool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.agent.specialist.JSONEntityResultConverter;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class that exposes {@link JSONEntityTool}-annotated methods as Spring AI {@link ToolCallback}s.
 * <p>
 * For each annotated method the base:
 * <ul>
 * <li>generates the tool's native {@code inputSchema} — the self-contained JSON Schema Bedrock hands
 * the model — from the method's request parameter (see {@link JSONEntityToolSchemaGenerator}), so the
 * request structure lives in the tool contract rather than in injected prompt prose;</li>
 * <li>on invocation, deserializes the model's JSON argument into the request POJO via the Synapse
 * {@code concreteType}-aware path ({@link JDOSecondaryPropertyUtils#createObjectFromJSON}), which
 * understands the discriminated {@code oneOf} unions that Spring AI's plain Jackson mapper does not;</li>
 * <li>serializes the returned {@link org.sagebionetworks.repo.manager.agent.specialist.ToolResponse}
 * via {@link JSONEntityResultConverter}.</li>
 * </ul>
 * <p>
 * A request parameter may instead be declared as a raw {@code String}, in which case the model's JSON
 * is passed through untouched (the tool consumes the raw payload) while the model still sees a generated
 * schema, sourced from {@link JSONEntityToolParam#schemaType()}. This is used where a typed round-trip
 * would lose information the raw JSON must preserve (e.g. the undefined-vs-null distinction on a grid
 * update value).
 * <p>
 * When a request argument cannot be parsed, {@code call(...)} does not throw: it returns a plain error
 * string, which Spring AI feeds back to the model as the tool result so the model can self-correct on
 * its next turn.
 */
public abstract class JSONEntityToolBase {

	/**
	 * A lenient mapper used only to normalize the model's raw argument string before the strict parsers
	 * (org.json / the {@code concreteType}-aware path) see it. It accepts raw C0 control characters
	 * (e.g. an unescaped newline inside a free-text value) that {@link JSONObject} would otherwise reject;
	 * re-serializing escapes them, so a control character in an argument no longer costs a model round trip.
	 */
	private static final ObjectMapper LENIENT_MAPPER = createLenientMapper();

	private static ObjectMapper createLenientMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.getFactory().configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
		return mapper;
	}

	/**
	 * Re-serialize the model's argument through the lenient mapper so raw control characters become
	 * properly escaped JSON. Input that is not parseable even leniently is returned untouched, leaving the
	 * strict parsers to produce the corrective error the model already knows how to act on.
	 */
	private static String normalizeJson(String toolInput) {
		if (toolInput == null || toolInput.isBlank()) {
			return toolInput;
		}
		try {
			return LENIENT_MAPPER.writeValueAsString(LENIENT_MAPPER.readTree(toolInput));
		} catch (JsonProcessingException e) {
			return toolInput;
		}
	}

	private final List<ToolCallback> callbacks;

	public JSONEntityToolBase() {
		callbacks = new ArrayList<>();

		for (Method method : this.getClass().getMethods()) {
			if (!Modifier.isStatic(method.getModifiers())) {

				JSONEntityTool toolAnno = method.getDeclaredAnnotation(JSONEntityTool.class);
				if (toolAnno != null) {

					String name = toolAnno.name().isBlank() ? method.getName() : toolAnno.name();

					// Wrap each callback so every invocation of a base-derived tool is logged.
					callbacks.add(new LoggingToolCallback(new Builder().setToolObject(this)
							.setToolDefinition(DefaultToolDefinition.builder().name(name)
									.description(toolAnno.description()).inputSchema(generateInputSchema(method)).build())
							.setToolMetadata(ToolMetadata.builder().returnDirect(toolAnno.returnDirect()).build())
							.setToolMethod(method).build()));
				}
			}
		}
		if (callbacks.isEmpty()) {
			throw new IllegalStateException("No tools found in this class");
		}
	}

	public List<ToolCallback> getToolCallbacks() {
		return callbacks;
	}

	/**
	 * The seed iterators for every polymorphic interface reachable from a tool's request type, one per
	 * interface, from {@code <Interface>InstanceFactory.singleton().getKeySetIterator()}. These enumerate
	 * an interface's concrete implementers, which the schema generator cannot otherwise discover.
	 * <p>
	 * Subclasses whose request type contains {@code oneOf} interface unions must override this; the
	 * default is empty (no polymorphic properties).
	 */
	protected Collection<Iterator<String>> getPolymorphicImplementerSeeds() {
		return List.of();
	}

	/**
	 * Build the {@code inputSchema} for a tool method. Two shapes are supported:
	 * <ul>
	 * <li><b>request-body</b>: a single {@link JSONEntity} request parameter (or a raw
	 * {@code String}/{@code JSONObject} carrier whose {@link JSONEntityToolParam#schemaType()} is set)
	 * carries the entire tool input; the schema is generated from that type;</li>
	 * <li><b>scalar</b>: zero or more scalar parameters ({@code String}/{@code Long}/{@code Integer}/
	 * {@code Boolean}/{@code Double}) each become a top-level property. A method that takes only a
	 * {@link ToolContext} (or nothing) yields a valid no-argument schema.</li>
	 * </ul>
	 */
	private String generateInputSchema(Method method) {
		Parameter bodyParameter = findBodyParameter(method);
		if (bodyParameter != null) {
			JSONEntityToolParam paramAnno = bodyParameter.getDeclaredAnnotation(JSONEntityToolParam.class);
			Class<? extends JSONEntity> schemaType = paramAnno.schemaType();
			if (JSONEntity.class.equals(schemaType)) {
				// The parameter is itself the request POJO; use its declared type as the schema source.
				schemaType = bodyParameter.getType().asSubclass(JSONEntity.class);
			}
			return JSONEntityToolSchemaGenerator.generateSchema(schemaType.getName(),
					getPolymorphicImplementerSeeds(), paramAnno.description());
		}
		return JSONEntityToolSchemaGenerator.generateScalarSchema(collectScalarParameters(method), null);
	}

	/**
	 * The single request-body parameter of a tool method, or {@code null} when the method uses scalar
	 * arguments. A parameter is the body when it is annotated and either its declared type is a
	 * {@link JSONEntity} or its {@link JSONEntityToolParam#schemaType()} is set (a raw carrier).
	 */
	private static Parameter findBodyParameter(Method method) {
		for (Parameter parameter : method.getParameters()) {
			if (isBodyParameter(parameter)) {
				return parameter;
			}
		}
		return null;
	}

	private static boolean isBodyParameter(Parameter parameter) {
		JSONEntityToolParam anno = parameter.getDeclaredAnnotation(JSONEntityToolParam.class);
		if (anno == null) {
			return false;
		}
		return JSONEntity.class.isAssignableFrom(parameter.getType())
				|| !JSONEntity.class.equals(anno.schemaType());
	}

	/**
	 * Collect the scalar arguments of a tool method (everything but the {@link ToolContext}). Each must
	 * be annotated with {@link JSONEntityToolParam} so it carries a name-backed description and its
	 * required flag.
	 */
	private static List<JSONEntityToolSchemaGenerator.ScalarParameter> collectScalarParameters(Method method) {
		List<JSONEntityToolSchemaGenerator.ScalarParameter> scalars = new ArrayList<>();
		for (Parameter parameter : method.getParameters()) {
			if (ToolContext.class.equals(parameter.getType())) {
				continue;
			}
			JSONEntityToolParam anno = parameter.getDeclaredAnnotation(JSONEntityToolParam.class);
			if (anno == null) {
				throw new IllegalStateException("Tool method '" + method.getName() + "' parameter '"
						+ parameter.getName() + "' must be annotated with @JSONEntityToolParam");
			}
			scalars.add(new JSONEntityToolSchemaGenerator.ScalarParameter(parameter.getName(), parameter.getType(),
					anno.description(), anno.required()));
		}
		return scalars;
	}

	private class Builder {

		private ToolDefinition toolDefinition;
		private ToolMetadata toolMetadata;
		private Method toolMethod;
		private Object toolObject;

		public ToolCallback build() {
			return new ToolCallback() {

				@Override
				public ToolMetadata getToolMetadata() {
					return toolMetadata;
				}

				@Override
				public ToolDefinition getToolDefinition() {
					return toolDefinition;
				}

				@Override
				public String call(String toolInput) {
					return call(toolInput, null);
				}

				@Override
				public String call(String toolInput, @Nullable ToolContext toolContext) {
					Object[] arguments;
					try {
						arguments = marshalArguments(toolInput, toolContext);
					} catch (IllegalArgumentException e) {
						// A malformed argument is fed back to the model as the tool result so it can retry;
						// it is not an exceptional condition for the agent loop.
						return "The argument provided to '" + toolDefinition.name()
								+ "' was not valid JSON for its input schema: " + e.getMessage()
								+ ". Resubmit the call with a corrected argument.";
					}
					Object result = callMethod(arguments);
					return new JSONEntityResultConverter().convert(result, toolMethod.getGenericReturnType());
				}
			};
		}

		/**
		 * Bind the model's JSON argument and the tool context onto the tool method's parameters:
		 * <ul>
		 * <li>a typed {@link JSONEntity} parameter is deserialized via the {@code concreteType}-aware path;</li>
		 * <li>a {@link JSONObject} parameter receives the parsed-but-untyped payload &mdash; well-formedness
		 * is validated here (so the base can return corrective feedback), yet the raw tree is preserved so
		 * an omitted key stays distinct from an explicit JSON null;</li>
		 * <li>a raw {@link String} parameter receives the untouched payload with no parsing;</li>
		 * <li>a {@link ToolContext} parameter receives the context.</li>
		 * </ul>
		 *
		 * @throws IllegalArgumentException if a typed or {@link JSONObject} request parameter cannot be
		 *                                  parsed from the input.
		 */
		private Object[] marshalArguments(String toolInput, @Nullable ToolContext toolContext) {
			// Normalize up front so a raw control character (e.g. a newline in a free-text value) is
			// escaped before either the scalar (org.json) or body (concreteType-aware) parser sees it.
			toolInput = normalizeJson(toolInput);
			Parameter[] parameters = toolMethod.getParameters();
			Object[] arguments = new Object[parameters.length];
			boolean hasBody = findBodyParameter(toolMethod) != null;
			// In scalar mode the input is a JSON object of named arguments; parse it once up front.
			JSONObject namedArguments = hasBody ? null : parseNamedArguments(toolInput);
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				Class<?> parameterType = parameter.getType();
				if (ToolContext.class.equals(parameterType)) {
					arguments[i] = toolContext;
				} else if (hasBody) {
					arguments[i] = marshalBodyArgument(parameterType, toolInput);
				} else {
					arguments[i] = extractScalarArgument(namedArguments, parameter);
				}
			}
			return arguments;
		}

		/**
		 * Bind the entire tool input to the single request-body parameter: a typed {@link JSONEntity} is
		 * deserialized via the {@code concreteType}-aware path; a {@link JSONObject} receives the
		 * parsed-but-untyped payload (well-formedness validated, undefined-vs-null preserved); a raw
		 * {@link String} carrier receives the untouched payload.
		 */
		private Object marshalBodyArgument(Class<?> parameterType, String toolInput) {
			if (JSONEntity.class.isAssignableFrom(parameterType)) {
				return deserialize(parameterType.asSubclass(JSONEntity.class), toolInput);
			} else if (JSONObject.class.equals(parameterType)) {
				return parseRawObject(toolInput);
			}
			return toolInput;
		}

		private JSONObject parseNamedArguments(String toolInput) {
			if (toolInput == null || toolInput.isBlank()) {
				return new JSONObject();
			}
			return parseRawObject(toolInput);
		}

		/**
		 * Read one named scalar argument from the parsed input and coerce it to the parameter's type. A
		 * missing required argument, or a value that cannot be read as the declared type, is reported as
		 * {@link IllegalArgumentException} so {@code call(...)} can feed corrective guidance to the model.
		 */
		private Object extractScalarArgument(JSONObject namedArguments, Parameter parameter) {
			String name = parameter.getName();
			JSONEntityToolParam anno = parameter.getDeclaredAnnotation(JSONEntityToolParam.class);
			if (!namedArguments.has(name) || namedArguments.isNull(name)) {
				if (anno != null && anno.required()) {
					throw new IllegalArgumentException("missing required argument '" + name + "'");
				}
				return null;
			}
			Class<?> type = parameter.getType();
			try {
				if (String.class.equals(type)) {
					return namedArguments.getString(name);
				} else if (Long.class.equals(type) || long.class.equals(type)) {
					return namedArguments.getLong(name);
				} else if (Integer.class.equals(type) || int.class.equals(type)) {
					return namedArguments.getInt(name);
				} else if (Double.class.equals(type) || double.class.equals(type)) {
					return namedArguments.getDouble(name);
				} else if (Float.class.equals(type) || float.class.equals(type)) {
					return (float) namedArguments.getDouble(name);
				} else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
					return namedArguments.getBoolean(name);
				}
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("argument '" + name + "' could not be read as "
						+ type.getSimpleName() + ": " + e.getMessage(), e);
			}
			throw new IllegalStateException("Unsupported scalar tool argument type: " + type.getName());
		}

		private JSONEntity deserialize(Class<? extends JSONEntity> type, String toolInput) {
			try {
				return JDOSecondaryPropertyUtils.createObjectFromJSON(type, toolInput);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException(
						e.getCause() == null ? e.getMessage() : e.getCause().getMessage(), e);
			}
		}

		private JSONObject parseRawObject(String toolInput) {
			try {
				return new JSONObject(toolInput);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException(
						e.getCause() == null ? e.getMessage() : e.getCause().getMessage(), e);
			}
		}

		@SuppressWarnings("null")
		@Nullable
		private Object callMethod(Object[] methodArguments) {
			if (isObjectNotPublic() || isMethodNotPublic()) {
				this.toolMethod.setAccessible(true);
			}

			Object result;
			try {
				result = this.toolMethod.invoke(this.toolObject, methodArguments);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
			} catch (InvocationTargetException ex) {
				throw new ToolExecutionException(this.toolDefinition, ex.getCause());
			}
			return result;
		}

		public Builder setToolDefinition(ToolDefinition toolDefinition) {
			this.toolDefinition = toolDefinition;
			return this;
		}

		public Builder setToolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		public Builder setToolMethod(Method toolMethod) {
			this.toolMethod = toolMethod;
			return this;
		}

		public Builder setToolObject(Object toolObject) {
			this.toolObject = toolObject;
			return this;
		}

		private boolean isObjectNotPublic() {
			return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
		}

		private boolean isMethodNotPublic() {
			return !Modifier.isPublic(this.toolMethod.getModifiers());
		}
	}

}
