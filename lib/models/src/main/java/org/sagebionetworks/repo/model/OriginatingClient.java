package org.sagebionetworks.repo.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public enum OriginatingClient {
	SYNAPSE,
	BRIDGE;

	private Map<String,String> parameters;
	
	private OriginatingClient() {
		parameters = new HashMap<String,String>();
		parameters.put("originClient", this.name().toLowerCase());
	}

	public static OriginatingClient getClientFromOriginClientParam(String value) {
		if (StringUtils.isNotBlank(value) && value.toLowerCase().equals("bridge")) {
			return OriginatingClient.BRIDGE; 
		}
		return OriginatingClient.SYNAPSE;
	}
	
	public Map<String,String> getParameterMap() {
		return parameters;
	}

}
