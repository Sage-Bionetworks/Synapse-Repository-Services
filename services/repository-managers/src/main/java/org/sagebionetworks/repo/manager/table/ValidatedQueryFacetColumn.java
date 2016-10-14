package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.table.FacetRange;
import org.sagebionetworks.repo.model.table.FacetType;

/**
 * An immutable class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	private String columnName;
	private Set<String> columnValues;
	private FacetRange facetRange;
	private FacetType facetType;
	private String valuesSearchConditionString;
	
	/**
	 * Constructor.
	 * @param columnName
	 * @param columnValues
	 * @param facetRange 
	 * @param facetType
	 */
	public ValidatedQueryFacetColumn(String columnName, FacetType facetType, Set<String> columnValues, FacetRange facetRange){
		this.columnName = columnName;
		this.columnValues = new HashSet<>(columnValues);
		this.facetRange = facetRange;
		this.facetType = facetType;
		this.valuesSearchConditionString = createSearchConditionString();
	}
	

	public Set<String> getColumnValues() {
		return new HashSet<>(columnValues);
	}

	public String getColumnName() {
		return columnName;
	}
	
	//returns null if no search conditions are applied
	public String getSearchConditionString(){
		return valuesSearchConditionString;
	}
	
	public FacetType getFacetType(){
		return facetType;
	}
	
	public FacetRange getFacetRange(){
		return facetRange;
	}
	
	private String createSearchConditionString(){
		switch (this.facetType){
		case enumeration:
			return createEnumerationSearchCondition(this.columnValues);
		case range:
			return createRangeSearchCondition(this.facetRange);
		default:
			throw new IllegalArgumentException("Unexpected FacetType");
		}
		
	}
	
	private String createRangeSearchCondition(FacetRange facetRange){
		if(facetRange == null || ( (facetRange.getMin() == null || facetRange.getMin().equals(""))
									&& (facetRange.getMax() == null || facetRange.getMax().equals("")) ) ){
			return null;
		}
		String min = facetRange.getMin();
		String max = facetRange.getMax();
		
		StringBuilder builder = new StringBuilder("(");
		
		//at this point we know at least one value is not null and is not empty string
		builder.append(this.columnName);
		if(min == null){ //only max exists
			builder.append(" <= ");
			builder.append(max);
		}else if (max == null){ //only min exists
			builder.append(" >= ");
			builder.append(min);
		}else{
			builder.append(" BETWEEN ");
			builder.append(min);
			builder.append(" AND ");
			builder.append(max);
		}
		
		builder.append(")");
		return builder.toString();
	}
	
	private String createEnumerationSearchCondition(Set<String> values){
		if(values == null || values.isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder("(");
		int initialSize = builder.length();
		for(String value : values){
			if(builder.length() > initialSize){
				builder.append(" OR ");
			}
			builder.append(columnName);
			builder.append("=");
			builder.append(value);
		}
		return builder.toString();
	}
	

}
