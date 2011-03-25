package org.sagebionetworks.web.shared;

import java.util.List;

import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

import com.google.gwt.user.client.rpc.IsSerializable;

public class WhereCondition implements IsSerializable{
	
	private String id;
	private WhereOperator operator;
	private String value;
	
	/**
	 * For RPC.
	 */
	public WhereCondition(){
		
	}
	
	/**
	 * All value constructor.
	 * 
	 * @param id
	 * @param operator
	 * @param value
	 */
	public WhereCondition(String id, WhereOperator operator, String value) {
		super();
		this.id = id;
		this.operator = operator;
		this.value = value;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOperator() {
		return operator.name();
	}
	public void setOperator(String operator) {
		this.operator = WhereOperator.valueOf(operator);
	}
	/**
	 * We are using "fetch" to get the real operator 
	 * since GWT RPC does not support enums
	 * @return
	 */
	public WhereOperator fetchOperator(){
		return this.operator;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		WhereCondition other = (WhereCondition) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (operator != other.operator)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	/**
	 * Write this string to sql
	 * @return
	 */
	public String toSql(String whiteSpace){
		if(id == null) throw new IllegalStateException("Id cannot be null");
		if(operator == null) throw new IllegalStateException("Opertator cannot be null");
		if(value == null) throw new IllegalStateException("Value cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(id);
		builder.append(whiteSpace);
		builder.append(operator.toSql());
		builder.append(whiteSpace);
		builder.append("\"");
		builder.append(value);
		builder.append("\"");
		return builder.toString();
	}
	
	public static String toSql(List<WhereCondition> list, String whiteSpace){
		if(list == null) throw new IllegalStateException("List cannot be null");
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<list.size(); i++){
			if(i != 0){
				builder.append(" and ");
			}
			builder.append(list.get(i).toSql(whiteSpace));
		}
		return builder.toString();
	}
	
}
