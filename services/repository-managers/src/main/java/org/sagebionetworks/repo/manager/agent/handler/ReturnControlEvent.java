package org.sagebionetworks.repo.manager.agent.handler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.agent.SessionContext;

public class ReturnControlEvent {

	private final Long runAsUserId;
	private final String actionGroup;
	private final String function;
	private final List<Parameter> parameters;
	private final List<Parameter> requestBodyParameters;
	private final SessionContext context;

	public ReturnControlEvent(Long runAsUserId, String actionGroup, String function, List<Parameter> parameters,
			List<Parameter> requestBodyParameters, SessionContext context) {
		super();
		this.runAsUserId = runAsUserId;
		this.actionGroup = actionGroup;
		this.function = function;
		this.parameters = parameters;
		/*
		 * The JSON body will be extracted from the parameters at a later stage to
		 * ensure any failures can be reported to the agent.
		 */
		this.requestBodyParameters = requestBodyParameters;
		this.context = context;
	}

	public ReturnControlEvent(Long userId, String actionGroup, String function, List<Parameter> parameters) {
		super();
		this.runAsUserId = userId;
		this.actionGroup = actionGroup;
		this.function = function;
		this.parameters = parameters;
		this.requestBodyParameters = null;
		this.context = null;
	}

	public Long getRunAsUserId() {
		return runAsUserId;
	}

	public String getActionGroup() {
		return actionGroup;
	}

	public String getFunction() {
		return function;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public Optional<String> getRequestBody() {
		return Optional.ofNullable(extractRequestBodyFromParameters());
	}

	/**
	 * Extracting a request body from the parameters will fail when the agent
	 * provides invalid JSON (something that can happen frequently). By delaying
	 * this extraction until the request body is actually needed, we can ensure that
	 * any exceptions will make it back to the agent.
	 * 
	 * @return
	 */
	String extractRequestBodyFromParameters() {
		if (requestBodyParameters == null) {
			return null;
		}
		JSONObject object = new JSONObject();
		requestBodyParameters.forEach(p -> {
			try {
				if ("object".equals(p.getType())) {
					object.put(p.getName(), new JSONObject(p.getValue()));
				} else if ("string".equals(p.getType())) {
					object.put(p.getName(), p.getValue());
				} else if ("integer".equals(p.getType())) {
					object.put(p.getName(), p.getValue());
				} else if ("array".equals(p.getType())) {
					object.put(p.getName(), new JSONArray(p.getValue()));
				} else {
					throw new IllegalArgumentException("Unknown type: " + p.getType());
				}
			} catch (JSONException e) {
				throw new IllegalArgumentException("Failed to parse the JSON value for parameter '" + p.getName()
						+ "' with value: " + p.getValue() + ". Error: " + e.getMessage(), e);
			}
		});
		return object.toString();
	}

	public <T extends SessionContext> Optional<T> getSessionContext(Class<? extends T> clazz) {
		if (context != null && clazz.isInstance(context)) {
			return Optional.of(clazz.cast(context));
		}
		return Optional.empty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(actionGroup, context, function, parameters, requestBodyParameters, runAsUserId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReturnControlEvent other = (ReturnControlEvent) obj;
		return Objects.equals(actionGroup, other.actionGroup) && Objects.equals(context, other.context)
				&& Objects.equals(function, other.function) && Objects.equals(parameters, other.parameters)
				&& Objects.equals(requestBodyParameters, other.requestBodyParameters)
				&& Objects.equals(runAsUserId, other.runAsUserId);
	}

	@Override
	public String toString() {
		return "ReturnControlEvent [runAsUserId=" + runAsUserId + ", actionGroup=" + actionGroup + ", function="
				+ function + ", parameters=" + parameters + ", requestBodyParameters=" + requestBodyParameters
				+ ", context=" + context + "]";
	}

}