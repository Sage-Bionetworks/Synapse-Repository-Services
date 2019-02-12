package org.sagebionetworks.repo.model.dbo;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SinglePrimaryKeySqlParameterSource that = (SinglePrimaryKeySqlParameterSource) o;
		return Objects.equals(value, that.value) &&
				Objects.equals(lastParamName, that.lastParamName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, lastParamName);
	}
}
