package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;

/**
 * This table row DAO is use for random access to row data.
 * @author John
 *
 */
public interface TableRowRandomAccessDAO extends SecondaryTableRowDAO {

	/**
	 * Given a RowReferenceSet, fetch all of the referenced row data.
	 * 
	 * @param referenceSet
	 * @return
	 */
	public RowSet fetchRowSet(RowReferenceSet referenceSet);
}
