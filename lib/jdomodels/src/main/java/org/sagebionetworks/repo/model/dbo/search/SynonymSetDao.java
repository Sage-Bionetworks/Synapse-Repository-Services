package org.sagebionetworks.repo.model.dbo.search;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.search.table.SynonymSet;

public interface SynonymSetDao {
	SynonymSet create(Long createdBy, SynonymSet synonymSet);
	Optional<SynonymSet> get(String id);
	SynonymSet update(Long modifiedBy, SynonymSet synonymSet);
	void delete(String id);
	List<SynonymSet> list(String organizationName, long limit, long offset);
	List<SynonymSet> listAll(long limit, long offset);
	Optional<SynonymSet> getByOrganizationAndName(String organizationName, String name);
	void truncateAll();
}
