package org.sagebionetworks.web.client.view.table.column.provider;

import org.sagebionetworks.web.shared.ColumnInfo.Type;

public class BooleanColumnProvider extends AbstractColumnProvider<Boolean> {
	
	private static Type[] SUPPORTED = new Type[]{Type.Boolean, Type.BooleanArray};

	@Override
	public Type[] supportedTypes() {
		return SUPPORTED;
	}

	@Override
	public String valueToString(Boolean value) {
		return Boolean.toString(value);
	}

}
