package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;

public interface SynonymSetService {

	SynonymSet create(Long userId, SynonymSet request);

	SynonymSet get(Long userId, String id);

	SynonymSet update(Long userId, SynonymSet request);

	void delete(Long userId, String id);

	ListSynonymSetsResponse list(Long userId, ListSynonymSetsRequest request);
}
