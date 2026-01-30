package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.model.grid.CrdtId;
import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;

public class ContextTest {

	private GridHeader header;
	private List<Column> columns;

	@BeforeEach
	public void before() {
		columns = List.of(
			new Column().setName("a").setColumnOrderNodeId(new CrdtId().setRep(1L).setSeq(1L)),
			new Column().setName("b").setColumnOrderNodeId(new CrdtId().setRep(1L).setSeq(2L)),
			new Column().setName("c").setColumnOrderNodeId(new CrdtId().setRep(1L).setSeq(3L))
		);
		header = new GridHeader().setOrderedColumns(columns);
	}

	@Test
	public void testGetHeader() {
		Context context = new Context(header);
		
		// call under test
		GridHeader result = context.getHeader();
		
		assertEquals(header, result);
	}

	@Test
	public void testGetColumnIndexForName() {
		Context context = new Context(header);
		
		for (int i = 0; i < columns.size(); i++) {
			// call under test
			assertEquals(i, context.getColumnIndexForName(columns.get(i).getName()));
		}
	}

	@Test
	public void testGetColumnIndexForNameNotFound() {
		Context context = new Context(header);
		
		String columnName = "invalid";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			context.getColumnIndexForName(columnName);
		}).getMessage();
		
		assertEquals("Column name not found: invalid", message);
	}

	@Test
	public void testConstructorWithEmptyColumns() {
		header.setOrderedColumns(Collections.emptyList());
		
		// call under test
		Context context = new Context(header);
		
		assertEquals(header, context.getHeader());
		
		// Should throw exception when trying to get any column
		assertThrows(IllegalArgumentException.class, () -> {
			context.getColumnIndexForName("anyColumn");
		});
	}

	@Test
	public void testConstructorWithNullColumns() {
		header.setOrderedColumns(null);
		
		// call under test
		Context context = new Context(header);
		
		assertEquals(header, context.getHeader());
		
		// Should throw exception when trying to get any column
		assertThrows(IllegalArgumentException.class, () -> {
			context.getColumnIndexForName("anyColumn");
		});
	}

	@Test
	public void testGetSelectedColumnIndicesWithNullSelectionModel() {
		// No selection model set
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithEmptySelectionModel() {
		// Empty selection model
		header.setReplicaSelectionModel(new ReplicaSelectionModel());
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithColumnSelectAllTrue() {
		// columnSelectAll = true should return all column indices
		ReplicaSelectionModel selectionModel = new ReplicaSelectionModel()
			.setColumnSelectAll(true);
		header.setReplicaSelectionModel(selectionModel);
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(List.of(0, 1, 2), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithColumnSelectAllFalse() {
		// columnSelectAll = false without column selection should return empty list
		ReplicaSelectionModel selectionModel = new ReplicaSelectionModel()
			.setColumnSelectAll(false);
		header.setReplicaSelectionModel(selectionModel);
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithSpecificColumnSelection() {
		// Select specific columns by their CrdtId
		CrdtId col1Id = new CrdtId().setRep(1L).setSeq(1L);
		CrdtId col3Id = new CrdtId().setRep(1L).setSeq(3L);
		
		ReplicaSelectionModel selectionModel = new ReplicaSelectionModel()
			.setColumnSelection(List.of(col1Id, col3Id));
		header.setReplicaSelectionModel(selectionModel);
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		// Should return indices for columns a (0) and c (2)
		assertEquals(List.of(0, 2), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithSingleColumnSelection() {
		// Select only one column
		CrdtId col2Id = new CrdtId().setRep(1L).setSeq(2L);
		
		ReplicaSelectionModel selectionModel = new ReplicaSelectionModel()
			.setColumnSelection(List.of(col2Id));
		header.setReplicaSelectionModel(selectionModel);
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		// Should return index for column b (1)
		assertEquals(List.of(1), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithEmptyColumnSelection() {
		// Empty column selection list
		ReplicaSelectionModel selectionModel = new ReplicaSelectionModel()
			.setColumnSelection(Collections.emptyList());
		header.setReplicaSelectionModel(selectionModel);
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithNullOrderedColumns() {
		// Null ordered columns
		header.setOrderedColumns(null);
		
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetSelectedColumnIndicesWithEmptyOrderedColumns() {
		// Empty ordered columns
		header.setOrderedColumns(Collections.emptyList());
		
		Context context = new Context(header);
		
		// call under test
		List<Integer> result = context.getSelectedColumnIndices();
		
		assertEquals(Collections.emptyList(), result);
	}
}
