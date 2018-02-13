package org.sagebionetworks.repo.model.dbo.migration;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.migration.IdRange;

/**
 * Given a stream of rowIs and associated row counts, collate a set of row ID
 * ranges such that each range contains the requested number of rows or less.
 *
 */
public class RangeBuilder {

	List<IdRange> ranges;
	long rowsPerRange;
	long totalNumberRowsInRange;
	Long minimumId;
	Long maximumId;

	public RangeBuilder(long rowsPerRange) {
		this.rowsPerRange = rowsPerRange;
		this.ranges = new LinkedList<>();
		this.totalNumberRowsInRange = 0;
		this.minimumId = null;
		this.maximumId = null;
	}

	/**
	 * Add a row with its total associated counts.
	 * 
	 * @param rowId
	 *            The ID of the row.
	 * @param associatedCount
	 *            Total number of rows associated with this row.
	 */
	public void addRow(long rowId, long associatedCount) {
		// Does this row fit in the current range?
		if (this.totalNumberRowsInRange + associatedCount > this.rowsPerRange) {
			// This row does not fit in the current range.
			
			// if there is an existing range then added.
			if (this.totalNumberRowsInRange > 0) {
				IdRange range = new IdRange();
				range.setMinimumId(this.minimumId);
				range.setMaximumId(this.maximumId);
				ranges.add(range);
			}

//			if (associatedCount > this.rowsPerRange) {
//				// Row so large it must be in its own range.
//				IdRange range = new IdRange();
//				range.setMinimumId(rowId);
//				range.setMaximumId(rowId + 1);
//				ranges.add(range);
//				this.maximumId = null;
//				this.minimumId = null;
//				this.totalNumberRowsInRange = 0;
//			} else {
				this.totalNumberRowsInRange = associatedCount;
				this.minimumId = rowId;
				this.maximumId = rowId + 1;
//			}

		} else {
			// This row fits in the current range.
			if(this.minimumId == null) {
				// start of a new range.
				this.minimumId = rowId;
			}
			this.totalNumberRowsInRange += associatedCount;
			this.maximumId = rowId + 1;
		}
	}

	/**
	 * 
	 * @return
	 */
	public List<IdRange> collateResults() {
		if (this.totalNumberRowsInRange > 0) {
			IdRange range = new IdRange();
			range.setMinimumId(minimumId);
			range.setMaximumId(maximumId);
			ranges.add(range);
		}
		return ranges;
	}

}
