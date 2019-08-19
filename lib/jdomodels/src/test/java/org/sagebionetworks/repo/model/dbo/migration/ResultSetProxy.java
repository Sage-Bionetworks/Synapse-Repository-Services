package org.sagebionetworks.repo.model.dbo.migration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;

/**
 * Creates a ResultSet that acts as a proxy for a target DatabaseObject. The
 * resulting ResultSet is used to test the translation of a ResultSet to DBO.
 * a
 *
 */
public class ResultSetProxy implements InvocationHandler {

	DatabaseObject<?> target;
	Map<String, String> mapColumnNameToFieldName;
	boolean wasLastValueReturnedNull;

	/**
	 * New InvocationHandler that wraps the provided target using
	 * the provided mapping from database column names to DBO field names.
	 * @param fields
	 * @param target
	 */
	ResultSetProxy(FieldColumn[] fields, DatabaseObject<?> target) {
		mapColumnNameToFieldName = new HashMap<>(fields.length);
		for (FieldColumn field : fields) {
			mapColumnNameToFieldName.put(field.getColumnName(), field.getFieldName());
		}
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ("wasNull".equals(method.getName())) {
			return wasLastValueReturnedNull;
		}
		// Lookup the field name that matches the column name argument
		String fieldName = mapColumnNameToFieldName.get(args[0]);
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		Object valueFromTarget = field.get(target);
		wasLastValueReturnedNull = valueFromTarget == null;
		return translateTypeAsNeeded(valueFromTarget, field.getType(), method.getReturnType());
	}

	/**
	 * Translate the provided value from one class type to another.
	 * @param value
	 * @param fromType
	 * @param toType
	 * @return
	 */
	static Object translateTypeAsNeeded(Object value, Class fromType, Class toType) {
		if (fromType.equals(toType)) {
			// no translation needed.
			return value;
		} else if (Date.class.equals(fromType)) {
			Date dateValue = (Date) value;
			if (Timestamp.class.equals(toType)) {
				return new Timestamp(dateValue.getTime());
			} else if (long.class.equals(toType)) {
				return dateValue.getTime();
			}
		} else if (Long.class.equals(fromType)) {
			if (long.class.equals(toType)) {
				return value;
			}else if(String.class.equals(toType)) {
				return value.toString();
			}
		}else if (Integer.class.equals(fromType)) {
			if (int.class.equals(toType)) {
				return value;
			}
		}else if (Double.class.equals(fromType)) {
			if (double.class.equals(toType)) {
				return value;
			}
		}  else if (Boolean.class.equals(fromType)) {
			if (boolean.class.equals(toType)) {
				return value;
			}
		} else if (fromType.isEnum()) {
			Enum enumValue = (Enum) value;
			if (String.class.equals(toType)) {
				return enumValue.name();
			}
		} else if(fromType.isArray()) {
			if(Blob.class.equals(toType)) {
				return BlobProxy.createProxy((byte[]) value);
			}
		}
		throw new IllegalArgumentException(
				"Unsupported translation from type: " + fromType.getName() + " to " + toType.getName());
	}
	
	/**
	 * Creates a ResultSet that acts as a proxy for a target DatabaseObject.
	 * @param fields The fields that define the mapping from database column names to DBO field names.
	 * @param target Data from the provided target object will be translated and returned via the 
	 * resulting ResultSet.
	 * @return
	 */
	public static ResultSet createProxy(final FieldColumn[] fields, final DatabaseObject<?> target) {
		return (ResultSet) Proxy.newProxyInstance(ResultSetProxy.class.getClassLoader(),
				new Class[] { ResultSet.class }, new ResultSetProxy(fields, target));
	}
}
