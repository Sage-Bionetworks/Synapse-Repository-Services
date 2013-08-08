package org.sagebionetworks.repo.manager.trash;

import org.sagebionetworks.StackConfiguration;

/**
 * Constants for the trash and trash cans.
 *
 * @author Eric Wu
 */
public class TrashConstants {

	/**
	 * The maximum number of entities that can be moved
	 * into the trash can at one time.
	 */
	public static final int MAX_TRASHABLE = StackConfiguration.getTrashCanMaxTrashable();

	/**
	 * The ID of the bootstrapped trash folder.
	 */
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfiguration.getTrashFolderEntityIdStatic());
}
