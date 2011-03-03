package org.sagebionetworks.web.client.presenter;

import java.util.List;

/**
 * Listen to column selection changes.
 * @author jmhill
 *
 */
public interface ColumnSelectionChangeListener {
	
	/**
	 * Called when the column selection changes.
	 * @param newSelection
	 */
	public void columnSelectionChanged(List<String> newSelection);

}
