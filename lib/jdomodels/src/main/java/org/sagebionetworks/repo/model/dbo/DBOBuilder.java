package org.sagebionetworks.repo.model.dbo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DBOBuilder<T> {

	public static abstract interface RowMapper {
		public abstract void map(Object result, ResultSet rs) throws ReflectiveOperationException, SQLException;
	}

	private static abstract class BaseRowMapper implements RowMapper {
		final private Method fieldSetter;
		private final String columnName;

		public BaseRowMapper(Method fieldSetter, String columnName) {
			this.fieldSetter = fieldSetter;
			this.columnName = columnName;
		}

		public final void map(Object result, ResultSet rs) throws ReflectiveOperationException, SQLException {
			fieldSetter.invoke(result, getValue(rs, columnName));
		}

		public abstract Object getValue(ResultSet rs, String columnName) throws ReflectiveOperationException, SQLException;
	}

	private static class AssignmentRowMapper extends BaseRowMapper {
		final Method mapperMethod;

		public AssignmentRowMapper(Method fieldSetter, String columnName, Method mapperMethod) {
			super(fieldSetter, columnName);
			this.mapperMethod = mapperMethod;
		}

		public Object getValue(ResultSet rs, String columnName) throws ReflectiveOperationException, SQLException {
			return mapperMethod.invoke(rs, columnName);
		}
	}

	@SuppressWarnings("rawtypes")
	private static class EnumRowMapper extends BaseRowMapper {
		final private Class<? extends Enum> enumType;

		public EnumRowMapper(Method fieldSetter, String columnName, Class<? extends Enum> enumType) {
			super(fieldSetter, columnName);
			this.enumType = enumType;
		}

		public Object getValue(ResultSet rs, String columnName) throws ReflectiveOperationException, SQLException {
			String stringValue = rs.getString(columnName);
			@SuppressWarnings("unchecked")
			Object enumValue = Enum.valueOf(enumType, stringValue);
			return enumValue;
		}
	}

	private static class BlobRowMapper extends BaseRowMapper {

		public BlobRowMapper(Method fieldSetter, String columnName) {
			super(fieldSetter, columnName);
		}

		public Object getValue(ResultSet rs, String columnName) throws ReflectiveOperationException, SQLException {
			Blob blobValue = rs.getBlob(columnName);
			if (blobValue != null) {
				return blobValue.getBytes(1, (int) blobValue.length());
			} else {
				return null;
			}
		}
	}

	private static class DateRowMapper extends BaseRowMapper {

		public DateRowMapper(Method fieldSetter, String columnName) {
			super(fieldSetter, columnName);
		}

		public Object getValue(ResultSet rs, String columnName) throws ReflectiveOperationException, SQLException {
			Timestamp ts = rs.getTimestamp(columnName);
			if (ts != null) {
				return new Date(ts.getTime());
			} else {
				return null;
			}
		}
	}

	public static String getTableName(Class<?> clazz) {
		Table tableAnnotation = clazz.getAnnotation(Table.class);
		return tableAnnotation.name();
	}

	public static <T> FieldColumn[] getFields(Class<T> clazz) {
		List<FieldColumn> result = Lists.transform(getAnnotatedFields(clazz, Field.class), new Function<Entry<Field>, FieldColumn>() {
			@Override
			public FieldColumn apply(Entry<Field> fieldEntry) {
				return new FieldColumn(fieldEntry.field.getName(), fieldEntry.annotation.name(), fieldEntry.annotation.primary())
						.withIsBackupId(fieldEntry.annotation.backupId()).withIsEtag(fieldEntry.annotation.etag());
			}
		});
		return result.toArray(new FieldColumn[result.size()]);
	}

	private static final Object[][] TYPE_MAP = { { Long.class, "getLong" }, { String.class, "getString" } };

	public static <T> RowMapper[] getFieldMappers(final Class<? extends T> clazz, final String[] customColumns) {
		List<Entry<Field>> fields = getAnnotatedFields(clazz, Field.class);
		// filter out custom columns. They are handled by the caller
		fields = Lists.newArrayList(Iterables.filter(getAnnotatedFields(clazz, Field.class), new Predicate<Entry<Field>>() {
			@Override
			public boolean apply(Entry<Field> fieldEntry) {
				for (String customColumn : customColumns) {
					if (customColumn.equals(fieldEntry.annotation.name())) {
						return false;
					}
				}
				return true;
			}
		}));
		List<RowMapper> mappers = Lists.transform(fields, new Function<Entry<Field>, RowMapper>() {
			@Override
			public RowMapper apply(Entry<Field> fieldEntry) {
				Method setterMethod;
				String setterMethodName = "set" + StringUtils.capitalize(fieldEntry.field.getName());
				try {
					setterMethod = clazz.getMethod(setterMethodName, new Class[] { fieldEntry.field.getType() });
				} catch (ReflectiveOperationException e) {
					throw new IllegalArgumentException("Could not find method '" + setterMethodName + "' on " + clazz.getName());
				}

				for (Object[] entry : TYPE_MAP) {
					if (fieldEntry.field.getType() == entry[0]) {
						String getMethod = (String) entry[1];
						try {
							Method mapperMethod = ResultSet.class.getMethod((String) getMethod, new Class[] { String.class });
							return new AssignmentRowMapper(setterMethod, fieldEntry.annotation.name(), mapperMethod);
						} catch (ReflectiveOperationException e) {
							throw new IllegalArgumentException("Could not find method '" + getMethod + "' on ResultSet");
						}
					}
				}

				// enum?
				if (Enum.class.isAssignableFrom(fieldEntry.field.getType())) {
					@SuppressWarnings("unchecked")
					Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) fieldEntry.field.getType();
					return new EnumRowMapper(setterMethod, fieldEntry.annotation.name(), enumClass);
				}

				if (fieldEntry.field.getType() == byte[].class) {
					return new BlobRowMapper(setterMethod, fieldEntry.annotation.name());
				}

				if (fieldEntry.field.getType() == Date.class) {
					return new DateRowMapper(setterMethod, fieldEntry.annotation.name());
				}

				throw new IllegalArgumentException("No default mapper for type " + fieldEntry.field.getType());
			}
		});
		return mappers.toArray(new RowMapper[mappers.size()]);
	}

	public static <T> String buildDLL(Class<T> clazz, String tableName) {
		List<String> lines = Lists.newArrayList();
		List<String> primaries = Lists.newArrayList();

		for (Entry<Field> entry : getAnnotatedFields(clazz, Field.class)) {
			lines.add(createFieldDefinition(entry.annotation, entry.field.getType(), clazz));

			if (entry.annotation.primary()) {
				primaries.add(escapeName(entry.annotation.name()));
			}
		}

		if (!primaries.isEmpty()) {
			lines.add("PRIMARY KEY (" + StringUtils.join(primaries, ",") + ")");
		}

		for (Entry<ForeignKey> entry : getAnnotatedFields(clazz, ForeignKey.class)) {
			String fieldName = entry.field.getAnnotation(Field.class).name();
			String fkName = StringUtils.isEmpty(entry.annotation.name()) ? makeFkName(fieldName, tableName) : entry.annotation.name();
			lines.add("constraint " + fkName + " foreign key (" + escapeName(fieldName) + ") references "
					+ escapeName(entry.annotation.table()) + " (" + escapeName(entry.annotation.field()) + ")"
					+ (entry.annotation.cascadeDelete() ? " on delete cascade" : ""));
		}

		Table tableAnnotation = clazz.getAnnotation(Table.class);
		for (String constraint : tableAnnotation.constraints()) {
			lines.add(constraint);
		}

		StringBuilder sb = new StringBuilder(1000);
		sb.append("CREATE TABLE " + escapeName(tableName) + " (\n\t");
		sb.append(StringUtils.join(lines, ",\n\t"));
		sb.append("\n)\n");

		return sb.toString();
	}

	private static String makeFkName(String fieldName, String tableName) {
		return tableName.replaceAll("_([A-Z])[A-Z]+", "_$1") + "_" + fieldName + "_FK";
	}

	private static String createFieldDefinition(Field fieldAnnotation, Class<?> fieldClazz, Class<?> owner) {
		List<String> parts = Lists.newArrayList();

		parts.add(escapeName(fieldAnnotation.name()));
		String type = fieldAnnotation.type();
		if (Strings.isNullOrEmpty(type)) {
			if (fieldClazz == Long.class) {
				type = "bigint(20)";
			} else if (fieldClazz == Date.class) {
				type = "datetime";
			} else if (fieldClazz == String.class) {
				if (fieldAnnotation.varchar() != 0) {
					type = "VARCHAR(" + fieldAnnotation.varchar() + ") CHARACTER SET latin1 COLLATE latin1_bin";
				} else if (fieldAnnotation.fixedchar() != 0) {
					type = "CHAR(" + fieldAnnotation.fixedchar() + ")";
				} else if (fieldAnnotation.etag()) {
					type = "CHAR(36)";
				} else {
					throw new IllegalArgumentException("No type defined and String field does not have varchar or fixedchar set for "
							+ fieldAnnotation.name() + " on " + owner.getName());
				}
			} else if (Enum.class.isAssignableFrom(fieldClazz)) {
				type = "CHAR (" + 32 + ")";
			} else if (!StringUtils.isEmpty(fieldAnnotation.blob())) {
				type = fieldAnnotation.blob();
			} else {
				throw new IllegalArgumentException("No type defined and " + fieldAnnotation.name() + " on " + owner.getName()
						+ " cannot be automatically translated");
			}
		}
		parts.add(type);
		if (!fieldAnnotation.nullable()) {
			parts.add("not null");
		}
		if (fieldAnnotation.defaultNull()) {
			if (!fieldAnnotation.nullable()) {
				throw new IllegalArgumentException("Conflicting annotation on " + fieldAnnotation.name() + " on " + owner.getName()
						+ ": not nullable and default null");
			}
			parts.add("default null");
		}
		if (!StringUtils.isEmpty(fieldAnnotation.sql())) {
			parts.add(fieldAnnotation.sql());
		}
		return StringUtils.join(parts, " ");
	}

	private static String escapeName(String name) {
		return '`' + name + '`';
	}

	private static class Entry<T extends Annotation> {
		java.lang.reflect.Field field;
		T annotation;
	}

	private static <T extends Annotation> List<Entry<T>> getAnnotatedFields(Class<?> clazz, Class<T> annotationType) {
		List<Entry<T>> entries = Lists.newArrayList();
		java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
		for (java.lang.reflect.Field field : fields) {
			T annotation = field.getAnnotation(annotationType);
			if (annotation != null) {
				Entry<T> entry = new Entry<T>();
				entry.field = field;
				entry.annotation = annotation;
				entries.add(entry);
			}
		}
		return entries;
	}
}
