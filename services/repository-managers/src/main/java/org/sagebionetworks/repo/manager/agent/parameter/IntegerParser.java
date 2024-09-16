package org.sagebionetworks.repo.manager.agent.parameter;

public class IntegerParser implements ParameterParser<Integer> {

	@Override
	public Integer parse(String value) {
		return Integer.parseInt(value);
	}

}
