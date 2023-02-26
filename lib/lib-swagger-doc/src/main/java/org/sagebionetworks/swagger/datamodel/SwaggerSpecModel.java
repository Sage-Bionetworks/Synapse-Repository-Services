package org.sagebionetworks.swagger.datamodel;

import java.util.*;

import org.sagebionetworks.swagger.datamodel.pathinfo.PathInfo;

public class SwaggerSpecModel {
	private String openapiVersion;
	private ApiInfo info;
	private List<ServerInfo> servers;
	private List<PathInfo> paths;
	private List<ComponentInfo> components;
	
	public SwaggerSpecModel(String openapiVersion) {
		this(openapiVersion, null);
	}
	
	public SwaggerSpecModel(String openapiVersion, ApiInfo info) {
		this.openapiVersion = openapiVersion;
		this.info = info;
		this.servers = new ArrayList<>();
		this.paths = new ArrayList<>();
		this.components = new ArrayList<>();
	}
	
	public ApiInfo getApiInfo() {
		return this.info;
	}
	
	public String getOpenapiVersion() {
		return this.openapiVersion;
	}
	
	public void setApiInfo(ApiInfo info) {
		this.info = info;
	}
	
	public void addServer(ServerInfo server) {
		this.servers.add(server);
	}
	
	public List<ServerInfo> getServers() {
		return new ArrayList<>(this.servers);
	}
	
	public void addPath(PathInfo path) {
		this.paths.add(path);
	}
	
	public List<PathInfo> getPaths() {
		return new ArrayList<>(this.paths);
	}
	
	public void addComponent(ComponentInfo component) {
		this.components.add(component);
	}
	
	public List<ComponentInfo> getComponents() {
		return this.components;
	}
}
