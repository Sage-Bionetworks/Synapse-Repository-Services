package org.sagebionetworks.web.shared;

import java.util.List;

import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;

public class EntityType {

	private String name;
	private String urlPrefix;
	private String className;
	private String defaultParentPath;
	private EntityTypeMetadata metadata;
    private List<EntityType> validParentTypes;
    private List<EntityType> validChildTypes;   
    
	public EntityType(String name, String urlPrefix, String className,
			String defaultParentPath, EntityTypeMetadata metadata) {
		super();
		this.name = name;
		this.urlPrefix = urlPrefix;
		this.className = className;
		this.defaultParentPath = defaultParentPath;
		this.metadata = metadata;
	}

	public List<EntityType> getValidParentTypes() {
		return validParentTypes;
	}

	public void setValidParentTypes(List<EntityType> validParentTypes) {
		this.validParentTypes = validParentTypes;
	}

	public List<EntityType> getValidChildTypes() {
		return validChildTypes;
	}

	public void setValidChildTypes(List<EntityType> validChildTypes) {
		this.validChildTypes = validChildTypes;
	}

	public String getName() {
		return name;
	}

	public String getUrlPrefix() {
		return urlPrefix;
	}

	public String getClassName() {
		return className;
	}

	public String getDefaultParentPath() {
		return defaultParentPath;
	}

	public EntityTypeMetadata getMetadata() {
		return metadata;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime
				* result
				+ ((defaultParentPath == null) ? 0 : defaultParentPath
						.hashCode());
		result = prime * result
				+ ((metadata == null) ? 0 : metadata.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((urlPrefix == null) ? 0 : urlPrefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityType other = (EntityType) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (defaultParentPath == null) {
			if (other.defaultParentPath != null)
				return false;
		} else if (!defaultParentPath.equals(other.defaultParentPath))
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (urlPrefix == null) {
			if (other.urlPrefix != null)
				return false;
		} else if (!urlPrefix.equals(other.urlPrefix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EntityType [name=" + name + ", urlPrefix=" + urlPrefix
				+ ", className=" + className + ", defaultParentPath="
				+ defaultParentPath + ", metadata=" + metadata
				+ ", validParentTypes=" + validParentTypes
				+ ", validChildTypes=" + validChildTypes + "]";
	}
    
}
