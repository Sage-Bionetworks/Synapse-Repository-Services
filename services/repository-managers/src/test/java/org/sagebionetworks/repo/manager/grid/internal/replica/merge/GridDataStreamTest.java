package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.junit.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.ColumnType;

public class GridDataStreamTest {
	
    @Test
    public void testStream() {

    	LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(20L).setSequenceNumber(200L);
    	
        List<RowView> rows = List.of(
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 1))
        			.setCells(new JSONArray(Arrays.asList("1", "more1", 1)))
			)),
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 2))
        			.setCells(new JSONArray(Arrays.asList("2", "more2", 2)))
			)),
			new RowView().setRowObject(new RowObject().setData(
        		new RowData().setVectorId(LogicalTimestamp.newIncrement(vectorId, 3))
        			.setCells(new JSONArray(Arrays.asList("3", "more3", 3)))
			))			
		);
        
        ColumnMapping[] mapping = new ColumnMapping[] {
            new ColumnMapping("a", ColumnType.STRING, 0, 0, true),
            new ColumnMapping("b", ColumnType.INTEGER, 1, 2, true),
            new ColumnMapping("c", ColumnType.STRING, 2, 1, false)
        };

        GridDataStream stream = new GridDataStream(rows.iterator(), mapping);
        
        int rowId = 1;
        
        while (stream.hasNext()) {
        	Object[] row = stream.next();
        	assertEquals(String.valueOf(rowId), row[0]);					// a
        	assertEquals(rowId, row[1]);									// b	
        	assertEquals("[20," + (200 + rowId) + "]", row[2].toString());	// c (vectorId)
			rowId++;
		}
    }
}