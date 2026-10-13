package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.Arrays;
import java.util.Iterator;

import org.sagebionetworks.repo.manager.grid.internal.replica.merge.JoinedRow.GridRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * A {@link DataStream} that streams rows from a grid replica view for import
 * into a temporary table. Each row is projected into a flat {@code Object[]}
 * where the upsert-key columns occupy the leading positions (in the order they
 * appear in the {@link ColumnMapping} array), followed by the
 * {@link GridRow#writeValues(Object[], int) values} of the row's {@link GridRow}.
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
	private final boolean includeRowJson;
	// Look-ahead buffer: holds the next row to be returned, or null when exhausted
	private Object[] nextRow;

	/**
	 * @param includeRowJson whether to stream each row's JSON document, needed only when the imported rows are
	 *                       validated
	 */
	public GridDataStream(Iterator<RowView> rowViewIterator, ColumnMapping[] columnMapping, boolean includeRowJson) {
		this.rowViewIterator = rowViewIterator;
		this.upsertKey = Arrays.stream(columnMapping).filter(ColumnMapping::isUpsertColumn).toArray(ColumnMapping[]::new);
		this.includeRowJson = includeRowJson;
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

			RowData rowData = row.getRowObject().getData();

			if (hasNullOrUndefinedUpsertKey(rowData)) {
				// skip this row – its upsert key is not usable
				continue;
			}

			Object[] values = new Object[upsertKey.length + GridRow.VALUE_COUNT];

			// First we map the upsert key columns
			for (int i = 0; i < upsertKey.length; i++) {
				values[i] = rowData.getCell(upsertKey[i].getGridIndex()).getValue();
			}

			// Followed by what an update of the row needs to be applied, validated and have its validation state written
			GridRow.fromRowView(row, includeRowJson).writeValues(values, upsertKey.length);

			nextRow = values;
			return;
		}
	}

	/**
	 * Returns {@code true} if any upsert-key cell in the row's cells has a
	 * {@link ConType#NULL} or {@link ConType#UNDEFINED} type.
	 */
	private boolean hasNullOrUndefinedUpsertKey(RowData rowData) {
		for (ColumnMapping key : upsertKey) {
			ConValue cell = rowData.getCell(key.getGridIndex());
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
