package org.sagebionetworks.repo.manager.agent.parameter;

public enum ParameterType {

	string(String.class, new StringParser()), integer(Integer.class, new IntegerParser());

	private ParameterType(Class<?> type, ParameterParser<?> parser) {
		this.classType = type;
		this.parser = parser;
	}

	private Class<?> classType;
	private ParameterParser<?> parser;

	public static ParameterType lookup(Class<?> lookup) {
		for (ParameterType type : ParameterType.values()) {
			if (type.classType.equals(lookup)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported Parameter type: " + lookup.getName());
	}

	public boolean matchesType(String type) {
		return name().equalsIgnoreCase(type);
	}

	public Object parse(String value) {
		return this.parser.parse(value);
	}

}
