package org.sagebionetworks.web.shared;

import java.util.List;

import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * 
 * @author jmhill
 *
 */
public class FilterEnumeration implements IsSerializable {
	
	private String columnId;
	private String defaultValue;
	private WhereOperator operator;
	private List<DisplayableValue> values;
	
	/**
	 * Needed for RPC
	 */
	public FilterEnumeration(){
		
	}
	
	/**
	 * For tests
	 * @param columnId
	 * @param defaultValue
	 * @param operator
	 * @param values
	 */
	public FilterEnumeration(String columnId, String defaultValue,
			WhereOperator operator, List<DisplayableValue> values) {
		super();
		this.columnId = columnId;
		this.defaultValue = defaultValue;
		this.operator = operator;
		this.values = values;
	}


	public String getColumnId() {
		return columnId;
	}
	public void setColumnId(String columnId) {
		this.columnId = columnId;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public List<DisplayableValue> getValues() {
		return values;
	}
	public void setValues(List<DisplayableValue> values) {
		this.values = values;
	}
	/**
	 * Enums are not supported in GWT RPC serialization.
	 * @return
	 */
	public String getOperator() {
		return operator.name();
	}
	/**
	 * Enums are not supported in GWT RPC serialization.
	 * @param operator
	 */
	public void setOperator(String operator) {
		this.operator = WhereOperator.valueOf(operator);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnId == null) ? 0 : columnId.hashCode());
		result = prime * result
				+ ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilterEnumeration other = (FilterEnumeration) obj;
		if (columnId == null) {
			if (other.columnId != null)
				return false;
		} else if (!columnId.equals(other.columnId))
			return false;
		if (defaultValue == null) {
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (operator != other.operator)
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FilterEnumeration [columnId=" + columnId + ", defaultValue="
				+ defaultValue + ", operator=" + operator + ", values="
				+ values + "]";
	}

}
