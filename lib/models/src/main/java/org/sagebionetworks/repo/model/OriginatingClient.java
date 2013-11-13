package org.sagebionetworks.repo.model;

import org.apache.commons.lang.StringUtils;

public enum OriginatingClient {
	SYNAPSE,
	BRIDGE;

	public static OriginatingClient getClientFromOriginClientParam(String value) {
		if (StringUtils.isNotBlank(value) && value.toLowerCase().equals("bridge")) {
			return OriginatingClient.BRIDGE; 
		}
		return OriginatingClient.SYNAPSE;
	}
}
