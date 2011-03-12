package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Basic header data that is common across columns
 * @author jmhill
 *
 */
public interface HeaderData extends IsSerializable{
	
	/**
	 * The id of this column
	 * @return
	 */
	public String getId();
	
	/**
	 * The String used for the column header
	 * @return
	 */
	public String getDisplayName();
	
	/**
	 * The string used for tool-tips on the column
	 * @return
	 */
	public String getDescription();
	
	/**
	 * What Id should be used for sorting?
	 * For most columns this will be the same as the id.
	 * 
	 * @return
	 */
	public String getSortId();

	/**
	 * Returns the column's default display width in pixels
	 * @return default width for the column
	 */
	public int getColumnWidth();
}
