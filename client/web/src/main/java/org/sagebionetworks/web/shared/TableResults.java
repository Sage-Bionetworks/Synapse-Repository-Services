package org.sagebionetworks.web.shared;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TableResults implements IsSerializable {
	
	private List<HeaderData> columnInfoList;
	
	private int totalNumberResults;
	private List<Map<String, Object>> rows;
	private Exception exception;
	
	/**
	 * We want GWT to serialize every type in this class.
	 */
	private TableValues values = null;
	
	public int getTotalNumberResults() {
		return totalNumberResults;
	}
	public void setTotalNumberResults(int totalNumberResults) {
		this.totalNumberResults = totalNumberResults;
	}
	public List<Map<String, Object>> getRows() {
		return rows;
	}
	public void setRows(List<Map<String, Object>> rows) {
		this.rows = rows;
	}
	public List<HeaderData> getColumnInfoList() {
		return columnInfoList;
	}
	public void setColumnInfoList(List<HeaderData> columnInfoList) {
		this.columnInfoList = columnInfoList;
	}
	public TableValues getValues() {
		return values;
	}
	public void setValues(TableValues values) {
		this.values = values;
	}
	
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}
	/**
	 * Duplicate setter to allow for "rows" to also be called "results"
	 * @param rows
	 */
	public void setResults(List<Map<String, Object>> rows) {
		this.rows = rows;
	}
	
	/**
	 * Duplicate setter to allow for "totalNumberResults" to also be called "totalNumberOfResults"
	 * @param totalNumberResults
	 */
	public void setTotalNumberOfResults(int totalNumberResults) {
		this.totalNumberResults = totalNumberResults;
	}
}
