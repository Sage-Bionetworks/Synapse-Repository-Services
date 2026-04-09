package org.sagebionetworks.repo.model.dbo.search;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ColumnAnalyzerOverrideDaoImpl implements ColumnAnalyzerOverrideDao {

	private final JdbcTemplate jdbcTemplate;
	private final IdGenerator idGenerator;

	public ColumnAnalyzerOverrideDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	private static final RowMapper<ColumnAnalyzerOverride> ROW_MAPPER = (rs, rowNum) -> {
		ColumnAnalyzerOverride dto = new ColumnAnalyzerOverride();
		dto.setId(String.valueOf(rs.getLong("ID")));
		dto.setEtag(rs.getString("ETAG"));
		dto.setOrganizationName(rs.getString("ORGANIZATION_NAME"));
		dto.setName(rs.getString("NAME"));
		dto.setDescription(rs.getString("DESCRIPTION"));
		dto.setOverrides(JDOSecondaryPropertyUtils.readJsonToEntityList(rs.getString("OVERRIDES"), ColumnAnalyzerOverrideEntry.class));
		dto.setCreatedBy(String.valueOf(rs.getLong("CREATED_BY")));
		dto.setCreatedOn(new Date(rs.getTimestamp("CREATED_ON").getTime()));
		dto.setModifiedBy(String.valueOf(rs.getLong("MODIFIED_BY")));
		dto.setModifiedOn(new Date(rs.getTimestamp("MODIFIED_ON").getTime()));
		return dto;
	};

	@Override
	@WriteTransaction
	public ColumnAnalyzerOverride create(Long createdBy, ColumnAnalyzerOverride override) {
		Long id = idGenerator.generateNewId(IdType.COLUMN_ANALYZER_OVERRIDE_ID);

		try {
			jdbcTemplate.update(
					"INSERT INTO COLUMN_ANALYZER_OVERRIDE (ID, ETAG, ORGANIZATION_NAME, NAME, DESCRIPTION,"
					+ " OVERRIDES, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
					+ " VALUES (?, UUID(), ?, ?, ?, ?, ?, NOW(3), ?, NOW(3))",
					id,
					override.getOrganizationName(),
					override.getName(),
					override.getDescription(),
					override.getOverrides() == null ? "[]" : JDOSecondaryPropertyUtils.writeEntityListToJson(override.getOverrides()),
					createdBy,
					createdBy
			);
		} catch (DuplicateKeyException e) {
			handleDuplicateKeyException(e);
		}

		return get(String.valueOf(id))
				.orElseThrow(() -> new IllegalStateException("The column analyzer override was not created."));
	}

	@Override
	public Optional<ColumnAnalyzerOverride> get(String id) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject(
					"SELECT * FROM COLUMN_ANALYZER_OVERRIDE WHERE ID = ?",
					ROW_MAPPER, Long.parseLong(id)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	@WriteTransaction
	public ColumnAnalyzerOverride update(Long modifiedBy, ColumnAnalyzerOverride override) {
		String currentEtag = getEtagForUpdate(override.getId());

		if (!currentEtag.equals(override.getEtag())) {
			throw new ConflictingUpdateException(
					"The column analyzer override was updated since you last fetched it, please fetch it again and reapply your changes.");
		}

		try {
			jdbcTemplate.update(
					"UPDATE COLUMN_ANALYZER_OVERRIDE SET ETAG = UUID(), NAME = ?, DESCRIPTION = ?,"
					+ " OVERRIDES = ?, MODIFIED_BY = ?, MODIFIED_ON = NOW(3) WHERE ID = ?",
					override.getName(),
					override.getDescription(),
					override.getOverrides() == null ? "[]" : JDOSecondaryPropertyUtils.writeEntityListToJson(override.getOverrides()),
					modifiedBy,
					Long.parseLong(override.getId())
			);
		} catch (DuplicateKeyException e) {
			handleDuplicateKeyException(e);
		}

		return get(override.getId())
				.orElseThrow(() -> new IllegalStateException("The column analyzer override was not updated."));
	}

	@Override
	@WriteTransaction
	public void delete(String id) {
		try {
			jdbcTemplate.update("DELETE FROM COLUMN_ANALYZER_OVERRIDE WHERE ID = ?", Long.parseLong(id));
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("Cannot delete column analyzer override '" + id + "' because it is still referenced.", e);
		}
	}

	@Override
	public List<ColumnAnalyzerOverride> list(String organizationName, long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM COLUMN_ANALYZER_OVERRIDE WHERE ORGANIZATION_NAME = ? ORDER BY ID LIMIT ? OFFSET ?",
				ROW_MAPPER, organizationName, limit, offset);
	}

	@Override
	public List<ColumnAnalyzerOverride> listAll(long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM COLUMN_ANALYZER_OVERRIDE ORDER BY ID LIMIT ? OFFSET ?",
				ROW_MAPPER, limit, offset);
	}

	@Override
	@WriteTransaction
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM COLUMN_ANALYZER_OVERRIDE WHERE ID > -1");
	}

	private String getEtagForUpdate(String id) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT ETAG FROM COLUMN_ANALYZER_OVERRIDE WHERE ID = ? FOR UPDATE",
					String.class, Long.parseLong(id));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("A column analyzer override with ID " + id + " does not exist.");
		}
	}

	private static void handleDuplicateKeyException(DuplicateKeyException e) {
		if (e.getMessage() != null && e.getMessage().contains("UNIQUE_CAO_ORG_NAME")) {
			throw new IllegalArgumentException(
					"A column analyzer override with the same name already exists in this organization.", e);
		}
		throw e;
	}

}
