package org.sagebionetworks.repo.model.dbo.search;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SCOB_BIND_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SCOB_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SCOB_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SCOB_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SCOB_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SCOB_SEARCH_CONFIG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_COL_ANALYZER_OVERRIDES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_COL_SEMANTIC_ENRICHMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_DEFAULT_ANALYZER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_CONFIG_ORGANIZATION_NAME;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.search.table.ColumnSemanticEnrichmentEntry;
import org.sagebionetworks.repo.model.search.table.SearchConfigBinding;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SearchConfigurationDaoImpl implements SearchConfigurationDao {

	private static final String DEFAULT_ANALYZER_FIELD = "SearchConfiguration.defaultAnalyzer";
	private static final String OVERRIDES_FIELD = "SearchConfiguration.columnAnalyzerOverrides";
	private static final String SEMANTIC_ENRICHMENT_FIELD = "SearchConfiguration.columnSemanticEnrichment";

	private static final RowMapper<SearchConfiguration> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		SearchConfiguration config = new SearchConfiguration();
		config.setId(String.valueOf(rs.getLong(COL_SEARCH_CONFIG_ID)));
		config.setEtag(rs.getString(COL_SEARCH_CONFIG_ETAG));
		config.setOrganizationName(rs.getString(COL_SEARCH_CONFIG_ORGANIZATION_NAME));
		config.setName(rs.getString(COL_SEARCH_CONFIG_NAME));
		config.setDescription(rs.getString(COL_SEARCH_CONFIG_DESCRIPTION));
		config.setDefaultAnalyzer(OpaqueJsonColumnCodecUtil.deserialize(
				rs.getString(COL_SEARCH_CONFIG_DEFAULT_ANALYZER), DEFAULT_ANALYZER_FIELD));
		config.setColumnAnalyzerOverrides(OpaqueJsonColumnCodecUtil.deserializeList(
				rs.getString(COL_SEARCH_CONFIG_COL_ANALYZER_OVERRIDES), OVERRIDES_FIELD));
		config.setColumnSemanticEnrichment(OpaqueJsonColumnCodecUtil.deserializeList(
				rs.getString(COL_SEARCH_CONFIG_COL_SEMANTIC_ENRICHMENT),
				ColumnSemanticEnrichmentEntry.class, SEMANTIC_ENRICHMENT_FIELD));
		config.setCreatedBy(String.valueOf(rs.getLong(COL_SEARCH_CONFIG_CREATED_BY)));
		config.setCreatedOn(new Date(rs.getTimestamp(COL_SEARCH_CONFIG_CREATED_ON).getTime()));
		config.setModifiedBy(String.valueOf(rs.getLong(COL_SEARCH_CONFIG_MODIFIED_BY)));
		config.setModifiedOn(new Date(rs.getTimestamp(COL_SEARCH_CONFIG_MODIFIED_ON).getTime()));
		return config;
	};

	private static final RowMapper<SearchConfigBinding> BINDING_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		SearchConfigBinding binding = new SearchConfigBinding();
		binding.setBindId(String.valueOf(rs.getLong(COL_SCOB_BIND_ID)));
		binding.setSearchConfigurationId(String.valueOf(rs.getLong(COL_SCOB_SEARCH_CONFIG_ID)));
		binding.setObjectId(String.valueOf(rs.getLong(COL_SCOB_OBJECT_ID)));
		binding.setObjectType(rs.getString(COL_SCOB_OBJECT_TYPE));
		binding.setCreatedBy(String.valueOf(rs.getLong(COL_SCOB_CREATED_BY)));
		binding.setCreatedOn(new Date(rs.getTimestamp(COL_SCOB_CREATED_ON).getTime()));
		return binding;
	};

	private final JdbcTemplate jdbcTemplate;
	private final IdGenerator idGenerator;

	public SearchConfigurationDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	@WriteTransaction
	@Override
	public SearchConfiguration create(Long createdBy, SearchConfiguration config) {
		ValidateArgument.required(createdBy, "createdBy");
		ValidateArgument.required(config, "config");
		ValidateArgument.required(config.getOrganizationName(), "config.organizationName");
		ValidateArgument.required(config.getName(), "config.name");

		Long id = idGenerator.generateNewId(IdType.SEARCH_CONFIGURATION_ID);

		try {
			jdbcTemplate.update(
					"INSERT INTO SEARCH_CONFIGURATION (ID, ETAG, ORGANIZATION_NAME, NAME, DESCRIPTION,"
					+ " DEFAULT_ANALYZER, COLUMN_ANALYZER_OVERRIDES, COLUMN_SEMANTIC_ENRICHMENT,"
					+ " CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
					+ " VALUES (?, UUID(), ?, ?, ?, ?, ?, ?, ?, NOW(3), ?, NOW(3))",
					id,
					config.getOrganizationName(),
					config.getName(),
					config.getDescription(),
					OpaqueJsonColumnCodecUtil.serialize(config.getDefaultAnalyzer(), DEFAULT_ANALYZER_FIELD),
					OpaqueJsonColumnCodecUtil.serialize(config.getColumnAnalyzerOverrides(), OVERRIDES_FIELD),
					OpaqueJsonColumnCodecUtil.serialize(config.getColumnSemanticEnrichment(), SEMANTIC_ENRICHMENT_FIELD),
					createdBy,
					createdBy
			);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A search configuration with the same name already exists in this organization.", e);
		}

		return get(id.toString()).orElseThrow(() -> new IllegalStateException("Failed to create SearchConfiguration"));
	}

	@Override
	public Optional<SearchConfiguration> get(String id) {
		ValidateArgument.required(id, "id");
		try {
			SearchConfiguration result = jdbcTemplate.queryForObject(
					"SELECT * FROM SEARCH_CONFIGURATION WHERE ID = ?",
					ROW_MAPPER, Long.parseLong(id));
			return Optional.ofNullable(result);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public SearchConfiguration update(Long modifiedBy, SearchConfiguration config) {
		ValidateArgument.required(modifiedBy, "modifiedBy");
		ValidateArgument.required(config, "config");
		ValidateArgument.required(config.getId(), "config.id");

		Long id = Long.parseLong(config.getId());

		String currentEtag = getCurrentEtagForUpdate(id);
		if (!currentEtag.equals(config.getEtag())) {
			throw new ConflictingUpdateException("SearchConfiguration was updated since last fetched. Please re-fetch and try again.");
		}

		int updated;
		try {
			updated = jdbcTemplate.update(
					"UPDATE SEARCH_CONFIGURATION SET ETAG = UUID(), NAME = ?, DESCRIPTION = ?,"
					+ " DEFAULT_ANALYZER = ?,"
					+ " COLUMN_ANALYZER_OVERRIDES = ?,"
					+ " COLUMN_SEMANTIC_ENRICHMENT = ?,"
					+ " MODIFIED_BY = ?, MODIFIED_ON = NOW(3) WHERE ID = ?",
					config.getName(),
					config.getDescription(),
					OpaqueJsonColumnCodecUtil.serialize(config.getDefaultAnalyzer(), DEFAULT_ANALYZER_FIELD),
					OpaqueJsonColumnCodecUtil.serialize(config.getColumnAnalyzerOverrides(), OVERRIDES_FIELD),
					OpaqueJsonColumnCodecUtil.serialize(config.getColumnSemanticEnrichment(), SEMANTIC_ENRICHMENT_FIELD),
					modifiedBy,
					id
			);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A search configuration with the same name already exists in this organization.", e);
		}

		if (updated == 0) {
			throw new NotFoundException("SearchConfiguration with id '" + config.getId() + "' does not exist.");
		}

		return get(config.getId()).orElseThrow(() -> new IllegalStateException("Failed to update SearchConfiguration"));
	}

	@WriteTransaction
	@Override
	public void delete(String id) {
		ValidateArgument.required(id, "id");
		jdbcTemplate.update("DELETE FROM SEARCH_CONFIGURATION WHERE ID = ?", Long.parseLong(id));
	}

	@Override
	public List<SearchConfiguration> list(String organizationName, long limit, long offset) {
		ValidateArgument.required(organizationName, "organizationName");
		return jdbcTemplate.query(
				"SELECT * FROM SEARCH_CONFIGURATION WHERE ORGANIZATION_NAME = ? ORDER BY NAME ASC LIMIT ? OFFSET ?",
				ROW_MAPPER, organizationName, limit, offset);
	}

	@Override
	public List<SearchConfiguration> listAll(long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM SEARCH_CONFIGURATION ORDER BY NAME ASC LIMIT ? OFFSET ?",
				ROW_MAPPER, limit, offset);
	}

	@WriteTransaction
	@Override
	public void bindSearchConfigToObject(Long searchConfigId, Long objectId, String objectType, Long createdBy) {
		ValidateArgument.required(searchConfigId, "searchConfigId");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(createdBy, "createdBy");
		Long bindId = idGenerator.generateNewId(IdType.SEARCH_CONFIG_BINDING_ID);
		jdbcTemplate.update(
				"INSERT INTO SEARCH_CONFIG_OBJECT_BINDING"
				+ " (BIND_ID, SEARCH_CONFIG_ID, OBJECT_ID, OBJECT_TYPE, CREATED_BY, CREATED_ON)"
				+ " VALUES (?, ?, ?, ?, ?, NOW(3))"
				+ " ON DUPLICATE KEY UPDATE SEARCH_CONFIG_ID = VALUES(SEARCH_CONFIG_ID),"
				+ " CREATED_BY = VALUES(CREATED_BY), CREATED_ON = NOW(3)",
				bindId, searchConfigId, objectId, objectType, createdBy);
	}

	@Override
	public Optional<SearchConfigBinding> getSearchConfigBindingForObject(Long objectId, String objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		try {
			SearchConfigBinding binding = jdbcTemplate.queryForObject(
					"SELECT * FROM SEARCH_CONFIG_OBJECT_BINDING WHERE OBJECT_ID = ? AND OBJECT_TYPE = ?",
					BINDING_ROW_MAPPER, objectId, objectType);
			return Optional.ofNullable(binding);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public void clearSearchConfigBinding(Long objectId, String objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		jdbcTemplate.update(
				"DELETE FROM SEARCH_CONFIG_OBJECT_BINDING WHERE OBJECT_ID = ? AND OBJECT_TYPE = ?",
				objectId, objectType);
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM SEARCH_CONFIG_OBJECT_BINDING WHERE BIND_ID > -1");
		jdbcTemplate.update("DELETE FROM SEARCH_CONFIGURATION WHERE ID > -1");
	}

	private String getCurrentEtagForUpdate(Long id) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT ETAG FROM SEARCH_CONFIGURATION WHERE ID = ? FOR UPDATE",
					String.class, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("SearchConfiguration with id '" + id + "' does not exist.");
		}
	}
}
