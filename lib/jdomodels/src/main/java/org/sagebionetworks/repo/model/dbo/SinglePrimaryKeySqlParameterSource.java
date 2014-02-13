package org.sagebionetworks.repo.model.dbo;

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;

public class SinglePrimaryKeySqlParameterSource extends AbstractSqlParameterSource {
	private final Object value;
	private String lastParamName = null;

	public SinglePrimaryKeySqlParameterSource(Object value) {
		this.value = value;
	}

	public boolean hasValue(String paramName) {
		checkParamName(paramName);
		return true;
	}

	public Object getValue(String paramName) {
		checkParamName(paramName);
		return value;
	}

	private void checkParamName(String paramName) {
		if (lastParamName != null && !paramName.equals(lastParamName)) {
			throw new IllegalStateException("SinglePrimaryKeySqlParameterSource used with table with multiple primary keys");
		}
	}
}
