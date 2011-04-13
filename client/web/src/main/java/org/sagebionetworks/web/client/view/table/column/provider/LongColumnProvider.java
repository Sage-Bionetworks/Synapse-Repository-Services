package org.sagebionetworks.web.client.view.table.column.provider;

import org.sagebionetworks.web.shared.ColumnInfo.Type;

/**
 * A Long to String column provider
 * 
 * @author jmhill
 *
 */
public class LongColumnProvider extends AbstractColumnProvider<Long> {
	
	/**
	 * Supports longs and arrays of longs.
	 */
	private static Type[] SUPPORTS = new Type[]{Type.Long, Type.LongArray};

	@Override
	public Type[] supportedTypes() {
		return SUPPORTS;
	}

	@Override
	public String valueToString(Long value) {
		return Long.toString(value);
	}

}
