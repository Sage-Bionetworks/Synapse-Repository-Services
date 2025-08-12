package org.sagebionetworks.repo.manager.grid.internal.replica.view.filter;

import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;

/**
 * A filer to limit view query results based on a cell's value.
 */
public class CellValueViewFilter implements ViewFilter {

	private Column column;
	private Object value;
	private Operator operator;

	public Column getColumn() {
		return column;
	}

	public CellValueViewFilter setColumn(Column column) {
		this.column = column;
		return this;
	}

	public Object getValue() {
		return value;
	}

	public CellValueViewFilter setValue(Object value) {
		this.value = value;
		return this;
	}

	public Operator getOperator() {
		return operator;
	}

	public CellValueViewFilter setOperator(Operator operator) {
		this.operator = operator;
		return this;
	}

	@Override
	public String getConditionSql(int index) {
		return String.format("JSON_EXTRACT(V1.VEC_VAL, '$.c%d.v') %s :cellValue%d", column.getVectorIndex(),
				operator.getSql(), index);
	}

	@Override
	public String getParameterKey(int index) {
		return String.format("cellValue%d", index);
	}

	@Override
	public Object getParameterValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(column, operator, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellValueViewFilter other = (CellValueViewFilter) obj;
		return Objects.equals(column, other.column) && operator == other.operator && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "CellValueViewFilter [column=" + column + ", value=" + value + ", operator=" + operator + "]";
	}

}
