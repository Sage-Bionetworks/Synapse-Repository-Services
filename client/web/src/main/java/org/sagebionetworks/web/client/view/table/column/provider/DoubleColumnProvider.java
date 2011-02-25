package org.sagebionetworks.web.client.view.table.column.provider;

import org.sagebionetworks.web.shared.ColumnInfo.Type;

/**
 * A very simple Double column provider.
 * 
 * @author jmhill
 *
 */
public class DoubleColumnProvider extends AbstractColumnProvider<Double> {
	
	private static Type[] SUPPORTED = new Type[]{Type.Double, Type.DoubleArray};

	@Override
	public Type[] supportedTypes() {
		return SUPPORTED;
	}

	@Override
	public String valueToString(Double value) {
		return Double.toString(value);
	}

}
