package org.sagebionetworks.repo.model.dbo;

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class CombinedSqlParameterSource extends AbstractSqlParameterSource {
	private MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
	private BeanPropertySqlParameterSource beanPropertySqlParameterSource;

	public CombinedSqlParameterSource(Object beanObject) {
		this.beanPropertySqlParameterSource = new BeanPropertySqlParameterSource(beanObject);
	}

	public void addValue(String paramName, Object value) {
		mapSqlParameterSource.addValue(paramName, value);
	}

	public boolean hasValue(String paramName) {
		return mapSqlParameterSource.hasValue(paramName) || beanPropertySqlParameterSource.hasValue(paramName);
	}

	public Object getValue(String paramName) {
		return mapSqlParameterSource.hasValue(paramName) ? mapSqlParameterSource.getValue(paramName) : beanPropertySqlParameterSource
				.getValue(paramName);
	}

	public int getSqlType(String paramName) {
		return mapSqlParameterSource.hasValue(paramName) ? mapSqlParameterSource.getSqlType(paramName) : beanPropertySqlParameterSource
				.getSqlType(paramName);
	}
}
