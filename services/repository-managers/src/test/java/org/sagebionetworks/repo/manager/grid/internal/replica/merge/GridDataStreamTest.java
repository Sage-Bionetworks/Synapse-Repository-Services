package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.ColumnType;

public class GridDataStreamTest {

	private static final ColumnMapping[] MAPPING = new ColumnMapping[] {
		new ColumnMapping("a", ColumnType.STRING, 0, 0, true),
		new ColumnMapping("b", ColumnType.INTEGER, 1, 2, true),
		new ColumnMapping("c", ColumnType.STRING, 2, 1, false)
	};

	/** Builds a RowView with the given vectorId increment and three cells at grid indices 0, 1, 2. */
	private static RowView buildRow(LogicalTimestamp base, long vecIncrement,
			ConValue cell0, ConValue cell1, ConValue cell2, long n1, long n2, long n3) {
		LogicalTimestamp vId = new LogicalTimestamp().setReplicaId(base.getReplicaId())
				.setSequenceNumber(base.getSequenceNumber());
		return new RowView().setRowObject(new RowObject().setData(
			new RowData().setVectorId(LogicalTimestamp.newIncrement(vId, vecIncrement))
				.setNodes(new ConstantNode[] {
					new ConstantNode().setId(LogicalTimestamp.newIncrement(vId, n1)).setValue(cell0),
					new ConstantNode().setId(LogicalTimestamp.newIncrement(vId, n2)).setValue(cell1),
					new ConstantNode().setId(LogicalTimestamp.newIncrement(vId, n3)).setValue(cell2)
				})
		));
	}

    @Test
    public void testStream() {

    	LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(20L).setSequenceNumber(200L);
    	
        List<RowView> rows = List.of(
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 1))
					.setNodes(new ConstantNode[] {
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 4)).setValue(new ConValue(ConType.STRING, "1")),
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 5)).setValue(new ConValue(ConType.STRING, "more1")),
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 6)).setValue(new ConValue(ConType.LONG, 1L))
					})
			)),
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 2))
        			.setNodes(new ConstantNode[] {
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 7)).setValue(new ConValue(ConType.STRING, "2")),
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 8)).setValue(new ConValue(ConType.STRING, "more2")),
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 9)).setValue(new ConValue(ConType.LONG, 2L))
					})
			)),
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 3))
        			.setNodes(new ConstantNode[] {
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 10)).setValue(new ConValue(ConType.STRING, "3")),
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 11)).setValue(new ConValue(ConType.STRING, "more3")),
							new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 12)).setValue(new ConValue(ConType.LONG, 3L))
					})
			))			
		);

        GridDataStream stream = new GridDataStream(rows.iterator(), MAPPING);

        long rowId = 1L;
        
        while (stream.hasNext()) {
        	Object[] row = stream.next();
        	assertEquals(String.valueOf(rowId), row[0]);					// a
        	assertEquals(rowId, row[1]);									// b
        	assertEquals("[20," + (200 + rowId) + "]", row[2].toString());	// c (vectorId)
			rowId++;
		}
        assertEquals(4L, rowId); // all 3 rows were returned
    }

	@Test
	public void testStreamSkipsNullAndUndefinedUpsertKeys() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(20L).setSequenceNumber(200L);

		// Row 1 – upsert key "a" is ConValue.NULL      → skipped
		// Row 2 – upsert key "b" is ConValue.UNDEFINED  → skipped
		// Row 3 – valid                         → returned
		// Row 4 – valid                         → returned
		// Row 5 – upsert key "a" is null      → skipped
		// Row 6 – upsert key "a" has no node at all (absent from the cell map) → skipped
		List<RowView> rows = new ArrayList<>();
		rows.add(buildRow(vectorId, 1,
			new ConValue(ConType.NULL, null),      new ConValue(ConType.STRING, "x"), new ConValue(ConType.LONG, 0L),
			2, 3, 4));
		rows.add(buildRow(vectorId, 5,
			new ConValue(ConType.STRING, "skip2"), new ConValue(ConType.STRING, "x"), new ConValue(ConType.UNDEFINED, null),
			6, 7, 8));
		rows.add(buildRow(vectorId, 9,
			new ConValue(ConType.STRING, "keep1"), new ConValue(ConType.STRING, "x"), new ConValue(ConType.LONG, 1L),
			10, 11, 12));
		rows.add(buildRow(vectorId, 13,
			new ConValue(ConType.STRING, "keep2"), new ConValue(ConType.STRING, "x"), new ConValue(ConType.LONG, 2L),
			14, 15, 16));
		rows.add(buildRow(vectorId, 17,
			null,      new ConValue(ConType.STRING, "x"), new ConValue(ConType.LONG, 3L),
			18, 19, 20));
		rows.add(new RowView().setRowObject(new RowObject().setData(
			new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 21))
				.setNodes(new ConstantNode[] {
					null,
					new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 22)).setValue(new ConValue(ConType.STRING, "x")),
					new ConstantNode().setId(LogicalTimestamp.newIncrement(vectorId, 23)).setValue(new ConValue(ConType.LONG, 4L))
				})
		)));

		GridDataStream stream = new GridDataStream(rows.iterator(), MAPPING);

		List<Object[]> result = new ArrayList<>();
		while (stream.hasNext()) {
			result.add(stream.next());
		}

		assertEquals(2, result.size());
		assertEquals("keep1", result.get(0)[0]);
		assertEquals("keep2", result.get(1)[0]);
	}
}