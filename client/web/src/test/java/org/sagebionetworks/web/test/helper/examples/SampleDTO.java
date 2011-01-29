package org.sagebionetworks.web.test.helper.examples;

/**
 * A Sample Data Transfer object to for testing.
 * 
 * @author j
 *
 */
public class SampleDTO {
	
	private String name = null;
	private String description = null;
	private int id = 0;
	
	
	public SampleDTO(String name, String description, int id) {
		super();
		this.name = name;
		this.description = description;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		SampleDTO other = (SampleDTO) obj;
		if (id != other.id)
			return false;
		return true;
	}


}
