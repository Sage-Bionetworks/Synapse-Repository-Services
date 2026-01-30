package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

public class GridDataStream implements DataStream {

	private Iterator<RowView> rowViewIterator;
	private ColumnMapping[] upsertKey;

	public GridDataStream(Iterator<RowView> rowViewIterator, ColumnMapping[] columnMapping) {
		this.rowViewIterator = rowViewIterator;
		this.upsertKey = Arrays.stream(columnMapping).filter(ColumnMapping::isUpsertColumn).toArray(ColumnMapping[]::new);
	}

	@Override
	public boolean hasNext() {
		return rowViewIterator.hasNext();
	}

	@Override
	public Object[] next() {
		RowView row = rowViewIterator.next();
		
		// The id of the vector that holds the row data
		LogicalTimestamp rowVecId = row.getRowObject().getData().getVectorId();

		// The actual JSON array of cell values
		List<ConValue> cellValues = row.getRowObject().getData().getCells();

		Object[] values = new Object[upsertKey.length + 1];

		// First we map the upsert key columns
		for (int i = 0; i < upsertKey.length; i++) {
			values[i] = cellValues.get(upsertKey[i].getGridIndex()).getValue();
		}

		values[upsertKey.length] = LogicalTimestampCompactSerializable.serialize(rowVecId);

		return values;
	}

}
