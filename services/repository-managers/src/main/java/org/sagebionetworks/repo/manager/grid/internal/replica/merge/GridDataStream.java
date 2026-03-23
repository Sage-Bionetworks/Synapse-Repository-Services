package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

/**
 * A {@link DataStream} that streams rows from a grid replica view for import
 * into a temporary table. Each row is projected into a flat {@code Object[]}
 * where the upsert-key columns occupy the leading positions (in the order they
 * appear in the {@link ColumnMapping} array) and the row's vector-clock ID is
 * appended as the final element.
 *
 * <p>Rows whose upsert-key cells carry a {@link org.sagebionetworks.repo.model.grid.patch.ConType#NULL},
 * {@link org.sagebionetworks.repo.model.grid.patch.ConType#UNDEFINED}, or
 * {@code null} {@link org.sagebionetworks.repo.model.grid.patch.ConValue} are
 * silently skipped, because such values cannot serve as a meaningful upsert key. Skipping these rows
 * effectively ignores them in the upsert step, so these rows will remain unchanged in the grid while
 * other rows may be added or updated.
 */
public class GridDataStream implements DataStream {

	private final Iterator<RowView> rowViewIterator;
	private final ColumnMapping[] upsertKey;
	// Look-ahead buffer: holds the next row to be returned, or null when exhausted
	private Object[] nextRow;

	public GridDataStream(Iterator<RowView> rowViewIterator, ColumnMapping[] columnMapping) {
		this.rowViewIterator = rowViewIterator;
		this.upsertKey = Arrays.stream(columnMapping).filter(ColumnMapping::isUpsertColumn).toArray(ColumnMapping[]::new);
		advance();
	}

	@Override
	public boolean hasNext() {
		return nextRow != null;
	}

	@Override
	public Object[] next() {
		Object[] current = nextRow;
		advance();
		return current;
	}

	/**
	 * Advances the iterator to the next row that does not have a NULL or UNDEFINED
	 * ConValue in any of its upsert-key cells. Rows with a NULL or UNDEFINED upsert
	 * key value are silently skipped. Sets {@code nextRow} to {@code null} when the
	 * underlying iterator is exhausted.
	 */
	private void advance() {
		nextRow = null;
		while (rowViewIterator.hasNext()) {
			RowView row = rowViewIterator.next();

			List<ConValue> cellValues = row.getRowObject().getData().getCells();

			if (hasNullOrUndefinedUpsertKey(cellValues)) {
				// skip this row – its upsert key is not usable
				continue;
			}

			// The id of the vector that holds the row data
			LogicalTimestamp rowVecId = row.getRowObject().getData().getVectorId();

			Object[] values = new Object[upsertKey.length + 1];

			// First we map the upsert key columns
			for (int i = 0; i < upsertKey.length; i++) {
				values[i] = cellValues.get(upsertKey[i].getGridIndex()).getValue();
			}

			values[upsertKey.length] = LogicalTimestampCompactSerializable.serialize(rowVecId);

			nextRow = values;
			return;
		}
	}

	/**
	 * Returns {@code true} if any upsert-key cell in the given cell list has a
	 * {@link ConType#NULL} or {@link ConType#UNDEFINED} type.
	 */
	private boolean hasNullOrUndefinedUpsertKey(List<ConValue> cellValues) {
		for (ColumnMapping key : upsertKey) {
			ConValue cell = cellValues.get(key.getGridIndex());
			if (cell == null) {
				return true;
			}
			ConType type = cell.getType();
			if (ConType.NULL.equals(type) || ConType.UNDEFINED.equals(type)) {
				return true;
			}
		}
		return false;
	}

}
