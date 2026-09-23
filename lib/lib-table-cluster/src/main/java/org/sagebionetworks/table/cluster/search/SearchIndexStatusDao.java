package org.sagebionetworks.table.cluster.search;

import java.util.Optional;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;

public interface SearchIndexStatusDao {

	void setDataSource(DataSource dataSource);

	void createTableIfDoesNotExist();

	void createOrUpdate(SearchIndexStatus status);

	Optional<SearchIndexState> getState(Long searchIndexId);

	Optional<SearchIndexStatus> getStatus(Long searchIndexId);

	boolean exists(Long searchIndexId);

	void delete(Long searchIndexId);

	void truncateAll();
}
