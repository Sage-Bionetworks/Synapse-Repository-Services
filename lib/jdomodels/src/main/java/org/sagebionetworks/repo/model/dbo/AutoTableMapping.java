package org.sagebionetworks.repo.model.dbo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AutoTableMapping<T> implements TableMapping<T> {

	private final Class<? extends T> clazz;
	private final String tableName;
	private final FieldColumn[] fields;
	private final DBOBuilder.RowMapper[] mappers;

	public static <T> TableMapping<T> create(Class<? extends T> clazz) {
		return new AutoTableMapping<T>(clazz);
	}

	private AutoTableMapping(Class<? extends T> clazz) {
		this.clazz = clazz;
		this.fields = DBOBuilder.getFields(clazz);
		this.tableName = DBOBuilder.getTableName(clazz);
		this.mappers = DBOBuilder.getFieldMappers(clazz);
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public String getDDLFileName() {
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
		} catch (ReflectiveOperationException e) {
			throw new SQLException("Error creating " + clazz.getName() + " object: " + e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw new SQLException("Error creating " + clazz.getName() + " object: " + e.getMessage(), e);
		}
	}

	public String getDDL() {
		return DBOBuilder.buildDLL(clazz, tableName);
	}
}
