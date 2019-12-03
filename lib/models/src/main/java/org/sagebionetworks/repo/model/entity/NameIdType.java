package org.sagebionetworks.repo.model.entity;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Simple POJO to capture name, id, and type.
 *
 */
public class NameIdType {
	
    private String name;
    private String id;
    private String type;
    
	public String getName() {
		return name;
	}
	public NameIdType withName(String name) {
		this.name = name;
		return this;
	}
	public String getId() {
		return id;
	}
	public NameIdType withId(String id) {
		this.id = id;
		return this;
	}
	public String getType() {
		return type;
	}
	public NameIdType withType(String type) {
		this.type = type;
		return this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		NameIdType other = (NameIdType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "NameIdType [name=" + name + ", id=" + id + ", type=" + type + "]";
	}
    
	/**
	 * Create a List<EntityHeader> copy from a given List<NameIdType>.
	 * @param input
	 * @return
	 */
	public static List<EntityHeader> toEntityHeader(List<NameIdType> input) {
		ValidateArgument.required(input, "input");
		return input.stream().map(t -> {
			EntityHeader header = new EntityHeader();
			header.setName(t.getName());
			header.setId(t.getId());
			header.setType(t.getType());
			return header;
		}).collect(Collectors.toList());
	}
}
