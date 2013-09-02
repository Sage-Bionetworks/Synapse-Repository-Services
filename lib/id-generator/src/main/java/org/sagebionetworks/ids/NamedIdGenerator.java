package org.sagebionetworks.ids;

/**
 * This ID generator will generate the same ID for a given name.
 * 
 * @author John
 * 
 */
public interface NamedIdGenerator {

	public enum NamedType {
		USER_GROUP_ID,
		NAME_TEST_ID
	}

	/**
	 * Generate a new ID for a given name. If an ID already exits for this name
	 * then that will be returned.
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	Long generateNewId(String name, NamedType type);

	/**
	 * Unconditionally assign the passed ID to the passed name. If the passed
	 * name is already assigned to another ID, then the old ID will be deleted!
	 * If the passed ID already exists and is assigned to another name, then old
	 * name will be overwritten with the passed name! Again, this method will
	 * unconditionally assign the passed ID to the passed name.
	 * 
	 * @param idToLock
	 * @param name
	 * @param type
	 */
	void unconditionallyAssignIdToName(Long idToLock, String name, NamedType type);
	
	/**
	 * Clear all rows for this type.
	 * @param type
	 */
	public void truncateTable(NamedType type);

}
