package org.sagebionetworks.repo.model.dbo.search;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;

public interface ColumnAnalyzerOverrideDao {
	ColumnAnalyzerOverride create(Long createdBy, ColumnAnalyzerOverride override);
	Optional<ColumnAnalyzerOverride> get(String id);
	ColumnAnalyzerOverride update(Long modifiedBy, ColumnAnalyzerOverride override);
	void delete(String id);
	List<ColumnAnalyzerOverride> list(String organizationName, long limit, long offset);
	List<ColumnAnalyzerOverride> listAll(long limit, long offset);
	void truncateAll();
}
