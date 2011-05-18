package org.sagebionetworks.web.client.view.table.column.provider;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.ColumnInfo.Type;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;

/**
 * An abstract column provider.
 * 
 * @author jmhill
 *
 */
@SuppressWarnings({"unchecked"})
public abstract class AbstractColumnProvider<T> implements ColumnProvider {

	@Override
	public boolean isCompatible(ColumnInfo meta) {
		if (meta == null)
			return false;
		if (meta.getType() == null)
			return false;
		ColumnInfo.Type baseType = ColumnInfo.Type.valueOf(meta.getType());
		// Is this type in the list of types
		Type[] types = supportedTypes();
		if(types == null) throw new IllegalArgumentException("supportedTypes() returned null");
		for(Type type: types){
			if(type == baseType) return true;
		}
		return false;
	}

	@Override
	public Column<Map<String, Object>, ?> createColumn(final ColumnInfo meta) {
		final Type type = ColumnInfo.Type.valueOf(meta.getType());
		return new TextColumn<Map<String, Object>>() {
			@Override
			public String getValue(Map<String, Object> map) {
				Object value = map.get(meta.getId());
				if(value != null){
					if(value.getClass().isArray()){
						T[] array = (T[]) value;
						// Return a comma separated list of strings
						if (array != null) {
							return arrayToString(array);
						}
					}else if(value instanceof List){
						List<T> list = (List<T>) value;

						// Return a comma separated list of strings
						if (list != null) {
							T[] array = (T[]) list.toArray();
							return arrayToString(array);
						}
					}else{
						return valueToString((T)value);
					}
				}
				return null;
			}
		};
	}
	
	/**
	 * Converts the array to a string
	 * @param array
	 * @return
	 */
	private String arrayToString(T[] array){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i != 0) {
				builder.append(", ");
			}
			builder.append(valueToString(array[i]));
		}
		return builder.toString();
	}
	
	/**
	 * Which types are supported?
	 * @return
	 */
	abstract public Type[] supportedTypes();
	
	/**
	 * Convert a single value of the given type to a String
	 * @param type
	 * @param value
	 * @return
	 */
	abstract public String valueToString(T value);

}
