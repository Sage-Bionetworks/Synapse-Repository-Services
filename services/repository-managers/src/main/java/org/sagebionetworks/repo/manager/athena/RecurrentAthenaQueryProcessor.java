package org.sagebionetworks.repo.manager.athena;

import java.util.List;

import org.sagebionetworks.repo.model.athena.RowMapper;

public interface RecurrentAthenaQueryProcessor<T> {

	String getQueryName();
	
	RowMapper<T> getRowMapper();
	
	void processQueryResultsPage(List<T> resultsPage);
	
}
