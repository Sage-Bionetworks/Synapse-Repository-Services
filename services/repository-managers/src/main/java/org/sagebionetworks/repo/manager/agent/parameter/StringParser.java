package org.sagebionetworks.repo.manager.agent.parameter;

public class StringParser implements ParameterParser<String> {

	@Override
	public String parse(String value) {
		return value;
	}

}
