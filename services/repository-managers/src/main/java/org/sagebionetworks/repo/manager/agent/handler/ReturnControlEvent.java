package org.sagebionetworks.repo.manager.agent.handler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.agent.SessionContext;

public class ReturnControlEvent {

	private final Long runAsUserId;
	private final String actionGroup;
	private final String function;
	private final List<Parameter> parameters;
	private final String requestBody;
	private final SessionContext context;

	public ReturnControlEvent(Long runAsUserId, String actionGroup, String function, List<Parameter> parameters,
			String requestBody, SessionContext context) {
		super();
		this.runAsUserId = runAsUserId;
		this.actionGroup = actionGroup;
		this.function = function;
		this.parameters = parameters;
		this.requestBody = requestBody;
		this.context = context;
	}

	public ReturnControlEvent(Long userId, String actionGroup, String function, List<Parameter> parameters) {
		super();
		this.runAsUserId = userId;
		this.actionGroup = actionGroup;
		this.function = function;
		this.parameters = parameters;
		this.requestBody = null;
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
		return Optional.ofNullable(requestBody);
	}

	public <T extends SessionContext> Optional<T> getSessionContext(Class<? extends T> clazz) {
		if (context != null && clazz.isInstance(context)) {
			return Optional.of(clazz.cast(context));
		}
		return Optional.empty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(actionGroup, context, function, parameters, requestBody, runAsUserId);
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
				&& Objects.equals(requestBody, other.requestBody) && Objects.equals(runAsUserId, other.runAsUserId);
	}

	@Override
	public String toString() {
		return "ReturnControlEvent [runAsUserId=" + runAsUserId + ", actionGroup=" + actionGroup + ", function="
				+ function + ", parameters=" + parameters + ", requestBody=" + requestBody + ", context=" + context
				+ "]";
	}

}