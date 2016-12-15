package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.query.util.TableSqlProcessor;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	
	private String columnName;
	private FacetType facetType;
	private ColumnType columnType;
	private FacetColumnRequest facetColumnRequest;
	private String searchConditionString;
	
	/**
	 * Constructor.
	 * Pass in 
	 * @param columnName name of the column
	 * @param facetType the type of the facet either enum or 
	 * @param columnValues the 
	 * @param facetRange 
	 * 
	 */
	public ValidatedQueryFacetColumn(ColumnModel columnModel, FacetColumnRequest facetColumnRequest){
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.required(columnModel.getName(), "columnModel.name");
		ValidateArgument.required(columnModel.getFacetType(), "columnModel.facetType");
		ValidateArgument.required(columnModel.getColumnType(), "columnModel.columnType");
		ValidateArgument.requirement(facetColumnRequest == null  || columnModel.getName().equals(facetColumnRequest.getColumnName()), "names of the columns must match");
		//checks to make sure that useless parameters are not passed in
		if(facetColumnRequest != null){
			if(FacetType.enumeration.equals(columnModel.getFacetType()) && !(facetColumnRequest instanceof FacetColumnValuesRequest)){
				throw new IllegalArgumentException("facetColumnRequest was not an instance of FacetColumnValuesRequest");
			}
			if(FacetType.range.equals(columnModel.getFacetType()) && !(facetColumnRequest instanceof FacetColumnRangeRequest)){
				throw new IllegalArgumentException("facetColumnRequest was not an instance of FacetColumnRangeRequest");
			}
		}
		
		
		this.columnName = columnModel.getName();
		this.facetType = columnModel.getFacetType();
		this.columnType = columnModel.getColumnType();
		this.facetColumnRequest = facetColumnRequest;
		this.searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetColumnRequest, this.columnType);
	}

	public String getColumnName() {
		return this.columnName;
	}
	
	/**
	 * returns null if there were no filter requests associated with this column
	 * @return
	 */
	public FacetColumnRequest getFacetColumnRequest(){
		return this.facetColumnRequest;
	}
	
	/**
	 * returns null if no search conditions exist
	 * @return
	 */
	public String getSearchConditionString(){
		return this.searchConditionString;
	}
	
	public FacetType getFacetType(){
		return this.facetType;
	}
	


}
