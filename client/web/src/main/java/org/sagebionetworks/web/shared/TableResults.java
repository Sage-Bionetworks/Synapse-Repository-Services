package org.sagebionetworks.web.shared;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TableResults implements IsSerializable {
	
	private List<HeaderData> columnInfoList;
	
	private int totalNumberResults;
	private List<Map<String, Object>> rows;
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

}
