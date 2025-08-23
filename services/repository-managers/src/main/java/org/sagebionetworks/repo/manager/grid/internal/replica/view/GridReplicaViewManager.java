package org.sagebionetworks.repo.manager.grid.internal.replica.view;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.filter.ViewFilter;

/**
 * Provides a paginated “view” of a grid replica using a specialized query that
 * transforms the CRDT data nodes in the database into a tabular, paginated
 * “grid”. This grid view is read-only.
 */
public interface GridReplicaViewManager {

	/**
	 * Read the header for the given replica.
	 * 
	 * @param gridSessionId
	 * @param replicaId
	 * @return
	 */
	Optional<GridHeader> readHeader(String gridSessionId, Long replicaId);

	/**
	 * Query for a single page of rows with all columns selected without a where
	 * clause ('select * from grid123').
	 * 
	 * @param header
	 * @param limit
	 * @param offset
	 * @return
	 */

	List<RowView> querySinglePage(GridHeader header, Long limit, Long offset);

	/**
	 * Query for a single page of rows with all columns selected using the provided
	 * filters.
	 * 
	 * @param header
	 * @param filters
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<RowView> querySinglePage(GridHeader header, List<ViewFilter> filters, Long limit, Long offset);


    /**
     * Returns an iterator that can be used to retrieve and stream through a grid session's query results.
     * @param header
     * @param filters
     * @return
     */
    Iterator<RowView> getQueryIterator(GridHeader header, List<ViewFilter> filters);

}
