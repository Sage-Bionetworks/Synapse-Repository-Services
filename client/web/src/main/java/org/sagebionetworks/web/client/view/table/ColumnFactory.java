package org.sagebionetworks.web.client.view.table;

import java.util.Map;

import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.user.cellview.client.Column;

public interface ColumnFactory {
	
	/**
	 * Creates a column that is compatible with the passed metadata.
	 * @param meta
	 * @return
	 */
	Column<Map<String, Object>, ?> createColumn(HeaderData meta);

}
