package org.sagebionetworks.repo.model.dbo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class DBOBuilder<T> {

	public interface RowMapper {
		public <T> void map(T result, ResultSet rs) throws ReflectiveOperationException, SQLException;
	}

	public static class AssignmentRowMapper implements RowMapper {
		final Method fieldSetter;
		final Method mapperMethod;
		final String columnName;

		public AssignmentRowMapper(Method fieldSetter, Method mapperMethod, String columnName) {
			this.fieldSetter = fieldSetter;
			this.mapperMethod = mapperMethod;
			this.columnName = columnName;
		}

		public <T> void map(T result, ResultSet rs) throws ReflectiveOperationException {
			fieldSetter.invoke(result, mapperMethod.invoke(rs, columnName));
		}
	}

	@SuppressWarnings("rawtypes")
	public static class EnumRowMapper implements RowMapper {
		final java.lang.reflect.Field field;
		final String columnName;
		final Class<? extends Enum> enumType;

		public EnumRowMapper(java.lang.reflect.Field field, String columnName, Class<? extends Enum> enumType) {
			this.field = field;
			this.columnName = columnName;
			this.enumType = enumType;
		}

		public <T> void map(T result, ResultSet rs) throws ReflectiveOperationException, SQLException {
			String stringValue = rs.getString(columnName);
			@SuppressWarnings("unchecked")
			Object enumValue = Enum.valueOf(enumType, stringValue);
			field.set(result, enumValue);
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

	private static final Object[][] TYPE_MAP = {
		{ Long.class, "getLong" },
		{ String.class, "getString" },
		{ java.util.Date.class, "getDate" }
	};

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static RowMapper[] getFieldMappers(final Class clazz) {
		List<RowMapper> mappers = Lists.transform(getAnnotatedFields(clazz, Field.class), new Function<Entry<Field>, RowMapper>() {
			@Override
			public RowMapper apply(Entry<Field> fieldEntry) {
				for (Object[] entry : TYPE_MAP) {
					if (fieldEntry.field.getType() == entry[0]) {
						try {
							Method mapperMethod = ResultSet.class.getMethod((String) entry[1], new Class[] { String.class });
							Method setterMethod = clazz.getMethod("set" + StringUtils.capitalize(fieldEntry.field.getName()),
									new Class[] { fieldEntry.field.getType() });
							return new AssignmentRowMapper(setterMethod, mapperMethod, fieldEntry.annotation.name());
						} catch (ReflectiveOperationException e) {
							throw new IllegalArgumentException("Could not find method '" + entry[1] + "' on ResultSet");
						}
					}
				}

				// enum?
				if (Enum.class.isAssignableFrom(fieldEntry.field.getType())) {
					return new EnumRowMapper(fieldEntry.field, fieldEntry.annotation.name(), (Class<? extends Enum>) fieldEntry.field
							.getType());
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
			String fkName = makeFkName(fieldName, tableName);
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
					type = "VARCHAR (" + fieldAnnotation.varchar() + ") CHARACTER SET latin1 COLLATE latin1_bin";
				} else if (fieldAnnotation.fixedchar() != 0) {
					type = "CHAR (" + fieldAnnotation.fixedchar() + ")";
				} else if (fieldAnnotation.etag()) {
					type = "CHAR (36)";
				} else {
					throw new IllegalArgumentException("No type defined and String field does not have varchar or fixedchar set for "
							+ fieldAnnotation.name() + " on " + owner.getName());
				}
			} else if (Enum.class.isAssignableFrom(fieldClazz)) {
				type = "CHAR (" + 32 + ")";
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
		parts.add(fieldAnnotation.sql());
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
