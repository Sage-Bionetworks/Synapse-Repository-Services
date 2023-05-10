package org.sagebionetworks.openapi.datamodel;

import java.util.List;

import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.openapi.datamodel.pathinfo.EndpointInfo;

import com.google.gson.Gson;

/**
 * Models the OpenAPI specification.
 * @author lli
 *
 */
public class OpenAPISpecModel {
	private String openapi;
	private ApiInfo info;
	private List<ServerInfo> servers;
	private List<TagInfo> tags;
	// This maps path -> { operation -> endpointInfo}
	private Map<String, Map<String, EndpointInfo>> paths;
	private ComponentInfo components;
	
	public String getOpenapi() {
		return openapi;
	}
	
	public OpenAPISpecModel withOpenapi(String openapi) {
		this.openapi = openapi;
		return this;
	}
	
	public ApiInfo getInfo() {
		return info;
	}
	
	public OpenAPISpecModel withInfo(ApiInfo info) {
		this.info = info;
		return this;
	}
	
	public List<ServerInfo> getServers() {
		return servers;
	}
	
	public OpenAPISpecModel withServers(List<ServerInfo> servers) {
		this.servers = servers;
		return this;
	}
	
	public Map<String, Map<String, EndpointInfo>> getPaths() {
		return paths;
	}
	
	public OpenAPISpecModel withPaths(Map<String, Map<String, EndpointInfo>> paths) {
		this.paths = paths;
		return this;
	}
	
	public ComponentInfo getComponents() {
		return components;
	}
	
	public OpenAPISpecModel withComponents(ComponentInfo components) {
		this.components = components;
		return this;
	}
	
	public List<TagInfo> getTags() {
		return this.tags;
	}
	
	public OpenAPISpecModel withTags(List<TagInfo> tags) {
		this.tags = tags;
		return this;
	}
	
	/**
	 * Generates a JSONObject based on the state of this OpenAPISpecModel.
	 * 
	 * @return the JSON representation of this object.
	 */
	public JSONObject generateJSON() {
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return new JSONObject(json);
	}

	@Override
	public int hashCode() {
		return Objects.hash(components, info, openapi, paths, servers, tags);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenAPISpecModel other = (OpenAPISpecModel) obj;
		return Objects.equals(components, other.components) && Objects.equals(info, other.info)
				&& Objects.equals(openapi, other.openapi) && Objects.equals(paths, other.paths)
				&& Objects.equals(servers, other.servers) && Objects.equals(tags, other.tags);
	}

	@Override
	public String toString() {
		return "OpenAPISpecModel [openapi=" + openapi + ", info=" + info + ", servers=" + servers + ", paths=" + paths
				+ ", components=" + components + ", tags=" + tags + "]";
	}
}
