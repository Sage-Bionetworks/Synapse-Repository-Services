package org.sagebionetworks.repo.manager.table.change;

import java.io.IOException;

import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for a single table change, that includes metadata about the
 * change and methods to optionally load the full table change.
 *
 */
public interface TableChangeMetaData {

	/**
	 * The number assigned to this table change.
	 * 
	 * @return
	 */
	Long getChangeNumber();

	/**
	 * The type of this change.
	 * 
	 * @return
	 */
	TableChangeType getChangeType();

	/**
	 * Load the actual change data for this change.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	ChangeData loadChangeData() throws NotFoundException, IOException;
}
