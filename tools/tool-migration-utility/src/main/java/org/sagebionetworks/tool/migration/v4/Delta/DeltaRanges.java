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
	private List<IdRange> ranges;
	
	public DeltaRanges(MigrationType type, List<IdRange> idRanges) {
		this.migrationType = type;
		this.ranges = idRanges;
	}

}
