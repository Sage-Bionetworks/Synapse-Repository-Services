package org.sagebionetworks.swagger.datamodel.pathinfo;

import org.json.JSONObject;

public class ParameterInfo {
	private String name;
	private String in;
	private boolean isRequired;
	private String description;
	private JSONObject schema;
	
	public ParameterInfo(String name, String in, JSONObject schema) {
		this(name, in, schema, "", true);
	}
	
	public ParameterInfo(String name, String in, JSONObject schema, String description, boolean isRequired) {
		if (in.equals("path") && !isRequired) {
			throw new IllegalArgumentException("All parameters in path should be required");
		}
		this.name = name;
		this.in = in;
		this.schema = schema;
		this.description = description;
		this.isRequired = isRequired;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getIn() {
		return this.in;
	}
	
	public boolean isRequired() {
		return this.isRequired;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public JSONObject getSchema() {
		return this.schema;
	}
}
