package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.LinkedHashMap;
import java.util.Map;

public class PathInfo {
	private String path;
	private Map<String, EndpointInfo> operationToEndpointInfo;
	
	public PathInfo(String relativePath) {
		this.path = relativePath;
		this.operationToEndpointInfo = new LinkedHashMap<>();
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void addEndpointInfo(String operation, EndpointInfo endpointInfo) {
		if (this.operationToEndpointInfo.containsKey(operation)) {
			throw new IllegalArgumentException("This operation already exists inside of the map.");
		}
		this.operationToEndpointInfo.put(operation, endpointInfo);
	}
	
	public Map<String, EndpointInfo> getOperationToEndpointInfo() {
		return new LinkedHashMap<>(this.operationToEndpointInfo);
	}
}
