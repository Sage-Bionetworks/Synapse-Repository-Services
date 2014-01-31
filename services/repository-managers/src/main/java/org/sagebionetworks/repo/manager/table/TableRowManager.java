package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for Table Row management.
 * 
 * @author jmhill
 *
 */
public interface TableRowManager {
	
	/**
	 * Append a set of rows to a table.
	 * 
	 * @param user
	 * @param delta
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws IOException 
	 */
	public RowReferenceSet appendRows(UserInfo user, Long tableId, List<ColumnModel> models, RowSet delta) throws DatastoreException, NotFoundException, IOException;

}
