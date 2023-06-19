package org.sagebionetworks.openapi.datamodel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.openapi.datamodel.pathinfo.EndpointInfo;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Models the OpenAPI specification.
 * @author lli
 *
 */
public class OpenAPISpecModel implements JSONEntity {
	private String openapi;
	private ApiInfo info;
	private List<ServerInfo> servers;
	private List<TagInfo> tags;
	// This maps path -> { operation -> endpointInfo}
	private Map<String, Map<String, EndpointInfo>> paths;
	// this maps componentType (schemas, parameters) -> { id -> schema }
	private Map<String, Map<String, JsonSchema>> components;
	
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
	
	public Map<String, Map<String, JsonSchema>> getComponents() {
		return components;
	}
	
	public OpenAPISpecModel withComponents(Map<String, Map<String, JsonSchema>> components) {
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
		try {
			return EntityFactory.createJSONObjectForEntity(this);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
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

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (openapi == null) {
			throw new IllegalArgumentException("The 'openapi' field should not be null.");
		}
		if (info == null) {
			throw new IllegalArgumentException("The 'info' field should not be null.");
		}
		if (paths == null) {
			throw new IllegalArgumentException("The 'paths' field should not be null.");
		}
		if (paths.isEmpty()) {
			throw new IllegalArgumentException("The 'paths' field should not be empty.");
		}
		writeTo.put("openapi", openapi);
		writeTo.put("info", info.writeToJSONObject(writeTo.createNew()));
		
		if (this.servers != null) {
			JSONArrayAdapter servers = writeTo.createNewArray();
			populateServers(servers);
			writeTo.put("servers", servers);
		}
		
		if (this.tags != null) {
			JSONArrayAdapter tags = writeTo.createNewArray();
			populateTags(tags);
			writeTo.put("tags", tags);
		}
		
		JSONObjectAdapter paths = writeTo.createNew();
		populatePaths(paths);
		writeTo.put("paths", paths);
		
		if (this.components != null) {
			JSONObjectAdapter components = writeTo.createNew();
			populateComponents(components);
			writeTo.put("components", components);
		}
		
		return writeTo;
	}
	
	void populateComponents(JSONObjectAdapter components) throws JSONObjectAdapterException {
		for (String componentType : this.components.keySet()) {
			JSONObjectAdapter currentComponent = components.createNew();
			populateCurrentComponent(currentComponent, componentType);
			components.put(componentType, currentComponent);
		}
	}
	
	void populateCurrentComponent(JSONObjectAdapter currentComponent, String componentType) throws JSONObjectAdapterException {
		for (String id : this.components.get(componentType).keySet()) {
			currentComponent.put(id, this.components.get(componentType).get(id).writeToJSONObject(currentComponent.createNew()));
		}
	}
	
	void populatePaths(JSONObjectAdapter paths) throws JSONObjectAdapterException {
		for (String path : this.paths.keySet()) {
			JSONObjectAdapter currentPathAdapter = paths.createNew();
			populateCurrentPath(currentPathAdapter, path);
			paths.put(path, currentPathAdapter);
		}
	}

	void populateCurrentPath(JSONObjectAdapter currentPathAdapter, String currentPath) throws JSONObjectAdapterException {
		for (String operation : this.paths.get(currentPath).keySet()) {
			currentPathAdapter.put(operation,
					this.paths.get(currentPath).get(operation).writeToJSONObject(currentPathAdapter.createNew()));
		}
	}
	
	void populateTags(JSONArrayAdapter tags) throws JSONObjectAdapterException {
		for (int i = 0; i < this.tags.size(); i++) {
			tags.put(i, this.tags.get(i).writeToJSONObject(tags.createNew()));
		}
	}
	
	void populateServers(JSONArrayAdapter servers) throws JSONObjectAdapterException {
		for (int i = 0; i < this.servers.size(); i++) {
			servers.put(i, this.servers.get(i).writeToJSONObject(servers.createNew()));
		}
	}
}
