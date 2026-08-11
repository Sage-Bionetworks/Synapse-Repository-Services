package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.util.ValidateArgument;

public class CellValueFilterElement implements FilterElement {

	private String columnName;
	private CellValueOperatorElement operator;
	private Object value;

	public CellValueFilterElement(Filter filter) {
		this((CellValueFilter) filter);
	}

	public CellValueFilterElement(CellValueFilter filter) {
		ValidateArgument.required(filter, "filter");
		ValidateArgument.required(filter.getOperator(), "filter.operator");
		this.columnName = filter.getColumnName();
		this.operator = CellValueOperatorElement.valueOf(filter.getOperator().name());
		this.value = convertValue(filter.getValue());
	}

	public CellValueFilterElement() {
	}

	// Getters and setters with method chaining
	public String getColumnName() {
		return columnName;
	}

	public CellValueFilterElement setColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}

	public CellValueOperatorElement getOperator() {
		return operator;
	}

	public CellValueFilterElement setOperator(CellValueOperatorElement operator) {
		this.operator = operator;
		return this;
	}

	public Object getValue() {
		return value;
	}

	public CellValueFilterElement setValue(Object value) {
		this.value = convertValue(value);
		return this;
	}

	public CellValueFilterElement setValue(List<Object> value) {
		this.value = new JSONArray(value);
		return this;
	}

	/**
	 * Convert value to handle JSONArrayAdapter (from schema-to-pojo deserialization).
	 * If the value is a JSONArrayAdapter, convert it to a plain JSONArray.
	 * @param val the value to convert
	 * @return the converted value
	 */
	private Object convertValue(Object val) {
		if (val instanceof JSONArrayAdapter) {
			// Handle JSONArrayAdapterImpl from schema-to-pojo deserialization
			// Convert to plain JSONArray so it works with instanceof checks and SQL binding
			try {
				String jsonString = ((JSONArrayAdapter) val).toJSONString();
				return new JSONArray(jsonString);
			} catch (Exception e) {
				// If conversion fails, return original value and let validation catch it
				return val;
			}
		}
		return val;
	}

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

	private void handleSingleValue(StringBuilder sqlBuilder, Map<String, Object> params, String bind,
			Integer columnIndex) {
		if (value == null) {
			throw new IllegalArgumentException("Expected exactly one value for operation: " + operator);
		}

		sqlBuilder.append("(");
		appendJsonLengthCheck(sqlBuilder, columnIndex);
		sqlBuilder.append(" ");

		// The value is matched strictly by its JSON type: a scalar value compares against a scalar cell
		// (VALS->>'...v[0]'), while a JSON array value compares against a LIST cell (VALS->'...v[0]' cast
		// to JSON). The grid stores no column type, so this literal-type match is the only way to avoid
		// ambiguity: a wrapped scalar ["A"] does not match a scalar cell "A", and does not match both a
		// scalar and a list cell in a mixed column.
		Object comparisonValue = value;
		boolean singleElementArray = value instanceof JSONArray && ((JSONArray) value).length() == 1;
		boolean equalityOperator = CellValueOperatorElement.EQUALS.equals(operator)
				|| CellValueOperatorElement.NOT_EQUALS.equals(operator);
		if (singleElementArray && !equalityOperator) {
			// Ordering and text operators (>, <, LIKE, ...) against a JSON array are not meaningful, so a
			// single-element array unambiguously refers to its scalar element. Unwrap it.
			comparisonValue = ((JSONArray) value).get(0);
		}
		appendComparison(sqlBuilder, params, bind, columnIndex, comparisonValue);
		sqlBuilder.append(")");
	}

	/**
	 * Append a single {@code VALS-><path> <operator> <value>} comparison, choosing the JSON accessor
	 * ({@code ->>} for strings, {@code ->} otherwise) and binding the value as JSON when it is a
	 * JSON-typed value (array, object, or boolean).
	 */
	private void appendComparison(StringBuilder sqlBuilder, Map<String, Object> params, String bind,
			Integer columnIndex, Object singleValue) {
		String function = isString(singleValue) ? "->>" : "->";
		sqlBuilder.append("VALS").append(function).append("'$[").append(columnIndex).append("].v[0]' ")
				.append(operator.toSql());

		if (isJsonType(singleValue)) {
			sqlBuilder.append(" CAST(:").append(bind).append(" AS JSON)");
			params.put(bind, singleValue.toString());
		} else {
			sqlBuilder.append(" :").append(bind);
			params.put(bind, singleValue);
		}
	}

	private void handleMultipleValues(StringBuilder sqlBuilder, Map<String, Object> params, String bind,
									  Integer columnIndex) {
		if (!(value instanceof JSONArray) || ((JSONArray) value).length() == 0) {
			throw new IllegalArgumentException("Expected at least one value for operation: " + operator);
		}

		JSONArray arrayValue = (JSONArray) value;

		sqlBuilder.append("(");
		appendJsonLengthCheck(sqlBuilder, columnIndex);

		sqlBuilder.append(" VALS->'$[").append(columnIndex).append("].v[0]' ").append(operator.toSql());
		sqlBuilder.append(" (:").append(bind).append(")");

		List<Object> toBind = new ArrayList<>();
		arrayValue.forEach(o -> {
			if (isJsonType(o)) {
				toBind.add(o.toString());
			} else {
				toBind.add(o);
			}
		});
		params.put(bind, toBind);
		sqlBuilder.append(")");
	}

	/**
	 * Append the JSON_LENGTH check to include or exclude undefined/timestamp values.
	 * This logic is shared between single and multiple value handlers.
	 */
	private void appendJsonLengthCheck(StringBuilder sqlBuilder, Integer columnIndex) {
		if (CellValueOperatorElement.NOT_EQUALS.equals(operator) || CellValueOperatorElement.NOT_IN.equals(operator)) {
			// If the check is "!=" or "NOT IN", include undefined/timestamp values (they will never match)
			sqlBuilder.append(" JSON_LENGTH(VALS, '$[").append(columnIndex).append("].v') != 1 OR");
		} else {
			// For all other operators, exclude undefined/timestamp values (they cannot be compared)
			sqlBuilder.append(" JSON_LENGTH(VALS, '$[").append(columnIndex).append("].v') = 1 AND");
		}
	}

	private void handleNoValue(StringBuilder sqlBuilder, Integer columnIndex) {
		if (value != null && !JSONObject.NULL.equals(value)) {
			throw new IllegalArgumentException("Expected no value for operator: " + operator);
		}

		// NOTE: These operators check the entire array, not just the first element.
		sqlBuilder.append(" VALS->'$[").append(columnIndex).append("].v' ").append(operator.toSql());
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
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		CellValueFilterElement other = (CellValueFilterElement) obj;
		return Objects.equals(columnName, other.columnName) && operator == other.operator
				&& Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "CellValueFilterElement [columnName=" + columnName + ", operator=" + operator + ", value=" + value + "]";
	}
}
