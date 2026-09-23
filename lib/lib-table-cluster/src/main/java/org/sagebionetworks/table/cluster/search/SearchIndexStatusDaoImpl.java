package org.sagebionetworks.table.cluster.search;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SearchIndexStatusDaoImpl implements SearchIndexStatusDao {

	private static final String DDL = SQLUtils.loadSQLFromClasspath("schema/SearchIndexStatus.sql");

	private JdbcTemplate template;

	@Override
	public void setDataSource(DataSource dataSource) {
		this.template = new JdbcTemplate(dataSource);
	}

	@Override
	public void createTableIfDoesNotExist() {
		template.update(DDL);
	}

	@Override
	public void createOrUpdate(SearchIndexStatus status) {
		ValidateArgument.required(status, "status");
		ValidateArgument.required(status.getSearchIndexId(), "status.searchIndexId");
		ValidateArgument.required(status.getState(), "status.state");
		template.update(
				"INSERT INTO SEARCH_INDEX_STATUS"
				+ "  (SEARCH_INDEX_ID, STATE, ERROR_MESSAGE, CHANGED_ON)"
				+ " VALUES"
				+ "  (?, ?, ?, NOW(3)) AS new"
				+ " ON DUPLICATE KEY UPDATE"
				+ "  STATE = new.STATE,"
				+ "  ERROR_MESSAGE = new.ERROR_MESSAGE,"
				+ "  CHANGED_ON = NOW(3)",
				KeyFactory.stringToKey(status.getSearchIndexId()),
				status.getState().name(),
				status.getErrorMessage());
	}

	@Override
	public Optional<SearchIndexState> getState(Long searchIndexId) {
		try {
			String stateStr = template.queryForObject(
					"SELECT STATE FROM SEARCH_INDEX_STATUS WHERE SEARCH_INDEX_ID = ?",
					String.class, searchIndexId);
			return Optional.of(SearchIndexState.valueOf(stateStr));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<SearchIndexStatus> getStatus(Long searchIndexId) {
		try {
			SearchIndexStatus status = template.queryForObject(
					"SELECT SEARCH_INDEX_ID, STATE, ERROR_MESSAGE, CHANGED_ON"
					+ " FROM SEARCH_INDEX_STATUS WHERE SEARCH_INDEX_ID = ?",
					(ResultSet rs, int rowNum) -> {
						SearchIndexStatus s = new SearchIndexStatus();
						s.setSearchIndexId(KeyFactory.keyToString(rs.getLong("SEARCH_INDEX_ID")));
						s.setState(SearchIndexState.valueOf(rs.getString("STATE")));
						s.setErrorMessage(rs.getString("ERROR_MESSAGE"));
						Timestamp changedOn = rs.getTimestamp("CHANGED_ON");
						if (changedOn != null) {
							s.setChangedOn(new Date(changedOn.getTime()));
						}
						return s;
					}, searchIndexId);
			return Optional.ofNullable(status);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public boolean exists(Long searchIndexId) {
		Long count = template.queryForObject(
				"SELECT COUNT(*) FROM SEARCH_INDEX_STATUS WHERE SEARCH_INDEX_ID = ?",
				Long.class, searchIndexId);
		return count > 0;
	}

	@Override
	public void delete(Long searchIndexId) {
		template.update("DELETE FROM SEARCH_INDEX_STATUS WHERE SEARCH_INDEX_ID = ?", searchIndexId);
	}

	@Override
	public void truncateAll() {
		template.update("DELETE FROM SEARCH_INDEX_STATUS WHERE SEARCH_INDEX_ID > -1");
	}
}
