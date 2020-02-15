package org.sagebionetworks.table.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A sparsely populated matrix of data representing a single table change set.
 *
 */
public class SparseChangeSet implements TableChange {

	String tableId;
	String etag;
	List<ColumnModel> schema;
	Map<String, ColumnModel> schemaMap;
	Map<String, Integer> columnIndexMap;
	List<SparseRow> sparseRows;

	/**
	 * Create a new empty change set.
	 * 
	 * @param schema
	 * @param versionNumber
	 */
	public SparseChangeSet(String tableId, List<ColumnModel> schema) {
		this(tableId, schema, null);
	}
	
	public SparseChangeSet(String tableId, List<ColumnModel> schema, String etag) {
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(schema, "schema");
		initialize(tableId, schema, etag);
	}
	
	/**
	 * Create a new SparseChangeSet from a Data Transfer Object (DTO).
	 * 
	 * @param dto
	 * @param columnProvider
	 */
	public SparseChangeSet(SparseChangeSetDto dto, List<ColumnModel> schema){
		ValidateArgument.required(dto, "dto");
		ValidateArgument.required(dto.getTableId(), "dto.tableId");
		ValidateArgument.required(dto.getColumnIds(), "dto.columnIds");
		ValidateArgument.required(dto.getRows(), "dto.rows");
		ValidateArgument.required(schema, "schema");
		initialize(dto.getTableId(), schema, dto.getEtag());
		// Add all of the rows from the DTO.
		addAllRows(dto.getRows());
	}
	
	/**
	 * Create a SparseChangeSet.
	 * @param tableId
	 * @param schema
	 * @param rows
	 */
	public SparseChangeSet(String tableId, List<ColumnModel> schema, List<SparseRowDto> rows, String etag){
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(schema, "schema");
		initialize(tableId, schema, etag);
		addAllRows(rows);
	}

	/**
	 * Add all of the given rows to this change set.
	 * @param rows
	 */
	public void addAllRows(Iterable<SparseRowDto> rows) {
		// Add all of the rows from the DTO.
		for(SparseRowDto row: rows){
			SparseRow sparse = this.addEmptyRow();
			sparse.setRowId(row.getRowId());
			sparse.setVersionNumber(row.getVersionNumber());
			sparse.setRowEtag(row.getEtag());
			if(row.getValues() != null){
				for(ColumnModel cm: this.schema){
					if(row.getValues().containsKey(cm.getId())){
						sparse.setCellValue(cm.getId(), row.getValues().get(cm.getId()));
					}
				}
			}
		}
	}
	
	/**
	 * Common initialization.
	 * @param tableId
	 * @param schema
	 * @param versionNumber
	 */
	private void initialize(String tableId, List<ColumnModel> schema, String etag) {
		this.tableId = tableId;
		this.sparseRows = new LinkedList<SparseRow>();
		this.schema = new LinkedList<>(schema);
		this.etag = etag;
		schemaMap = new HashMap<String, ColumnModel>(schema.size());
		columnIndexMap = new HashMap<String, Integer>(schema.size());
		for (int i = 0; i < schema.size(); i++) {
			ColumnModel cm = schema.get(i);
			columnIndexMap.put(cm.getId(), i);
			schemaMap.put(cm.getId(), cm);
		}
	}
	
	/**
	 * Write all of the data from this change set into a Data Transfer Object
	 * @return
	 */
	public SparseChangeSetDto writeToDto(){
		SparseChangeSetDto dto = new SparseChangeSetDto();
		dto.setTableId(this.tableId);
		dto.setEtag(this.etag);
		// Write the column models ids
		List<String> columnIds = new LinkedList<String>();
		for(ColumnModel cm: this.schema){
			columnIds.add(cm.getId());
		}
		dto.setColumnIds(columnIds);
		List<SparseRowDto> rows = new LinkedList<SparseRowDto>();
		for(SparseRow row: this.sparseRows){
			SparseRowDto partial = new SparseRowDto();
			partial.setRowId(row.getRowId());
			partial.setVersionNumber(row.getVersionNumber());
			partial.setEtag(row.getRowEtag());
			HashMap<String, String> values = new HashMap<String, String>(this.schema.size());
			for(ColumnModel cm: this.schema){
				if(row.hasCellValue(cm.getId())){
					values.put(cm.getId(), row.getCellValue(cm.getId()));
				}
			}
			partial.setValues(values);
			rows.add(partial);
		}
		dto.setRows(rows);
		return dto;
	}
	
