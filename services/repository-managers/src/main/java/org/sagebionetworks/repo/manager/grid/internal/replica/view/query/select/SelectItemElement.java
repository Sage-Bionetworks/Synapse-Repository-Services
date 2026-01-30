package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Element;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;

public interface SelectItemElement extends Element {
	/**
	 * Return true if this is an aggregate function.
	 * 
	 * @return
	 */
	boolean isAggregate();
	
	/**
	 * Set the SelectColumns to be returned with the query results.
	 * @param header
	 * @param selectColumns
	 */
	void setSelect(Context context, Long index, List<SelectColumn> selectColumns );
}
