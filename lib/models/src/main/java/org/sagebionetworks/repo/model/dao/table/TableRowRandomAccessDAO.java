package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;

/**
 * 
 * @author John
 *
 */
public interface TableRowRandomAccessDAO {

	/**
	 * Store a RowSet for random access.
	 * 
	 * @param toStore
	 */
	public void storeRowSet(RowSet toStore);
	
	public RowSet fetchRowSet(RowReferenceSet referenceSet);
}
