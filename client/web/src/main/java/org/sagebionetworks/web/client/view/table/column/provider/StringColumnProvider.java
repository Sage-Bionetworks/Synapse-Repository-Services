package org.sagebionetworks.web.client.view.table.column.provider;

import org.sagebionetworks.web.shared.ColumnInfo.Type;

/**
 * Provides columns for Strings and String arrays.
 * 
 * @author jmhill
 * 
 */
public class StringColumnProvider extends AbstractColumnProvider<String> {
	
	private static Type[] SUPPORTED = new Type[]{Type.String, Type.StringArray};

	@Override
	public Type[] supportedTypes() {
		return SUPPORTED;
	}

	@Override
	public String valueToString(String value) {
		return value;
	}

}
