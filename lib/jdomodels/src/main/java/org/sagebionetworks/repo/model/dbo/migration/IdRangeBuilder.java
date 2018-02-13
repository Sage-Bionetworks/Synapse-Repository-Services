package org.sagebionetworks.repo.model.dbo.migration;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.migration.IdRange;

/**
 * Calculates ranges of IDs that contain the target number of rows or less.
 * Ranges are determined by inspecting each primary ID and its associated
 * secondary cardinality. If a single primary ID has more secondary rows than
 * the target size, a range will be created that only includes the single
 * primary ID.
 *
 */
public class IdRangeBuilder {

	List<IdRange> ranges;
	long optimalRowsPerRange;
	long totalNumberRowsInRange;
	Long minimumId;
	Long maximumId;

	public IdRangeBuilder(long optimalRowsPerRange) {
		this.optimalRowsPerRange = optimalRowsPerRange;
		this.ranges = new LinkedList<>();
		this.totalNumberRowsInRange = 0;
		this.minimumId = null;
		this.maximumId = null;
	}

	/**
	 * Add a row with its total associated counts.
	 * 
	 * @param primaryRowId
	 *            The ID of the primary row.
	 * @param cardinality
	 *            Total number of rows associated with this row.
	 */
	public void addRow(long primaryRowId, long cardinality) {
		// Does this row fit in the current range?
		if (this.totalNumberRowsInRange + cardinality > this.optimalRowsPerRange) {
			// This row does not fit in the current range.
			if (this.totalNumberRowsInRange > 0) {
				// Add the previous range before dealing with the new row.
				IdRange range = new IdRange();
				range.setMinimumId(this.minimumId);
				range.setMaximumId(this.maximumId);
				ranges.add(range);
			}
			// Start a new range containing just this row.
			this.totalNumberRowsInRange = cardinality;
			this.minimumId = primaryRowId; // inclusive
			this.maximumId = primaryRowId + 1; // exclusive
		} else {
			// This row fits in the current range.
			if (this.minimumId == null) {
				// start of a new range.
				this.minimumId = primaryRowId; // inclusive
			}
			this.totalNumberRowsInRange += cardinality;
			this.maximumId = primaryRowId + 1; // exclusive
		}
	}

	/**
	 * Collate all of the ranges observed.
	 * 
	 * @return IdRanges. Note: Minimum IDs are inclusive while maximum IDs are exclusive.
	 */
	public List<IdRange> collateResults() {
		if (this.totalNumberRowsInRange > 0) {
			// add the trailing range.
			IdRange range = new IdRange();
			range.setMinimumId(minimumId);
			range.setMaximumId(maximumId);
			ranges.add(range);
		}
		return ranges;
	}

}
