package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
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
	
    @Test
    public void testStream() {

    	LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(20L).setSequenceNumber(200L);
    	
        List<RowView> rows = List.of(
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 1))
        			.setCells(Arrays.asList(
							new ConValue(ConType.STRING, "1"),
							new ConValue(ConType.STRING, "more1"),
							new ConValue(ConType.LONG, 1L)
					))
			)),
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 2))
        			.setCells(Arrays.asList(
							new ConValue(ConType.STRING, "2"),
							new ConValue(ConType.STRING, "more2"),
							new ConValue(ConType.LONG, 2L)
					))
			)),
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 3))
        			.setCells(Arrays.asList(
							new ConValue(ConType.STRING, "3"),
							new ConValue(ConType.STRING, "more3"),
							new ConValue(ConType.LONG, 3L)
					))
			))			
		);
        
        ColumnMapping[] mapping = new ColumnMapping[] {
            new ColumnMapping("a", ColumnType.STRING, 0, 0, true),
            new ColumnMapping("b", ColumnType.INTEGER, 1, 2, true),
            new ColumnMapping("c", ColumnType.STRING, 2, 1, false)
        };

        GridDataStream stream = new GridDataStream(rows.iterator(), mapping);
        
        long rowId = 1L;
        
        while (stream.hasNext()) {
        	Object[] row = stream.next();
        	assertEquals(String.valueOf(rowId), row[0]);					// a
        	assertEquals(rowId, row[1]);									// b
        	assertEquals("[20," + (200 + rowId) + "]", row[2].toString());	// c (vectorId)
			rowId++;
		}
    }
}