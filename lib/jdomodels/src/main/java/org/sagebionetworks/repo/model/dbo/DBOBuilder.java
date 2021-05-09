package org.sagebionetworks.repo.model.dbo;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DBOBuilder<T> {

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypeHierarchy(Object.class).build();

	public interface RowMapper {
		public void map(Object result, ResultSet rs) throws SQLException, InvocationTargetException, IllegalAccessException;
	}

	public interface ParamTypeMapper {
		public Object convert(Object result);

		public Integer getSqlType();
	}

	private static abstract class BaseRowMapper implements RowMapper {
		private final Method fieldSetter;
		private final String columnName;
		private final boolean isNullable;

		public BaseRowMapper(Method fieldSetter, String columnName, boolean isNullable) {
			this.fieldSetter = fieldSetter;
			this.columnName = columnName;
			this.isNullable = isNullable;
		}

		public final void map(Object result, ResultSet rs) throws InvocationTargetException, SQLException, IllegalAccessException {
			Object value = getValue(rs, columnName);
			if (isNullable && rs.wasNull()) {
				value = null;
			}
			fieldSetter.invoke(result, value);
		}

		public abstract Object getValue(ResultSet rs, String columnName) throws SQLException, IllegalAccessException,
				InvocationTargetException;
	}

	private static class AssignmentRowMapper extends BaseRowMapper {
		final Method mapperMethod;

		public AssignmentRowMapper(Method fieldSetter, String columnName, boolean nullable, Method mapperMethod) {
			super(fieldSetter, columnName, nullable);
			this.mapperMethod = mapperMethod;
		}

		public Object getValue(ResultSet rs, String columnName) throws SQLException, IllegalAccessException, InvocationTargetException {
			return mapperMethod.invoke(rs, columnName);
		}
	}

	@SuppressWarnings("rawtypes")
	private static class EnumRowMapper extends BaseRowMapper {
		final private Class<? extends Enum> enumType;

		public EnumRowMapper(Method fieldSetter, String columnName, boolean nullable, Class<? extends Enum> enumType) {
			super(fieldSetter, columnName, nullable);
			this.enumType = enumType;
		}

		public Object getValue(ResultSet rs, String columnName) throws SQLException {
			String stringValue = rs.getString(columnName);
			if (stringValue != null) {
				@SuppressWarnings("unchecked")
				Object enumValue = Enum.valueOf(enumType, stringValue);
				return enumValue;
			} else {
				return null;
			}
		}
	}

	private static class BlobRowMapper extends BaseRowMapper {

		public BlobRowMapper(Method fieldSetter, String columnName, boolean nullable) {
			super(fieldSetter, columnName, nullable);
		}

		public Object getValue(ResultSet rs, String columnName) throws SQLException {
			Blob blobValue = rs.getBlob(columnName);
			if (blobValue != null) {
				return blobValue.getBytes(1, (int) blobValue.length());
			} else {
				return null;
			}
		}
	}

	private static class DateRowMapper extends BaseRowMapper {

		public DateRowMapper(Method fieldSetter, String columnName, boolean nullable) {
			super(fieldSetter, columnName, nullable);
		}

		public Object getValue(ResultSet rs, String columnName) throws SQLException {
			Long timestamp = rs.getLong(columnName);
			if (timestamp != null) {
				return new Date(timestamp);
			} else {
				return null;
			}
		}
	}
	
	private static class TimestampRowMapper extends BaseRowMapper {
		public TimestampRowMapper(Method fieldSetter, String columnName, boolean nullable) {
			super(fieldSetter, columnName, nullable);
		}

		public Object getValue(ResultSet rs, String columnName) throws SQLException {
			return  rs.getTimestamp(columnName);
		}
	}
	
	private static class BooleanRowMapper extends BaseRowMapper{

		public BooleanRowMapper(Method fieldSetter, String columnName,
				boolean isNullable) {
			super(fieldSetter, columnName, isNullable);
		}
		
		public Object getValue(ResultSet rs, String columnName) throws SQLException {
			Boolean bool = rs.getBoolean(columnName);
			if(rs.wasNull()){
				return null;
			}else{
				return bool;
			}
		}
		
	}

	private static class SerializedTypeRowMapper extends BaseRowMapper {

		private final Class<?> clazz;

		public SerializedTypeRowMapper(Method fieldSetter, String columnName, boolean nullable, Class<?> clazz) {
			super(fieldSetter, columnName, nullable);
			this.clazz = clazz;
		}

		public Object getValue(ResultSet rs, String columnName) throws SQLException {
			Blob blobValue = rs.getBlob(columnName);
			if (blobValue != null) {
				byte[] bytes = blobValue.getBytes(1, (int) blobValue.length());
				try {
					return clazz.cast(JDOSecondaryPropertyUtils.decompressObject(X_STREAM, bytes));
				} catch (IOException e) {
					throw new SQLException("Error converting type " + clazz.getName() + ": " + e.getMessage(), e);
				}
			} else {
				return null;
			}
		}
	}

	public static String getTableName(Class<?> clazz) {
		Table tableAnnotation = clazz.getAnnotation(Table.class);
		return tableAnnotation.name();
	}

	public static <T> FieldColumn[] getFields(final Class<T> clazz, final String[] customColumns) {
		List<FieldColumn> result = Lists.transform(getAnnotatedFields(clazz, Field.class), new Function<Entry<Field>, FieldColumn>() {
			@Override
			public FieldColumn apply(Entry<Field> fieldEntry) {
				return new FieldColumn(fieldEntry.field.getName(), fieldEntry.annotation.name(), fieldEntry.annotation.primary())
						.withIsBackupId(fieldEntry.annotation.backupId())
						.withIsEtag(fieldEntry.annotation.etag())
						.withIsSelfForeignKey(fieldEntry.annotation.isSelfForeignKey())
						.withHasFileHandleRef(fieldEntry.annotation.hasFileHandleRef());
			}
		});
		return result.toArray(new FieldColumn[result.size()]);
	}

	private static final Object[][] TYPE_MAP = { { Long.class, "getLong" }, { long.class, "getLong" }, { String.class, "getString" } };

	private static String getMethodFromTypeMap(Class<?> type) {
		for (Object[] entry : TYPE_MAP) {
			if (type == entry[0]) {
				return (String) entry[1];
			}
		}
		return null;
	}

	public static <T> RowMapper[] getFieldMappers(final Class<? extends T> clazz, final String[] customColumns) {
		List<Entry<Field>> fields = getAnnotatedFieldsWithoutCustomColums(clazz, Field.class, customColumns);
		List<RowMapper> mappers = Lists.transform(fields, new Function<Entry<Field>, RowMapper>() {
			@Override
			public RowMapper apply(Entry<Field> fieldEntry) {
				Method setterMethod;
				String setterMethodName = "set" + StringUtils.capitalize(fieldEntry.field.getName());
				try {
					setterMethod = clazz.getMethod(setterMethodName, new Class[] { fieldEntry.field.getType() });
				} catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("Could not find method '" + setterMethodName + "' on " + clazz.getName());
				}

				String getMethod = getMethodFromTypeMap(fieldEntry.field.getType());
				if (getMethod != null) {
					try {
						Method mapperMethod = ResultSet.class.getMethod((String) getMethod, new Class[] { String.class });
						return new AssignmentRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable(),
								mapperMethod);
					} catch (NoSuchMethodException e) {
						throw new IllegalArgumentException("Could not find method '" + getMethod + "' on ResultSet");
					}
				}

				// enum?
				if (Enum.class.isAssignableFrom(fieldEntry.field.getType())) {
					@SuppressWarnings("unchecked")
					Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) fieldEntry.field.getType();
					return new EnumRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable(), enumClass);
				}

				if (!StringUtils.isEmpty(fieldEntry.annotation.serialized())) {
					return new SerializedTypeRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable(),
							fieldEntry.field.getType());
				}

				if (fieldEntry.field.getType() == byte[].class) {
					return new BlobRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable());
				}

				if (fieldEntry.field.getType() == Date.class) {
					return new DateRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable());
				}
				
				if (fieldEntry.field.getType() == Timestamp.class) {
					return new TimestampRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable());
				}
				
				if (fieldEntry.field.getType() == Boolean.class) {
					return new BooleanRowMapper(setterMethod, fieldEntry.annotation.name(), fieldEntry.annotation.nullable());
				}


				throw new IllegalArgumentException("No default mapper for type " + fieldEntry.field.getType());
			}
		});
		return mappers.toArray(new RowMapper[mappers.size()]);
	}

	public static <T> Map<String, DBOBuilder.ParamTypeMapper> getParamTypeMappers(final Class<? extends T> clazz, final String[] customColumns) {
		List<Entry<Field>> fields = getAnnotatedFieldsWithoutCustomColums(clazz, Field.class, customColumns);
		ImmutableMap.Builder<String, ParamTypeMapper> mappers = ImmutableMap.builder();
		for (Entry<Field> fieldEntry : fields) {
			// enum?
			if (Enum.class.isAssignableFrom(fieldEntry.field.getType())) {
				mappers.put(fieldEntry.field.getName(), new ParamTypeMapper() {
					@Override
					public Object convert(Object result) {
						return ((Enum<?>) result).name();
					}

					@Override
					public Integer getSqlType() {
						return Types.VARCHAR;
					}
				});
			}

			if (!StringUtils.isEmpty(fieldEntry.annotation.serialized())) {
				mappers.put(fieldEntry.field.getName(), new ParamTypeMapper() {
					@Override
					public Object convert(Object result) {
						try {
							return JDOSecondaryPropertyUtils.compressObject(X_STREAM, result);
						} catch (IOException e) {
							throw new IllegalStateException(e.getMessage(), e);
						}
					}

					@Override
					public Integer getSqlType() {
						return Types.BLOB;
					}
				});
			}

			if (fieldEntry.field.getType() == Date.class) {
				mappers.put(fieldEntry.field.getName(), new ParamTypeMapper() {
					@Override
					public Object convert(Object result) {
						return ((Date) result).getTime();
					}

					@Override
					public Integer getSqlType() {
						return Types.NUMERIC;
					}
				});
			}

			if (fieldEntry.annotation.truncatable() && fieldEntry.field.getType() == String.class) {
				final int maxSize = (fieldEntry.annotation.varchar() > 0) ? fieldEntry.annotation.varchar() : ((fieldEntry.annotation
						.fixedchar() > 0) ? fieldEntry.annotation.fixedchar() : Integer.MAX_VALUE);
				mappers.put(fieldEntry.field.getName(), new ParamTypeMapper() {
					@Override
					public Object convert(Object result) {
						if (result instanceof String) {
							String stringResult = (String) result;
							if (stringResult.length() > maxSize) {
								result = stringResult.substring(0, maxSize);
							}
						}
						return result;
					}

					@Override
					public Integer getSqlType() {
						return null;
					}
				});
			}
		}
		return mappers.build();
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
					+ (entry.annotation.cascadeDelete() ? " on delete cascade" : " on delete restrict"));
		}

		Table tableAnnotation = clazz.getAnnotation(Table.class);
		for (String constraint : tableAnnotation.constraints()) {
			lines.add(constraint);
		}

		StringBuilder sb = new StringBuilder(1000);
		sb.append("CREATE TABLE IF NOT EXISTS " + escapeName(tableName) + " (\n\t");
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
				type = "BIGINT";
			} else if (fieldClazz == Date.class) {
				type = "BIGINT";
			} else if (fieldClazz == String.class) {
				if (fieldAnnotation.varchar() != 0) {
					type = "VARCHAR(" + fieldAnnotation.varchar() + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci";
				} else if (fieldAnnotation.fixedchar() != 0) {
					type = "CHAR(" + fieldAnnotation.fixedchar() + ")";
				} else if (fieldAnnotation.etag()) {
					type = "CHAR(36)";
				} else if (fieldAnnotation.blob() != null) {
					type = fieldAnnotation.blob();
				}  else {
					throw new IllegalArgumentException("No type defined and String field does not have varchar or fixedchar set for "
							+ fieldAnnotation.name() + " on " + owner.getName());
				}
			} else if (Enum.class.isAssignableFrom(fieldClazz)) {
				type = buildEnumType(fieldClazz);
			} else if (!StringUtils.isEmpty(fieldAnnotation.blob())) {
				type = fieldAnnotation.blob();
			} else if (!StringUtils.isEmpty(fieldAnnotation.serialized())) {
				type = fieldAnnotation.serialized();
			} else if (fieldClazz == Boolean.class) {
				type = "bit(1)";
			} else if (fieldClazz == Timestamp.class) {
				type = "timestamp";
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

	/**
	 * Build up an DDL ENUM type from a java enum.
	 * @param fieldClazz
	 * @return
	 */
	private static String buildEnumType(Class<?> fieldClazz) {
		String type;
		Class<? extends Enum> enumClass = (Class<? extends Enum>) fieldClazz;
		StringBuilder builder = new StringBuilder();
		builder.append("ENUM( ");
		Enum[] values =  enumClass.getEnumConstants();
		for(int i=0; i< values.length; i++){
			if(i > 0){
				builder.append(", ");
			}
			builder.append("'").append(values[i].name()).append("'");
		}
		builder.append(")");
		type = builder.toString();
		return type;
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

	private static <T extends Annotation> List<Entry<Field>> getAnnotatedFieldsWithoutCustomColums(Class<?> clazz,
			Class<Field> annotationType, final String[] customColumns) {
		// filter out custom columns. They are handled by the caller
		return Lists.newArrayList(Iterables.filter(getAnnotatedFields(clazz, Field.class), new Predicate<Entry<Field>>() {
			@Override
			public boolean apply(Entry<Field> fieldEntry) {
				// Assumption is that custom columns are rare, and thus a linear search is acceptable
				for (String customColumn : customColumns) {
					if (customColumn.equals(fieldEntry.annotation.name())) {
						return false;
					}
				}
				return true;
			}
		}));
	}
}
