package org.sagebionetworks.controller.model;

import java.util.List;
import java.util.Objects;

import org.json.JSONObject;

import com.google.gson.Gson;

/**
 * Closely represents the information found in a controller.
 * @author lli
 *
 */
public class ControllerModel {
	private String path; // The path to the controller.
	private String displayName; // The name of the controller.
	private List<MethodModel> methods;

	public String getPath() {
		return path;
	}
	
	public ControllerModel withPath(String path) {
		this.path = path;
		return this;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public ControllerModel withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}
	
	public List<MethodModel> getMethods() {
		return methods;
	}
	
	public ControllerModel withMethods(List<MethodModel> methods) {
		this.methods = methods;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(displayName, methods, path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControllerModel other = (ControllerModel) obj;
		return Objects.equals(displayName, other.displayName) && Objects.equals(methods, other.methods)
				&& Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return "ControllerModel [path=" + path + ", displayName=" + displayName + ", methods=" + methods + "]";
	}	
}
