package org.sagebionetworks.web.shared;

import java.util.List;

import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

import com.google.gwt.user.client.rpc.IsSerializable;

public class WhereCondition implements IsSerializable{
	
	private String id;
	private String operator;
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
		this.operator = operator.name();
		this.value = value;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = WhereOperator.valueOf(operator).name();
	}
	/**
	 * We are using "fetch" to get the real operator 
	 * since GWT RPC does not support enums
	 * @return
	 */
	public WhereOperator fetchOperator(){
		return WhereOperator.valueOf(operator);
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Is this string a number
	 * @param toTest
	 * @return
	 */
	public static boolean isNumber(String toTest){
		if(toTest == null) return false;
		// Use java to do the parsing
		try{
			// Is it a long?
			Long.valueOf(toTest);
			return true;
		}catch(NumberFormatException e){
		}
		try{
			// Is it a double?
			Double.valueOf(toTest);
			return true;
		}catch(NumberFormatException e){
		}
		return false;
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
		builder.append(fetchOperator().toSql());
		builder.append(whiteSpace);
		boolean isNumber = WhereCondition.isNumber(value);
		// Add quotes to non-numbers
		if(!isNumber){
			builder.append("\"");
		}
		builder.append(value);
		if(!isNumber){
			builder.append("\"");
		}
		return builder.toString();
	}
	
	public static String toSql(List<WhereCondition> list, String whiteSpace){
		if(list == null) throw new IllegalStateException("List cannot be null");
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<list.size(); i++){
			if(i != 0){
				builder.append(whiteSpace);
				builder.append("and");
				builder.append(whiteSpace);
			}
			builder.append(list.get(i).toSql(whiteSpace));
		}
		return builder.toString();
	}
	
}
