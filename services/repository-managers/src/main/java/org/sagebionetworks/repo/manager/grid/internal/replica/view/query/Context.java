package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.model.grid.CrdtId;
import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;

public class Context {
	
	private static Map<String, Integer> buildColumnNamesToIndexMap(GridHeader header) {
		if (header.getOrderedColumns() == null || header.getOrderedColumns().isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, Integer> map = new HashMap<>(header.getOrderedColumns().size());
		
		for (int i = 0; i < header.getOrderedColumns().size(); i++) {
			map.put(header.getOrderedColumns().get(i).getName(), i);
		}
		
		return map;
	}
	
	private static List<Integer> buildSelectedColumnIndices(GridHeader header) {
		if (header.getOrderedColumns() == null || header.getOrderedColumns().isEmpty()) {
			return Collections.emptyList();
		}

		ReplicaSelectionModel selectionModel = header.getReplicaSelectionModel();
		
		if (selectionModel == null || (
			!Boolean.TRUE.equals(selectionModel.getColumnSelectAll()) 
			&& 
			(selectionModel.getColumnSelection() == null || selectionModel.getColumnSelection().isEmpty()))
		) {
			return Collections.emptyList();
		}
		
		// All the columns are selected, just returns the indices for all the ordered columns
		if (Boolean.TRUE.equals(selectionModel.getColumnSelectAll())) {
			return IntStream.range(0, header.getOrderedColumns().size()).boxed().collect(Collectors.toList());
		}
		
		// We first map the ordered columns ids to their respective index
		Map<CrdtId, Integer> colIdToIndexMap = new HashMap<>(header.getOrderedColumns().size());
		
		for (int i = 0; i < header.getOrderedColumns().size(); i++) {
			colIdToIndexMap.put(header.getOrderedColumns().get(i).getColumnOrderNodeId(), i);
		}
		
		return selectionModel.getColumnSelection().stream().map(colIdToIndexMap::get).collect(Collectors.toList());
		
	}

	private final GridHeader header;
	private final Map<String, Integer> columnNameToIndexMap;
	private List<Integer> selectedColumnIndices;

	public Context(GridHeader header) {
		this.header = header;
		this.columnNameToIndexMap = buildColumnNamesToIndexMap(header);
		this.selectedColumnIndices = buildSelectedColumnIndices(header);
	}

	public GridHeader getHeader() {
		return header;
	}

	/**
	 * 
	 * @param columnName
	 * @return The zero-based index in the ordered columns list for the given column name.
	 * @throws IllegalArgumentException if the column name is not found.
	 */
	public Integer getColumnIndexForName(String columnName) {
		Integer index = columnNameToIndexMap.get(columnName);
		if (index == null) {
			throw new IllegalArgumentException("Column name not found: " + columnName);
		}
		return index;
	}
	
	/**
	 * @return The list of zero-based indices in the ordered columns list that match the columns that are currently selected by the user.
	 */
	public List<Integer> getSelectedColumnIndices() {
		return selectedColumnIndices;
	}
	
}
