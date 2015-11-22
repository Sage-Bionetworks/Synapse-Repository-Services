package org.sagebionetworks.tool.migration.v4.Delta;

import java.util.List;

import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.tool.migration.v4.RangeMetadataIterator;

/**
 * Container for list of ranges where there is a difference between prod and staging
 *
 */
public class DeltaRanges {
	
	private MigrationType migrationType;
	private List<IdRange> updRanges;
	private IdRange insRange;
	private IdRange delRange;
	
	public DeltaRanges(MigrationType type, IdRange insRange, List<IdRange> idRanges, IdRange delRange) {
		this.migrationType = type;
		this.insRange = insRange;
		this.updRanges = updRanges;
		this.delRange = delRange;
	}

}
