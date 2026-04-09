package org.sagebionetworks.repo.model.dbo.search;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TextAnalyzerDaoImpl implements TextAnalyzerDao {

	private static final String MSG_DUPLICATE_NAME = "A text analyzer with the given name already exists in this organization.";

	private static final RowMapper<TextAnalyzer> ROW_MAPPER = (rs, rowNum) -> {
		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setId(String.valueOf(rs.getLong("ID")));
		analyzer.setEtag(rs.getString("ETAG"));
		analyzer.setName(rs.getString("NAME"));
		analyzer.setDescription(rs.getString("DESCRIPTION"));
		analyzer.setOrganizationName(rs.getString("ORGANIZATION_NAME"));
		analyzer.setSettings(JDOSecondaryPropertyUtils.createObjectFromJSON(TextAnalyzerSettings.class, rs.getString("SETTINGS")));
		analyzer.setCreatedBy(String.valueOf(rs.getLong("CREATED_BY")));
		analyzer.setCreatedOn(new Date(rs.getTimestamp("CREATED_ON").getTime()));
		analyzer.setModifiedBy(String.valueOf(rs.getLong("MODIFIED_BY")));
		analyzer.setModifiedOn(new Date(rs.getTimestamp("MODIFIED_ON").getTime()));
		return analyzer;
	};

	private final JdbcTemplate jdbcTemplate;
	private final IdGenerator idGenerator;

	public TextAnalyzerDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	@WriteTransaction
	@Override
	public TextAnalyzer create(TextAnalyzer analyzer, Long userId) {
		ValidateArgument.required(analyzer, "analyzer");
		ValidateArgument.required(analyzer.getName(), "analyzer.name");
		ValidateArgument.required(analyzer.getOrganizationName(), "analyzer.organizationName");
		ValidateArgument.required(analyzer.getSettings(), "analyzer.settings");
		ValidateArgument.required(userId, "userId");

		Long id = idGenerator.generateNewId(IdType.TEXT_ANALYZER_ID);

		try {
			jdbcTemplate.update(
					"INSERT INTO TEXT_ANALYZER (ID, ETAG, NAME, DESCRIPTION, ORGANIZATION_NAME, SETTINGS,"
					+ " CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
					+ " VALUES (?, UUID(), ?, ?, ?, ?, ?, NOW(3), ?, NOW(3))",
					id,
					analyzer.getName(),
					analyzer.getDescription(),
					analyzer.getOrganizationName(),
					JDOSecondaryPropertyUtils.createJSONFromObject(analyzer.getSettings()),
					userId,
					userId
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(MSG_DUPLICATE_NAME, e);
		}

		return get(id).orElseThrow(() -> new IllegalStateException("Failed to create TextAnalyzer"));
	}

	@Override
	public Optional<TextAnalyzer> get(Long id) {
		ValidateArgument.required(id, "id");
		try {
			TextAnalyzer result = jdbcTemplate.queryForObject(
					"SELECT * FROM TEXT_ANALYZER WHERE ID = ?",
					ROW_MAPPER, id);
			return Optional.ofNullable(result);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public TextAnalyzer update(TextAnalyzer analyzer, Long userId) {
		ValidateArgument.required(analyzer, "analyzer");
		ValidateArgument.required(analyzer.getId(), "analyzer.id");
		ValidateArgument.required(userId, "userId");

		Long id = Long.parseLong(analyzer.getId());

		String currentEtag = getCurrentEtagForUpdate(id);
		if (!currentEtag.equals(analyzer.getEtag())) {
			throw new ConflictingUpdateException("TextAnalyzer was updated since last fetched. Please re-fetch and try again.");
		}

		int updated;
		try {
			updated = jdbcTemplate.update(
					"UPDATE TEXT_ANALYZER SET ETAG = UUID(), NAME = ?, DESCRIPTION = ?, SETTINGS = ?,"
					+ " MODIFIED_BY = ?, MODIFIED_ON = NOW(3) WHERE ID = ?",
					analyzer.getName(),
					analyzer.getDescription(),
					JDOSecondaryPropertyUtils.createJSONFromObject(analyzer.getSettings()),
					userId,
					id
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(MSG_DUPLICATE_NAME, e);
		}

		if (updated == 0) {
			throw new NotFoundException("TextAnalyzer with id '" + analyzer.getId() + "' does not exist.");
		}

		return get(id).orElseThrow(() -> new IllegalStateException("Failed to update TextAnalyzer"));
	}

	@WriteTransaction
	@Override
	public void delete(Long id) {
		ValidateArgument.required(id, "id");
		jdbcTemplate.update("DELETE FROM TEXT_ANALYZER WHERE ID = ?", id);
	}

	@Override
	public List<TextAnalyzer> listByOrganization(String organizationName, long limit, long offset) {
		ValidateArgument.required(organizationName, "organizationName");
		return jdbcTemplate.query(
				"SELECT * FROM TEXT_ANALYZER WHERE ORGANIZATION_NAME = ? ORDER BY NAME ASC LIMIT ? OFFSET ?",
				ROW_MAPPER, organizationName, limit, offset);
	}

	@Override
	public List<TextAnalyzer> listAll(long limit, long offset) {
		return jdbcTemplate.query(
				"SELECT * FROM TEXT_ANALYZER ORDER BY NAME ASC LIMIT ? OFFSET ?",
				ROW_MAPPER, limit, offset);
	}

	@Override
	public boolean exists(Long id) {
		ValidateArgument.required(id, "id");
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM TEXT_ANALYZER WHERE ID = ?",
				Integer.class, id);
		return count > 0;
	}

	@WriteTransaction
	@Override
	public void createOrUpdateSystemAnalyzerForBootstrapOnly(Long id, TextAnalyzer analyzer, String organizationName, Long userId) {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(analyzer, "analyzer");
		ValidateArgument.required(analyzer.getName(), "analyzer.name");
		ValidateArgument.required(analyzer.getSettings(), "analyzer.settings");
		ValidateArgument.required(organizationName, "organizationName");
		ValidateArgument.required(userId, "userId");

		jdbcTemplate.update(
				"INSERT INTO TEXT_ANALYZER (ID, ETAG, NAME, DESCRIPTION, ORGANIZATION_NAME, SETTINGS,"
				+ " CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
				+ " VALUES (?, UUID(), ?, ?, ?, ?, ?, NOW(3), ?, NOW(3))"
				+ " ON DUPLICATE KEY UPDATE"
				+ " ETAG = UUID(), NAME = VALUES(NAME), DESCRIPTION = VALUES(DESCRIPTION),"
				+ " SETTINGS = VALUES(SETTINGS), MODIFIED_BY = VALUES(MODIFIED_BY), MODIFIED_ON = NOW(3)",
				id,
				analyzer.getName(),
				analyzer.getDescription(),
				organizationName,
				JDOSecondaryPropertyUtils.createJSONFromObject(analyzer.getSettings()),
				userId,
				userId
		);
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM TEXT_ANALYZER WHERE ID > -1");
	}

	private String getCurrentEtagForUpdate(Long id) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT ETAG FROM TEXT_ANALYZER WHERE ID = ? FOR UPDATE",
					String.class, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("TextAnalyzer with id '" + id + "' does not exist.");
		}
	}
}
