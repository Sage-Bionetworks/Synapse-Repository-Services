package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.repo.model.grid.query.RowValidationResultFilter;
import org.sagebionetworks.repo.model.grid.query.ValidationOperator;
import org.sagebionetworks.util.ValidateArgument;

public class RowValidationResultFilterElement implements FilterElement {

	private ValidationOperator operator;
	private String validationResultValue;

	public RowValidationResultFilterElement(Filter filter) {
		this((RowValidationResultFilter) filter);
	}

	public RowValidationResultFilterElement(RowValidationResultFilter filter) {
		ValidateArgument.required(filter, "filter");
		this.operator = filter.getOperator();
		this.validationResultValue = filter.getValidationResultValue();
	}

	public RowValidationResultFilterElement() {}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		ValidateArgument.required(operator, "operator");
		ValidateArgument.required(validationResultValue, "validationResultValue");

		String rhs = (operator == ValidationOperator.LIKE) ? " IS NOT NULL" : " IS NULL";
		if (operator != ValidationOperator.LIKE && operator != ValidationOperator.NOT_LIKE) {
			throw new IllegalArgumentException("Unknown operation: " + operator);
		}

		String bind = "val" + params.size();
		sqlBuilder.append(" JSON_SEARCH(VAL_RES, 'one', :").append(bind).append(", NULL, '$')").append(rhs);
		params.put(bind, validationResultValue);
	}

	public ValidationOperator getOperator() {
		return operator;
	}

	public RowValidationResultFilterElement setOperator(ValidationOperator operator) {
		this.operator = operator;
		return this;
	}

	public String getValidationResultValue() {
		return validationResultValue;
	}

	public RowValidationResultFilterElement setValidationResultValue(String validationResultValue) {
		this.validationResultValue = validationResultValue;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operator, validationResultValue);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowValidationResultFilterElement other = (RowValidationResultFilterElement) obj;
		return operator == other.operator && Objects.equals(validationResultValue, other.validationResultValue);
	}

	@Override
	public String toString() {
		return "RowValidationResultFilterElement [operator=" + operator + ", validationResultValue="
				+ validationResultValue + "]";
	}


}
