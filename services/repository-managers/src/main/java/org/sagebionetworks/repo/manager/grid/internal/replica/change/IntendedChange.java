package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import org.json.JSONObject;

/**
 * An object that represents an intended change to a grid document. These
 * objects are used to build new patches.
 */
public interface IntendedChange {

	/**
	 * The type of change.
	 * 
	 * @return
	 */
	IntendedChangeType getType();

	/**
	 * Write this change to JSON.
	 * 
	 * @return
	 */
	JSONObject toJson();

}
