package org.sagebionetworks.table.cluster.search;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import javax.sql.DataSource;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
	public void saveSnapshot(Long searchIndexId, IndexAuthorizationSnapshot snapshot) {
		ValidateArgument.required(searchIndexId, "searchIndexId");
		ValidateArgument.required(snapshot, "snapshot");
		try {
			String json = EntityFactory.createJSONStringForEntity(snapshot);
			// The status row always exists by capture time (CREATING/ACTIVE was written earlier), so the
			// UPDATE branch fires and only the snapshot column changes; a first-build insert defends the
			// unexpected case without disturbing the lifecycle state.
			template.update(
					"INSERT INTO SEARCH_INDEX_STATUS"
					+ "  (SEARCH_INDEX_ID, STATE, CHANGED_ON, SNAPSHOT_JSON)"
					+ " VALUES"
					+ "  (?, 'CREATING', NOW(3), ?) AS new"
					+ " ON DUPLICATE KEY UPDATE"
					+ "  SNAPSHOT_JSON = new.SNAPSHOT_JSON",
					searchIndexId, json);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<IndexAuthorizationSnapshot> getSnapshot(Long searchIndexId) {
		ValidateArgument.required(searchIndexId, "searchIndexId");
		String json;
		try {
			json = template.queryForObject(
					"SELECT SNAPSHOT_JSON FROM SEARCH_INDEX_STATUS WHERE SEARCH_INDEX_ID = ?",
					String.class, searchIndexId);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
		if (json == null) {
			// The status row exists but no snapshot has been captured for this index.
			return Optional.empty();
		}
		try {
			return Optional.of(EntityFactory.createEntityFromJSONString(json, IndexAuthorizationSnapshot.class));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
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
