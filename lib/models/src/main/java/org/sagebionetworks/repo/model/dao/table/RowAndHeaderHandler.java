package org.sagebionetworks.repo.model.dao.table;

import java.util.List;

/**
 * Captures both the headers and rows from 
 * @author jmhill
 *
 */
public interface RowAndHeaderHandler extends RowHandler {

	/**
	 * Called once the ha
	 * @param headers
	 */
	public void setHeaderColumnIds(List<String> headers);

	public void setEtag(String etag);
}
