package org.sagebionetworks.repo.manager.trash;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Constants for the trash and trash cans.
 */
public class TrashConstants {

	/**
	 * The maximum number of entities that can be moved
	 * into the trash can at one time.
	 */
	public static final int MAX_TRASHABLE =
			StackConfiguration.getTrashCanMaxTrashable();

	/**
	 * The ID of the bootstrapped trash folder.
	 */
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfiguration.getTrashFolderEntityIdStatic());
	
	/**
	 * The String value for the trash folder ID.
	 */
	public static final String TRASH_FOLDER_ID_STRING = KeyFactory.keyToString(TRASH_FOLDER_ID);
}
