package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.repo.model.grid.query.RowIsValidFilter;
import org.sagebionetworks.util.ValidateArgument;

public class RowIsValidFilterElement implements FilterElement {

	private Boolean value;
	   
	public RowIsValidFilterElement(Filter filter) {
		this((RowIsValidFilter) filter);
	}

	public RowIsValidFilterElement(RowIsValidFilter filter) {
		this.value = filter.getValue();
	}
	
	public RowIsValidFilterElement() {
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		ValidateArgument.required(value, "RowIsValidFilter.value");
		String bind = String.format("b%d", params.size());
		sqlBuilder.append(" VAL_RES->'$.isValid' = CAST(:").append(bind).append(" AS JSON)");
		params.put(bind, value.toString());
	}

	public Boolean getValue() {
		return value;
	}

	public RowIsValidFilterElement setValue(Boolean value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowIsValidFilterElement other = (RowIsValidFilterElement) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "RowIsValidFilterElement [value=" + value + "]";
	}

}
