package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.util.ValidateArgument;

public class CellValueFilterElement implements FilterElement {

	private String columnName;
	private CellValueOperatorElement operator;
	private List<Object> value;

	public CellValueFilterElement(Filter filter) {
		this((CellValueFilter) filter);
	}

	public CellValueFilterElement(CellValueFilter filter) {
		ValidateArgument.required(filter, "filter");
		this.columnName = filter.getColumnName();
		this.operator = CellValueOperatorElement.valueOf(filter.getOperator().name());
		this.value = Collections.unmodifiableList(filter.getValue());
	}

	public CellValueFilterElement() {}

	// Getters and setters with method chaining
	public String getColumnName() { return columnName; }
	public CellValueFilterElement setColumnName(String columnName) { this.columnName = columnName; return this; }
	public CellValueOperatorElement getOperator() { return operator; }
	public CellValueFilterElement setOperator(CellValueOperatorElement operator) { this.operator = operator; return this; }
	public List<Object> getValue() { return value; }
	public CellValueFilterElement setValue(Object... value) { this.value = Arrays.asList(value); return this; }
	public CellValueFilterElement setValue(List<Object> value) { this.value = value; return this; }

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		validateInputs(sqlBuilder, params, context);
		
		String bind = "val" + params.size();
		Integer columnIndex = context.getColumnIndexForName(columnName);

		switch (operator.getValueMultiplicity()) {
			case one:
				handleSingleValue(sqlBuilder, params, bind, columnIndex);
				break;
			case many:
				handleMultipleValues(sqlBuilder, params, bind, columnIndex);
				break;
			case none:
				handleNoValue(sqlBuilder, columnIndex);
				break;
			default:
				throw new IllegalArgumentException("Unknown operation: " + operator);
		}
	}

	private void validateInputs(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		ValidateArgument.required(sqlBuilder, "sqlBuilder");
		ValidateArgument.required(params, "params");
		ValidateArgument.required(context, "context");
		ValidateArgument.required(context.getHeader(), "context.header");
		ValidateArgument.required(context.getHeader().getOrderedColumns(), "context.header.orderedColumns");
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(operator, "operator");
	}

	private void handleSingleValue(StringBuilder sqlBuilder, Map<String, Object> params, String bind, Integer columnIndex) {
		if (value == null || value.size() != 1) {
			throw new IllegalArgumentException("Expected exactly one value for operation: " + operator);
		}
		
		Object val = value.get(0);
		String function = isString(val) ? "->>" : "->";
		sqlBuilder.append(" VALS").append(function).append("'$[").append(columnIndex).append("]' ").append(operator.toSql());

		if (isJsonType(val)) {
			sqlBuilder.append(" CAST(:").append(bind).append(" AS JSON)");
			params.put(bind, val.toString());
		} else {
			sqlBuilder.append(" :").append(bind);
			params.put(bind, val);
		}
	}

	private void handleMultipleValues(StringBuilder sqlBuilder, Map<String, Object> params, String bind, Integer columnIndex) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("Expected at least one value for operation: " + operator);
		}
		
		sqlBuilder.append(" VALS->'$[").append(columnIndex).append("]' ").append(operator.toSql());
		sqlBuilder.append(" (:").append(bind).append(")");
		
		List<Object> toBind = value.stream()
			.map(o -> isJsonType(o) ? o.toString() : o)
			.collect(Collectors.toList());
		params.put(bind, toBind);
	}

	private void handleNoValue(StringBuilder sqlBuilder, Integer columnIndex) {
		if (value != null && !value.isEmpty()) {
			throw new IllegalArgumentException("Expected no value for operator: " + operator);
		}
		sqlBuilder.append(" JSON_VALUE(VALS, '$[").append(columnIndex).append("]') ").append(operator.toSql());
	}

	private boolean isJsonType(Object val) {
		return val instanceof JSONArray || val instanceof Boolean || val instanceof JSONObject;
	}
	
	private boolean isString(Object val) {
		return val instanceof String;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, operator, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		CellValueFilterElement other = (CellValueFilterElement) obj;
		return Objects.equals(columnName, other.columnName) && operator == other.operator && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "CellValueFilterElement [columnName=" + columnName + ", operator=" + operator + ", value=" + value + "]";
	}
}