	/**
	 * Get the number of rows currently in this set.
	 * 
	 * @return
	 */
	public int getRowCount() {
		return this.sparseRows.size();
	}
	
	/**
	 * The etag identifies the version of a change set.
	 * @return
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * The etag identifies the version of a change set.
	 */
	public void setEtag(String etag) {
		this.etag = etag;
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
			Grouping group = new Grouping(models, groupRows);
			grouping.add(group);
		}
		return grouping;
	}

	public List<ListColumnRowChanges> groupListColumnChanges() {
		Map<String, Set<Long>> groupMap = new HashMap<>();

		// We only care about list columns in the schema
		List<String> listColumnsIdsOnly = schema.stream()
				.filter((ColumnModel cm) -> ColumnTypeListMappings.isList(cm.getColumnType()))
				.map(ColumnModel::getId)
				.collect(Collectors.toList());

		// No list columns in schema so no work to be done
		if(listColumnsIdsOnly.isEmpty()){
			return Collections.emptyList();
		}

		// check if each row has changed values for any of the list columns
		// and if so, add the rowId to set of changed rows for that column
		for (SparseRow row : this.rowIterator()) {
			for(String listColumnModelId: listColumnsIdsOnly){
				if(row.hasCellValue(listColumnModelId)){
					groupMap.computeIfAbsent(listColumnModelId, (String cmId) -> new HashSet<>())
					.add(row.getRowId());
				}
			}
		}

		// build result list based on map
		return groupMap.entrySet().stream()
				.map((Map.Entry<String, Set<Long>> entry) ->
						new ListColumnRowChanges(getColumnModel(entry.getKey()), entry.getValue())
				)
				.collect(Collectors.toList());
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
		Long versionNumber;
		String etag;
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
		public void setVersionNumber(Long rowVersionNumber){
			this.versionNumber = rowVersionNumber;
		}
		@Override
		public Long getVersionNumber() {
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
		
		@Override
		public void setRowEtag(String etag) {
			this.etag = etag;
		}

		@Override
		public String getRowEtag() {
			return etag;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((etag == null) ? 0 : etag.hashCode());
			result = prime * result + ((rowId == null) ? 0 : rowId.hashCode());
			result = prime * result + rowIndex;
			result = prime * result
					+ ((valueMap == null) ? 0 : valueMap.hashCode());
			result = prime * result
					+ ((versionNumber == null) ? 0 : versionNumber.hashCode());
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
			SparseRowImpl other = (SparseRowImpl) obj;
			if (etag == null) {
				if (other.etag != null)
					return false;
			} else if (!etag.equals(other.etag))
				return false;
			if (rowId == null) {
				if (other.rowId != null)
					return false;
			} else if (!rowId.equals(other.rowId))
				return false;
			if (rowIndex != other.rowIndex)
				return false;
			if (valueMap == null) {
				if (other.valueMap != null)
					return false;
			} else if (!valueMap.equals(other.valueMap))
				return false;
			if (versionNumber == null) {
				if (other.versionNumber != null)
					return false;
			} else if (!versionNumber.equals(other.versionNumber))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "SparseRowImpl [rowIndex=" + rowIndex + ", rowId=" + rowId
					+ ", rowVersionNumber=" + versionNumber + ", valueMap="
					+ valueMap + "]";
		}

	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		result = prime * result
				+ ((sparseRows == null) ? 0 : sparseRows.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
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
		SparseChangeSet other = (SparseChangeSet) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (sparseRows == null) {
			if (other.sparseRows != null)
				return false;
		} else if (!sparseRows.equals(other.sparseRows))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SparseChangeSet [tableId=" + tableId + ", etag=" + etag
				+ ", schema=" + schema + ", sparseRows=" + sparseRows + "]";
	}

}
