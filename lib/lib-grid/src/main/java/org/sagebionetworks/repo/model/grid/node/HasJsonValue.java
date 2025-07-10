package org.sagebionetworks.repo.model.grid.node;

public interface HasJsonValue<T> {

	/**
	 * Set the value from the passed JSON string.
	 * 
	 * @param json
	 * @return
	 */
	T setValueFromJson(String json);

	/**
	 * Get the JSON representation of the value.
	 * 
	 * @return
	 */
	String getValueAsJson();

}
