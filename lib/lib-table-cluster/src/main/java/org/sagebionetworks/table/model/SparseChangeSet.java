package org.sagebionetworks.table.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A sparsely populated matrix of data representing a single table change set.
 *
 */
public class SparseChangeSet {

	String tableId;
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
	public SparseChangeSet(String tableId, List<ColumnModel> schema, long versionNumber) {
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(schema, "schema");
		this.tableId = tableId;
		this.sparseRows = new LinkedList<SparseRow>();
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
	 * Get the schema of this change set.
	 * 
	 * @return
	 */
	public List<ColumnModel> getSchema(){
		// return a copy.
		return new LinkedList<>(schema);
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
	 * The ID of the table.
	 * @return
	 */
	public String getTableId(){
		return tableId;
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
			Grouping group = new Grouping(models, groupRows, tableId);
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
	 * Get all of the FileHandle ID within this change set.
	 * @return
	 */
	public Set<Long> getFileHandleIdsInSparseChangeSet(){
		// determine which columns are files
		List<String> fileColumnIds = new LinkedList<>();
		for(ColumnModel cm: schema){
			if(ColumnType.FILEHANDLEID.equals(cm.getColumnType())){
				fileColumnIds.add(cm.getId());
			}
		}
		Set<Long> results = new HashSet<>();
		for(SparseRow row: rowIterator()){
			for(String fileColumnId: fileColumnIds){
				if(row.hasCellValue(fileColumnId)){
					String cellValue = row.getCellValue(fileColumnId);
					if(!TableModelUtils.isNullOrEmpty(cellValue)){
						try {
							results.add(Long.parseLong(cellValue));
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("Passed a non-integer file handle id: "+cellValue);
						}	
					}
				}
			}
		}
		return results;
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
