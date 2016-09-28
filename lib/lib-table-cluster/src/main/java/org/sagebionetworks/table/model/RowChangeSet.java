package org.sagebionetworks.table.model;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRow;

public class RowChangeSet {

	ArrayList<ColumnModel> schema;
	List<PartialRow> rows;
	

	public RowChangeSet(List<ColumnModel> schema) {
		this.schema = new ArrayList<ColumnModel>(schema);
		this.rows = new ArrayList<PartialRow>();
	}



	/**
	 * Add a partial row to this change set.
	 * @param row
	 */
	public void addPartialRow(PartialRow row){
		
	}

}
