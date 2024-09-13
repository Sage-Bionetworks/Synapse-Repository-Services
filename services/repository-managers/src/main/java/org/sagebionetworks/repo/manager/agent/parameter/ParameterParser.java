package org.sagebionetworks.repo.manager.agent.parameter;

@FunctionalInterface
public interface ParameterParser<T> {

	/**
	 * Parser the provided value into the parameterizes value.
	 * 
	 * @param value
	 * @return
	 */
	T parse(String value);
}
