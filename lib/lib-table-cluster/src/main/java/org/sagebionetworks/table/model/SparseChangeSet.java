package org.sagebionetworks.table.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A sparsely populated matrix of data representing a single table change set.
 *
 */
public class SparseChangeSet {

	List<ColumnModel> schema;
	Map<String, ColumnModel> schemaMap;
	Map<String, Integer> columnIndexMap;
	long versionNumber;
	List<SparseRow> sparseRows;

	/**
	 * Create a new empty change set.
	 * 
	 * @param schema
	 * @param versionNumber
	 */
	public SparseChangeSet(List<ColumnModel> schema, long versionNumber) {
		ValidateArgument.required(schema, "schema");
		sparseRows = new LinkedList<SparseRow>();
		this.versionNumber = versionNumber;
		this.schema = new LinkedList<>(schema);
		schemaMap = new HashMap<String, ColumnModel>(schema.size());
		columnIndexMap = new HashMap<String, Integer>(schema.size());
		for (int i = 0; i < schema.size(); i++) {
			ColumnModel cm = schema.get(i);
			columnIndexMap.put(cm.getId(), i);
			schemaMap.put(cm.getId(), cm);
		}
	}
	
	/**
	 * Get the version number shared by all rows within this change set.
	 * 
	 * @return
	 */
	public long getChangeSetVersion(){
		return versionNumber;
	}

	/**
	 * Iterate over all rows of this change set..
	 * 
	 * @return
	 */
	public Iterable<SparseRow> rowIterator() {
		return sparseRows;
	}

	/**
	 * Add a new empty row to this change set.
	 * 
	 * @return
	 */
	public SparseRow addEmptyRow() {
		SparseRow newRow = new SparseRowImpl(sparseRows.size());
		sparseRows.add(newRow);
		return newRow;
	}

	/**
	 * Group all rows by the columns they have values for.
	 * 
	 * @return
	 */
	public Iterable<Grouping> groupByValidValues() {
		// group rows by the columns with values
		Map<List<String>, List<SparseRow>> groupMap = new LinkedHashMap<>();
		for(SparseRow row: this.rowIterator()){
			List<String> columnIds = new LinkedList<>();
			for(ColumnModel cm: schema){
				if(row.hasCellValue(cm.getId())){
					columnIds.add(cm.getId());
				}
			}
			List<SparseRow> groupRows = groupMap.get(columnIds);
			if(groupRows == null){
				groupRows = new LinkedList<>();
				groupMap.put(columnIds, groupRows);
			}
			groupRows.add(row);
		}
		// Build the grouping
		List<Grouping> grouping = new LinkedList<>();
		for(List<String> columnIds: groupMap.keySet()){
			List<SparseRow> groupRows = groupMap.get(columnIds);
			List<ColumnModel> models = new LinkedList<>();
			for(String columnId: columnIds){
				ColumnModel cm = getColumnModel(columnId);
				models.add(cm);
			}
			Grouping group = new Grouping(models, groupRows);
			grouping.add(group);
		}
		return grouping;
	}

	/**
	 * Get the
	 * 
	 * @param columnId
	 * @return
	 */
	public ColumnModel getColumnModel(String columnId) {
		ValidateArgument.required(columnId, "columnId");
		ColumnModel cm = schemaMap.get(columnId);
		if (cm == null) {
			throw new NotFoundException("ColumnModel not found for column ID: "
					+ columnId);
		}
		return cm;
	}

	/**
	 * Get the index of the given column ID.
	 * 
	 * @param columnId
	 * @return
	 */
	public Integer getColumnIndex(String columnId) {
		ValidateArgument.required(columnId, "columnId");
		Integer index = columnIndexMap.get(columnId);
		if (index == null) {
			throw new NotFoundException("ColumnModel not found for column ID: "
					+ columnId);
		}
		return index;
	}

	/**
	 * Private implementation of SparseRow. Note this class is not static, and
	 * accesses members of the parent change set.
	 */
	private class SparseRowImpl implements SparseRow {

		int rowIndex;
		Long rowId;
		Map<String, String> valueMap = new HashMap<String, String>();

		private SparseRowImpl(int rowIndex) {
			this.rowIndex = rowIndex;
		}

		@Override
		public Long getRowId() {
			return rowId;
		}

		@Override
		public void setRowId(Long rowId) {
			this.rowId = rowId;
		}

		@Override
		public Long getChangeSetVersion() {
			return versionNumber;
		}

		@Override
		public boolean hasCellValue(String columnId) {
			ValidateArgument.required(columnId, "columnId");
			return valueMap.containsKey(columnId);
		}

		@Override
		public String getCellValue(String columnId) throws NotFoundException {
			if (!hasCellValue(columnId)) {
				throw new NotFoundException(
						"Cell value does not exist for column Id: " + columnId);
			}
			return valueMap.get(columnId);
		}

		@Override
		public void setCellValue(String columnId, String value) {
			ValidateArgument.required(columnId, "columnId");
			ColumnModel cm = getColumnModel(columnId);
			Integer columnIndex = getColumnIndex(columnId);
			// process and validate the value.
			value = TableModelUtils.validateRowValue(value, cm, rowIndex,
					columnIndex);
			valueMap.put(columnId, value);
		}

		@Override
		public void removeValue(String columnId) {
			ValidateArgument.required(columnId, "columnId");
			valueMap.remove(columnId);
		}

		@Override
		public int getRowIndex() {
			return rowIndex;
		}

		@Override
		public boolean isDelete() {
			// this is a delete if there are no values
			return valueMap.keySet().isEmpty();
		}
		
	}

}
