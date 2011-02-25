package org.sagebionetworks.web.client.view;

import java.util.Map;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;

/**
 * We need to create new CellTables on the fly, and this allows us to test the view
 * without depending on the real implementation.
 * 
 * @author jmhill
 *
 */
public interface CellTableProvider {
	
	/**
	 * Create a new cell table.
	 * 
	 * @return
	 */
	public CellTable<Map<String, Object>> createNewTable();
	
	/**
	 * Create a new pager.
	 * @return
	 */
	public SimplePager createPager();

}
