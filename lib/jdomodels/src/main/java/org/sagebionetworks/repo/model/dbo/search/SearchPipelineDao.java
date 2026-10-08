package org.sagebionetworks.repo.model.dbo.search;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;

public interface SearchPipelineDao {
	NamedSearchPipeline create(Long createdBy, NamedSearchPipeline pipeline);
	Optional<NamedSearchPipeline> get(String id);

	/**
	 * Update the description and settings of an existing pipeline. The organization and name are immutable and are
	 * not written.
	 */
	NamedSearchPipeline update(Long modifiedBy, NamedSearchPipeline pipeline);
	List<NamedSearchPipeline> list(String organizationName, long limit, long offset);
	List<NamedSearchPipeline> listAll(long limit, long offset);
	Optional<NamedSearchPipeline> getByOrganizationAndName(String organizationName, String name);

	void truncateAll();
}
