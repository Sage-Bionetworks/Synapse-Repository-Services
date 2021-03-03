package org.sagebionetworks.repo.model.helper;

import java.util.function.Consumer;

public interface DaoObjectHelper<T> {

	/**
	 * Abstraction for a helper to create a new Dao object in the database using default values, but
	 * with the option to override any of the values.
	 * 
	 * @param consumer Use the the provided object to override any of the default values.
	 * @return
	 */
	T create(Consumer<T> consumer);
}
