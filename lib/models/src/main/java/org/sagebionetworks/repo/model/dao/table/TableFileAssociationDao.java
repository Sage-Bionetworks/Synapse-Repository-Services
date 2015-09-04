package org.sagebionetworks.repo.model.dao.table;

import java.util.List;
import java.util.Set;

public interface TableFileAssociationDao {

	/**
	 * Bind all of the given FileHandleIds to the given table.
	 * 
	 * @param tableId
	 * @param fileHandleIds
	 */
	public void bindFileHandleIdsToTable(String tableId,
			Set<String> fileHandleIds);

	/**
	 * Given a set of FileHandleIds and a tableId, get the sub-set
	 * of FileHandleIds that are actually associated with the table.
	 * 
	 * @param fileHandleIds
	 * @param tableId
	 * @return
	 */
	public Set<String> getFileHandleIdsAssociatedWithTable(
			List<String> fileHandleIds, String tableId);

}
