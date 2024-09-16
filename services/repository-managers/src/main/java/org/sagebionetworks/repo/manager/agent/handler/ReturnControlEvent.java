package org.sagebionetworks.repo.manager.agent.handler;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.manager.agent.parameter.Parameter;

public class ReturnControlEvent{
	
	private final Long runAsUserId;
	private final String actionGroup;
	private final String function;
	private final List<Parameter> parameters;
	
	
	public ReturnControlEvent(Long userId, String actionGroup, String function, List<Parameter> parameters) {
		super();
		this.runAsUserId = userId;
		this.actionGroup = actionGroup;
		this.function = function;
		this.parameters = parameters;
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


	@Override
	public int hashCode() {
		return Objects.hash(actionGroup, function, parameters, runAsUserId);
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
		return Objects.equals(actionGroup, other.actionGroup) && Objects.equals(function, other.function)
				&& Objects.equals(parameters, other.parameters) && Objects.equals(runAsUserId, other.runAsUserId);
	}

	@Override
	public String toString() {
		return "ReturnControlEvent [runAsUserId=" + runAsUserId + ", actionGroup=" + actionGroup + ", function="
				+ function + ", parameters=" + parameters + "]";
	}
	
}