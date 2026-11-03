package org.sagebionetworks.repo.model.dbo.search;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_ORGANIZATION_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEARCH_PIPELINE_SETTINGS;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.search.dsl.SearchPipeline;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SearchPipelineDaoImpl implements SearchPipelineDao {

	private static final RowMapper<NamedSearchPipeline> SEARCH_PIPELINE_ROW_MAPPER = (rs, rowNum) -> new NamedSearchPipeline()
		.setId(String.valueOf(rs.getLong(COL_SEARCH_PIPELINE_ID)))
		.setEtag(rs.getString(COL_SEARCH_PIPELINE_ETAG))
		.setOrganizationName(rs.getString(COL_SEARCH_PIPELINE_ORGANIZATION_NAME))
		.setName(rs.getString(COL_SEARCH_PIPELINE_NAME))
		.setDescription(rs.getString(COL_SEARCH_PIPELINE_DESCRIPTION))
		.setSettings(JDOSecondaryPropertyUtils.createObjectFromJSON(SearchPipeline.class, rs.getString(COL_SEARCH_PIPELINE_SETTINGS)))
		.setCreatedBy(String.valueOf(rs.getLong(COL_SEARCH_PIPELINE_CREATED_BY)))
		.setCreatedOn(new Date(rs.getTimestamp(COL_SEARCH_PIPELINE_CREATED_ON).getTime()))
		.setModifiedBy(String.valueOf(rs.getLong(COL_SEARCH_PIPELINE_MODIFIED_BY)))
		.setModifiedOn(new Date(rs.getTimestamp(COL_SEARCH_PIPELINE_MODIFIED_ON).getTime()));

	private final JdbcTemplate jdbcTemplate;
	private final IdGenerator idGenerator;

	public SearchPipelineDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	@Override
	@WriteTransaction
	public NamedSearchPipeline create(Long createdBy, NamedSearchPipeline pipeline) {
		ValidateArgument.required(pipeline, "pipeline");
		ValidateArgument.required(pipeline.getSettings(), "pipeline.settings");
		Long id = idGenerator.generateNewId(IdType.SEARCH_PIPELINE_ID);

		try {
			jdbcTemplate.update(
					"INSERT INTO SEARCH_PIPELINE (ID, ETAG, ORGANIZATION_NAME, NAME, DESCRIPTION, SETTINGS,"
					+ " CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
					+ " VALUES (?, UUID(), ?, ?, ?, ?, ?, NOW(3), ?, NOW(3))",
					id,
					pipeline.getOrganizationName(),
					pipeline.getName(),
					pipeline.getDescription(),
					JDOSecondaryPropertyUtils.createJSONFromObject(pipeline.getSettings()),
					createdBy,
					createdBy
			);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A search pipeline with the same name already exists in this organization.", e);
		}

		return get(id.toString()).orElseThrow(() -> new IllegalStateException("The search pipeline was not created."));
	}

	@Override
	public Optional<NamedSearchPipeline> get(String id) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject(
					"SELECT * FROM SEARCH_PIPELINE WHERE ID = ?",
					SEARCH_PIPELINE_ROW_MAPPER, Long.parseLong(id)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	@WriteTransaction
	public NamedSearchPipeline update(Long modifiedBy, NamedSearchPipeline pipeline) {
		ValidateArgument.required(pipeline, "pipeline");
		ValidateArgument.required(pipeline.getSettings(), "pipeline.settings");

		String currentEtag = getCurrentEtagForUpdate(Long.parseLong(pipeline.getId()));
		if (!currentEtag.equals(pipeline.getEtag())) {
			throw new ConflictingUpdateException("SearchPipeline was updated since last fetched. Please re-fetch and try again.");
		}

		jdbcTemplate.update(
				"UPDATE SEARCH_PIPELINE SET ETAG = UUID(), DESCRIPTION = ?, SETTINGS = ?,"
				+ " MODIFIED_BY = ?, MODIFIED_ON = NOW(3) WHERE ID = ?",
				pipeline.getDescription(),
				JDOSecondaryPropertyUtils.createJSONFromObject(pipeline.getSettings()),
				modifiedBy,
				Long.parseLong(pipeline.getId())
		);

		return get(pipeline.getId()).orElseThrow(() -> new IllegalStateException("The search pipeline was not updated."));
	}

	@Override
	public List<NamedSearchPipeline> list(String organizationName, long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM SEARCH_PIPELINE WHERE ORGANIZATION_NAME = ? ORDER BY ID LIMIT ? OFFSET ?",
				SEARCH_PIPELINE_ROW_MAPPER, organizationName, limit, offset);
	}

	@Override
	public List<NamedSearchPipeline> listAll(long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM SEARCH_PIPELINE ORDER BY ID LIMIT ? OFFSET ?",
				SEARCH_PIPELINE_ROW_MAPPER, limit, offset);
	}

	@Override
	public Optional<NamedSearchPipeline> getByOrganizationAndName(String organizationName, String name) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject(
					"SELECT * FROM SEARCH_PIPELINE WHERE ORGANIZATION_NAME = ? AND NAME = ?",
					SEARCH_PIPELINE_ROW_MAPPER, organizationName, name));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	@WriteTransaction
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM SEARCH_PIPELINE WHERE ID > -1");
	}

	private String getCurrentEtagForUpdate(Long id) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT ETAG FROM SEARCH_PIPELINE WHERE ID = ? FOR UPDATE",
					String.class, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("SearchPipeline with id '" + id + "' does not exist.");
		}
	}

}
