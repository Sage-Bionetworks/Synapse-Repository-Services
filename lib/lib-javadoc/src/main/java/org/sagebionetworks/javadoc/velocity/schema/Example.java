package org.sagebionetworks.javadoc.velocity.schema;

public class Example {

	String description;
	String value;
	public Example(String description, String value) {
		super();
		this.description = description;
		this.value = value;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
