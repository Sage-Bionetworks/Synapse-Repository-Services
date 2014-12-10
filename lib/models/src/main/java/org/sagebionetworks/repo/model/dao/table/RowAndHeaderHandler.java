package org.sagebionetworks.repo.model.dao.table;


/**
 * Captures both the headers and rows from 
 * @author jmhill
 *
 */
public interface RowAndHeaderHandler extends RowHandler {

	public void setEtag(String etag);
}
