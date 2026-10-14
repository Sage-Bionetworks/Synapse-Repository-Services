package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

	public FormTemplateDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
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
