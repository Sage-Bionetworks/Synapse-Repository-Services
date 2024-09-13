package org.sagebionetworks.repo.manager.agent.parameter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ParameterUtils {

	/**
	 * Attempt to extract the given parameter from the provided parameters.
	 * 
	 * @param <T>
	 * @param type
	 * @param name
	 * @param parameters
	 * @return {@link Optional#empty()} if the provided parameter cannot be found.
	 */
	public static <T> Optional<T> extractParameter(Class<? extends T> type, String name, List<Parameter> parameters) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(parameters, "parameters");
		ParameterType parameterType = ParameterType.lookup(type);
		return parameters.stream().filter(p -> name.equals(p.getName()) && parameterType.matchesType(p.getType())).findFirst()
				.map(p -> (T) parameterType.parse(p.getValue()));
	}

}
