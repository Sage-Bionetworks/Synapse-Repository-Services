package org.sagebionetworks.repo.model.dbo.migration;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Date;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;

public class DBOTestUtils {

	/**
	 * Create a sample DBO object from the provided MigratableDatabaseObject
	 * definition. All fields defined as FieldColumns will be assigned a sample
	 * value in the resulting sample DBO.
	 * 
	 * @param object
	 * @return
	 */
	public static DatabaseObject<?> createSampleObjectForType(MigratableDatabaseObject object) {
		try {
			DatabaseObject<?> sample = (DatabaseObject<?>) object.getDatabaseObjectClass().newInstance();
			int index = 0;
			for (FieldColumn fieldColumn : object.getTableMapping().getFieldColumns()) {
				Field field = sample.getClass().getDeclaredField(fieldColumn.getFieldName());
				field.setAccessible(true);
				Object value = createValue(sample, field, index++);
				field.set(sample, value);
			}
			return sample;
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException("Failed to create a sample DBO for class: "+object.getClass().getName(), e);
		}
	}

	/**
	 * Assign a sample value to the provide field of the provided DBO object.
	 * The sample value will be based on the provided index.
	 * @param object
	 * @param field
	 * @param index
	 * @return
	 */
	static Object createValue(DatabaseObject<?> object, Field field, int index) {
		if (String.class.equals(field.getType())) {
			return "a" + index;
		} else if (Boolean.class.equals(field.getType()) || boolean.class.equals(field.getType())) {
			if (index % 2 > 0) {
				return Boolean.FALSE;
			} else {
				return Boolean.TRUE;
			}
		} else if (Long.class.equals(field.getType()) || long.class.equals(field.getType())) {
			return new Long(index);
		} else if (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) {
			return new Integer(index);
		} else if (Double.class.equals(field.getType())) {
			return new Double(Math.PI * index);
		} else if (Date.class.equals(field.getType())) {
			return new Date(1000 * index);
		} else if (Timestamp.class.equals(field.getType())) {
			return new Timestamp(1000 * index);
		} else if (field.getType().isEnum()) {
			return field.getType().getEnumConstants()[0];
		} else if (field.getType().isArray()) {
			return new byte[] {(byte) index};
		}
		throw new IllegalArgumentException("Unsupported DBO field type: " + field.getType().getName()+" from: "+object.getClass().getName());
	}

}
