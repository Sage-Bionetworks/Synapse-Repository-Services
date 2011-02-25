package org.sagebionetworks.web.client.view.table.column.provider;

import org.sagebionetworks.web.shared.ColumnInfo.Type;

public class IntegerColumnProvider extends AbstractColumnProvider<Integer> {
	
	private static Type[] SUPPORTED = new Type[]{Type.Integer, Type.IntegerArray};

	@Override
	public Type[] supportedTypes() {
		return SUPPORTED;
	}

	@Override
	public String valueToString(Integer value) {
		return Integer.toString(value);
	}

}
