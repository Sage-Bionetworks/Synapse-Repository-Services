package org.sagebionetworks.web.client.widget.entity.children;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreviewData {

	private List<Map<String,String>> rows;
	private List<String> columnDisplayOrder;
	private Map<String, String> columnDescriptions;
	private Map<String, String> columnUnits;
	
	public PreviewData() {
		rows = new ArrayList<Map<String,String>>();
		columnDisplayOrder = new ArrayList<String>();
		columnDescriptions = new HashMap<String, String>();
		columnUnits = new HashMap<String, String>();	
	}
	
	public PreviewData(List<Map<String, String>> rows,
			List<String> columnDisplayOrder,
			Map<String, String> columnDescriptions,
			Map<String, String> columnUnits) {
		super();
		this.rows = rows;
		this.columnDisplayOrder = columnDisplayOrder;
		this.columnDescriptions = columnDescriptions;
		this.columnUnits = columnUnits;
	}

	public List<Map<String, String>> getRows() {
		return rows;
	}

	public void setRows(List<Map<String, String>> rows) {
		this.rows = rows;
	}

	public List<String> getColumnDisplayOrder() {
		return columnDisplayOrder;
	}

	public void setColumnDisplayOrder(List<String> columnDisplayOrder) {
		this.columnDisplayOrder = columnDisplayOrder;
	}

	public Map<String, String> getColumnDescriptions() {
		return columnDescriptions;
	}

	public void setColumnDescriptions(Map<String, String> columnDescriptions) {
		this.columnDescriptions = columnDescriptions;
	}

	public Map<String, String> getColumnUnits() {
		return columnUnits;
	}

	public void setColumnUnits(Map<String, String> columnUnits) {
		this.columnUnits = columnUnits;
	}
		
}
