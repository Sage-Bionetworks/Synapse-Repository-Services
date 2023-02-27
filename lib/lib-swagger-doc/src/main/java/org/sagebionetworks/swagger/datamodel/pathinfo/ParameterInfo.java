package org.sagebionetworks.swagger.datamodel.pathinfo;

import org.json.JSONObject;

public class ParameterInfo {
	private String name;
	private String in;
	private boolean isRequired;
	private String description;
	private JSONObject schema;
	
	public ParameterInfo(String name, String in, JSONObject schema) {
		this(name, in, true, schema, "");
	}
	
	public ParameterInfo(String name, String in, boolean isRequired, JSONObject schema, String description) {
		if (in.equals("path") && !isRequired) {
			throw new IllegalArgumentException("All parameters in path should be required");
		}
		this.name = name;
		this.in = in;
		this.isRequired = isRequired;
		this.schema = schema;
		this.description = description;
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
