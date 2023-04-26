package org.sagebionetworks.controller.annotations.model;

import java.util.Objects;

public class ControllerInfoModel {
	private String displayName;
	private String path;
	
	public String getDisplayName() {
		return displayName;
	}
	
	public ControllerInfoModel withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}
	
	public String getPath() {
		return path;
	}
	
	public ControllerInfoModel withPath(String path) {
		this.path = path;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(displayName, path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControllerInfoModel other = (ControllerInfoModel) obj;
		return Objects.equals(displayName, other.displayName) && Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return "ControllerInfoModel [displayName=" + displayName + ", path=" + path + "]";
	}
}
