package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;

/**
 * Abstraction for Table Row management.
 * 
 * @author jmhill
 *
 */
public interface TableRowManager {
	
	/**
	 * Store a RowSet
	 * @param user
	 * @param delta
	 * @return
	 */
	public RowReferenceSet storeChangeSet(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta);

}
