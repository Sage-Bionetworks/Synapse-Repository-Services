package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FormTemplateDaoImpl implements FormTemplateDao {

	/**
	 * The version number given to the first version of every template.
	 */
	public static final long FIRST_VERSION_NUMBER = 1L;

	private static final String SELECT_VERSION_JOIN = "SELECT T.ID, T.ETAG, R.NUMBER, R.TEMPLATE_JSON"
			+ " FROM FORM_TEMPLATE T JOIN FORM_TEMPLATE_REVISION R ON (T.ID = R.OWNER_ID)";

	private static final RowMapper<FormTemplate> TEMPLATE_MAPPER = (rs, rowNum) -> {
		FormTemplate template = JDOSecondaryPropertyUtils.createObjectFromJSON(FormTemplate.class,
				rs.getString("TEMPLATE_JSON"));
		// The id and version number identify the row, while the etag always reflects the
		// current state of the mutable owner row.
		template.setId(Long.toString(rs.getLong("ID")));
		template.setVersionNumber(rs.getLong("NUMBER"));
		template.setEtag(rs.getString("ETAG"));
		return template;
	};

	private final IdGenerator idGenerator;
	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedJdbcTemplate;

	public FormTemplateDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate,
			NamedParameterJdbcTemplate namedJdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
		this.namedJdbcTemplate = namedJdbcTemplate;
	}

	@WriteTransaction
	@Override
	public FormTemplate create(Long userId, FormTemplate template) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(template, "template");
		ValidateArgument.required(template.getName(), "template.name");

		Long id = idGenerator.generateNewId(IdType.FORM_TEMPLATE_ID);
		FormTemplate body = createVersionBody(template, id, FIRST_VERSION_NUMBER);
		String etag = UUID.randomUUID().toString();
		long now = System.currentTimeMillis();

		insertTemplate(id, etag, body.getName(), FIRST_VERSION_NUMBER, userId, now);
		insertTemplateRevision(id, FIRST_VERSION_NUMBER, userId, now, body);

		return body.setEtag(etag);
	}

	@WriteTransaction
	@Override
	public FormTemplate createNewVersion(Long userId, FormTemplate template) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(template, "template");
		ValidateArgument.required(template.getName(), "template.name");
		ValidateArgument.required(template.getId(), "template.id");
		ValidateArgument.required(template.getVersionNumber(), "template.versionNumber");

		Long id = Long.parseLong(template.getId());
		FormTemplate body = createVersionBody(template, id, template.getVersionNumber());
		String etag = UUID.randomUUID().toString();
		long now = System.currentTimeMillis();

		// The owner row always points at the newest version and carries the name it was given.
		updateTemplateToVersion(id, etag, body.getName(), body.getVersionNumber());
		insertTemplateRevision(id, body.getVersionNumber(), userId, now, body);

		return body.setEtag(etag);
	}

	@Override
	public Optional<FormTemplate> getLatestVersion(Long id) {
		ValidateArgument.required(id, "id");
		return jdbcTemplate
				.query(SELECT_VERSION_JOIN + " WHERE T.ID = ? AND T.CURRENT_REV_NUM = R.NUMBER", TEMPLATE_MAPPER, id)
				.stream().findFirst();
	}

	@Override
	public Optional<FormTemplate> getVersion(Long id, Long versionNumber) {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(versionNumber, "versionNumber");
		return jdbcTemplate
				.query(SELECT_VERSION_JOIN + " WHERE T.ID = ? AND R.NUMBER = ?", TEMPLATE_MAPPER, id, versionNumber)
				.stream().findFirst();
	}

	@MandatoryWriteTransaction
	@Override
	public Optional<FormTemplateInfoForUpdate> getForUpdate(Long id) {
		ValidateArgument.required(id, "id");
		return jdbcTemplate.query("SELECT ID, ETAG, CURRENT_REV_NUM FROM FORM_TEMPLATE WHERE ID = ? FOR UPDATE",
				(rs, rowNum) -> new FormTemplateInfoForUpdate(rs.getLong("ID"), rs.getString("ETAG"),
						rs.getLong("CURRENT_REV_NUM")),
				id).stream().findFirst();
	}

	@Override
	public List<FormTemplate> searchLatestVersions(String nameFilter, boolean includeDeprecated, long limit,
			long offset) {
		List<String> conditions = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		conditions.add("T.CURRENT_REV_NUM = R.NUMBER");
		if (nameFilter != null) {
			// The NAME column uses a case insensitive collation, so LIKE matches regardless of case.
			conditions.add("T.NAME LIKE ? ESCAPE '\\\\'");
			parameters.add("%" + escapeLikeWildcards(nameFilter) + "%");
		}
		if (!includeDeprecated) {
			conditions.add("R.DEPRECATED = FALSE");
		}
		parameters.add(limit);
		parameters.add(offset);

		return jdbcTemplate.query(SELECT_VERSION_JOIN + " WHERE " + String.join(" AND ", conditions)
				+ " ORDER BY T.ID LIMIT ? OFFSET ?", TEMPLATE_MAPPER, parameters.toArray());
	}

	@Override
	public boolean exists(Long id) {
		ValidateArgument.required(id, "id");
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FORM_TEMPLATE WHERE ID = ?", Long.class, id) > 0;
	}

	@Override
	public Set<String> getAttachmentFileHandleIds(Long id, Collection<String> fileHandleIds) {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		if (fileHandleIds.isEmpty()) {
			return Collections.emptySet();
		}
		// The attachments of every version are unnested, so a file stays associated after a newer
		// version stops listing it. The owner id is the leading key column, so only the versions of
		// the given template are read.
		return new HashSet<>(namedJdbcTemplate.queryForList("SELECT DISTINCT A.FILE_HANDLE_ID FROM FORM_TEMPLATE_REVISION R"
				+ " JOIN JSON_TABLE(R.TEMPLATE_JSON, '$.attachments[*]' COLUMNS (FILE_HANDLE_ID VARCHAR(30) PATH '$')) AS A"
				+ " WHERE R.OWNER_ID = :id AND A.FILE_HANDLE_ID IN (:fileHandleIds)",
				Map.of("id", id, "fileHandleIds", fileHandleIds), String.class));
	}

	@Override
	public void truncateAll() {
		// The revisions are removed by the cascading foreign key on their owner.
		jdbcTemplate.update("DELETE FROM FORM_TEMPLATE WHERE ID > -1");
	}

	private void insertTemplate(Long id, String etag, String name, long versionNumber, Long userId, long createdOn) {
		try {
			jdbcTemplate.update("INSERT INTO FORM_TEMPLATE (ID, ETAG, NAME, CURRENT_REV_NUM, CREATED_BY, CREATED_ON)"
					+ " VALUES (?, ?, ?, ?, ?, ?)", id, etag, name, versionNumber, userId, createdOn);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("A form template with the name '" + name + "' already exists.", e);
		}
	}

	private void updateTemplateToVersion(Long id, String etag, String name, Long versionNumber) {
		try {
			jdbcTemplate.update("UPDATE FORM_TEMPLATE SET ETAG = ?, NAME = ?, CURRENT_REV_NUM = ? WHERE ID = ?", etag,
					name, versionNumber, id);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("A form template with the name '" + name + "' already exists.", e);
		}
	}

	private void insertTemplateRevision(Long ownerId, Long versionNumber, Long userId, long modifiedOn,
			FormTemplate body) {
		jdbcTemplate.update(
				"INSERT INTO FORM_TEMPLATE_REVISION (OWNER_ID, NUMBER, MODIFIED_BY, MODIFIED_ON, DEPRECATED, TEMPLATE_JSON)"
						+ " VALUES (?, ?, ?, ?, ?, ?)",
				ownerId, versionNumber, userId, modifiedOn, body.getDeprecated(),
				JDOSecondaryPropertyUtils.createJSONFromObject(body));
	}

	/**
	 * Builds the body that will be stored for a version. The etag is left out because it belongs to
	 * the mutable owner row, and the deprecated flag is defaulted since it drives a not null column.
	 */
	private static FormTemplate createVersionBody(FormTemplate template, Long id, Long versionNumber) {
		FormTemplate body = JDOSecondaryPropertyUtils.createObjectFromJSON(FormTemplate.class,
				JDOSecondaryPropertyUtils.createJSONFromObject(template));
		return body.setId(Long.toString(id)).setVersionNumber(versionNumber).setEtag(null)
				.setDeprecated(Boolean.TRUE.equals(template.getDeprecated()));
	}

	private static String escapeLikeWildcards(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
