package org.sagebionetworks.repo.manager.config;

public enum AgentSuffix {

	basic("agent"), grid("grid-agent");

	AgentSuffix(String suffix) {
		this.suffix = suffix;
	}

	private final String suffix;

	public String getSuffix() {
		return suffix;
	}

	public static AgentSuffix fromSuffix(String suffix) {
		for(AgentSuffix type: AgentSuffix.values()) {
			if(type.suffix.equals(suffix)) {
				return type;
			}
		}
		throw new IllegalArgumentException("No suffix found for: "+suffix);
	}

}
