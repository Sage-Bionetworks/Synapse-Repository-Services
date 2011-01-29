package org.sagebionetworks.web.util;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * Compares two objects based on a single field.
 * 
 * The provided field name will determine how the two objects will be compared.
 * Note: The field does not need to be accessible. The resulting FieldComparator
 * will then compare the two objects based on the value of the given field.
 * 
 * @author jmhill
 * 
 * @param <T>
 */
public class FieldComparator<T> implements Comparator<T> {

	Field compareField = null;

	/**
	 * The provided field name will determine how the two objects will be
	 * compared. Note: The field does not need to be accessible. The resulting
	 * FieldComparator will then compare the two objects based on the value of
	 * the given field. a getName() method.
	 * 
	 * @param fieldName
	 */
	public FieldComparator(Class<T> clazz, String fieldName)  {
		try {
			compareField = clazz.getDeclaredField(fieldName);
			// Make sure we can access it even if it is private
			if (!compareField.isAccessible()) {
				compareField.setAccessible(true);
			}
		} catch (Exception e) {
			// convert to a runtime
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compare(T o1, T o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			else
				return -1;
		}
		if (o2 == null)
			return 1;
		// access both fields
		try {
			Object one = compareField.get(o1);
			Object two = compareField.get(o2);
			if (one == null) {
				if (two == null)
					return 0;
				else
					return -1;
			}
			if (two == null)
				return 1;
			// At this point we have two non-null objects
			if (one instanceof Comparable) {
				return ((Comparable) one).compareTo((Comparable) two);
			} else {
				throw new IllegalArgumentException("Unknown type: "
						+ one.getClass().toString());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
