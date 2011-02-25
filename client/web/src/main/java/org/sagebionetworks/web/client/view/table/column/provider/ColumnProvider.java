package org.sagebionetworks.web.client.view.table.column.provider;

import java.util.Map;

import org.sagebionetworks.web.shared.ColumnInfo;

import com.google.gwt.user.cellview.client.Column;

/**

 * @author jmhill
 *
 */
public interface ColumnProvider {
	
	/**
	 * True if this Provider an create a column render 
	 * that is compatible with the given type.
	 * @param toTest
	 * @return
	 */
	public boolean isCompatible(ColumnInfo toTest);
	
	/**
	 * When true is returned for {@link #isCompatible(ColumnInfo)} the
	 * provider will be expected to create a column for this type.
	 * @param meta
	 * @return
	 */
	public Column<Map<String, Object>, ?> createColumn(ColumnInfo meta);

}
