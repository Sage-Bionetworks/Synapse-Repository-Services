package org.sagebionetworks.repo.model.dbo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.repo.model.dbo.DBOBuilder.ParamTypeMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

/**
 * Implementation of a table mapping that uses @{@link Table} and @{@link Field} annotations to generate all the boiler
 * plate. This mean fewer duplication in the class itself and no need for a separate sql ddl file
 * 
 * @author marcel
 */
public class AutoTableMapping<T> implements TableMapping<T> {

	private final Class<? extends T> clazz;
	private final String tableName;
	private final FieldColumn[] fields;
	private final DBOBuilder.RowMapper[] mappers;
	private final Map<String, DBOBuilder.ParamTypeMapper> paramTypeMappers;

	/**
	 * Create a table mapping for this class (assumed to be annotated with @{@link Table} and @{@link Field}
	 * 
	 * @param clazz
	 * @param customColumns the columns that cannot use a default mapping and are handled by overriding
	 *        {@link TableMapping#mapRow(ResultSet, int)}
	 * @return
	 */
	public static <T> TableMapping<T> create(Class<? extends T> clazz, String... customColumns) {
		return new AutoTableMapping<T>(clazz, customColumns);
	}

	public AutoTableMapping(Class<? extends T> clazz, String... customColumns) {
		this.clazz = clazz;
		this.fields = DBOBuilder.getFields(clazz, customColumns);
		this.tableName = DBOBuilder.getTableName(clazz);
		this.mappers = DBOBuilder.getFieldMappers(clazz, customColumns);
		this.paramTypeMappers = DBOBuilder.getParamTypeMappers(clazz, customColumns);
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public String getDDLFileName() {
		// the one and only caller of this method should call getDLL instead
		throw new IllegalStateException("getDDLFileName should not be called on AutoTableMapping");
	}

	@Override
	public FieldColumn[] getFieldColumns() {
		return fields;
	}

	@Override
	public Class<? extends T> getDBOClass() {
		return clazz;
	}

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		try {
			T result = clazz.newInstance();
			for (DBOBuilder.RowMapper mapper : mappers) {
				mapper.map(result, rs);
			}
			return result;
		} catch (IllegalArgumentException e) {
			throw new SQLException("Error creating " + clazz.getName() + " object: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new SQLException("Error creating " + clazz.getName() + " object: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new SQLException("Error creating " + clazz.getName() + " object: " + e.getMessage(), e);
		} catch (InstantiationException e) {
			throw new SQLException("Error creating " + clazz.getName() + " object: " + e.getMessage(), e);
		}
	}

	public String getDDL() {
		return DBOBuilder.buildDLL(clazz, tableName);
	}

	public BeanPropertySqlParameterSource getSqlParameterSource(Object bean) {
		BeanPropertySqlParameterSource parameterSource = new BeanPropertySqlParameterSource(bean) {
			@Override
			public Object getValue(String paramName) {
				Object result = super.getValue(paramName);
				if (result != null) {
					ParamTypeMapper paramTypeMapper = paramTypeMappers.get(paramName);
					if (paramTypeMapper != null) {
						return paramTypeMapper.convert(result);
					}
				}
				return result;
			}
		};
		for (Entry<String, ParamTypeMapper> entry : paramTypeMappers.entrySet()) {
			Integer sqlType = entry.getValue().getSqlType();
			if (sqlType != null) {
				parameterSource.registerSqlType(entry.getKey(), sqlType);
			}
		}
		return parameterSource;
	}
}
