package org.sagebionetworks.repo.model.dbo.form;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_FILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORM_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.model.form.SubmissionStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class FormDaoImpl implements FormDao {

	public static final String FORM_DATA_DOES_NOT_EXIST_FOR_S = "FormData does not exist for: %s";
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	DBOBasicDao basicDao;
	@Autowired
	JdbcTemplate jdbcTemplate;

	private static RowMapper<DBOFormGroup> GROUP_MAPPER = new DBOFormGroup().getTableMapping();

	@WriteTransaction
	@Override
	public FormGroup createFormGroup(Long creator, String name) {
		ValidateArgument.required(creator, "creator");
		ValidateArgument.required(name, "name");
		DBOFormGroup dbo = new DBOFormGroup();
		dbo.setName(name);
		dbo.setCreatedBy(creator);
		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setGroupId(idGenerator.generateNewId(IdType.FORM_GROUP_ID));
		dbo = basicDao.createNew(dbo);
		return createGroupDto(dbo);
	}

	/**
	 * Create a FormGroup from the DBO.
	 * 
	 * @param dbo
	 * @return
	 */
	public static FormGroup createGroupDto(DBOFormGroup dbo) {
		ValidateArgument.required(dbo, "dbo");
		FormGroup dto = new FormGroup();
		dto.setGroupId(dbo.getGroupId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn().getTime()));
		dto.setName(dbo.getName());
		return dto;
	}

	@Override
	public Optional<FormGroup> lookupGroupByName(String name) {
		ValidateArgument.required(name, "name");
		try {
			DBOFormGroup dbo = jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_FORM_GROUP + " WHERE " + COL_FORM_GROUP_NAME + " = ?", GROUP_MAPPER, name);
			return Optional.of(createGroupDto(dbo));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public FormData createFormData(Long creatorId, String groupId, String name, String dataFileHandleId) {
		ValidateArgument.required(creatorId, "creatorId");
		ValidateArgument.required(groupId, "groupId");
		ValidateArgument.required(name, "name");
		ValidateArgument.required(dataFileHandleId, "dataFileHandleId");
		Timestamp now = new Timestamp(System.currentTimeMillis());
		DBOFormData dbo = new DBOFormData();
		dbo.setId(idGenerator.generateNewId(IdType.FORM_DATA_ID));
		dbo.setCreatedBy(creatorId);
		dbo.setGroupId(Long.parseLong(groupId));
		dbo.setName(name);
		dbo.setFileHandleId(Long.parseLong(dataFileHandleId));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setCreatedOn(now);
		dbo.setModifiedOn(now);
		dbo.setState(StateEnum.WAITING_FOR_SUBMISSION.name());
		basicDao.createNew(dbo);
		return getFormData(dbo.getId().toString());
	}

	@Override
	public long getFormDataCreator(String id) {
		ValidateArgument.required(id, "id");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_CREATED_BY + " FROM " + TABLE_FORM_DATA
					+ " WHERE " + COL_FORM_DATA_ID + " = ?", Long.class, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, id));
		}
	}

	@Override
	public String getFormDataGroupId(String id) {
		ValidateArgument.required(id, "id");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_GROUP_ID + " FROM " + TABLE_FORM_DATA
					+ " WHERE " + COL_FORM_DATA_ID + " = ?", String.class, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, id));
		}
	}

	@WriteTransaction
	@Override
	public FormData updateFormData(String id, String name, String dataFileHandleId) {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(name, "name");
		ValidateArgument.required(dataFileHandleId, "dataFileHandleId");
		jdbcTemplate.update("UPDATE " + TABLE_FORM_DATA + " SET " + COL_FORM_DATA_NAME + " = ?, "
				+ COL_FORM_DATA_FILE_ID + " = ?, " + COL_FORM_DATA_ETAG + " = UUID(), " + COL_FORM_DATA_MODIFIED_ON
				+ " = NOW(3) WHERE " + COL_FORM_DATA_ID + " = ?", name, dataFileHandleId, id);
		return getFormData(id);
	}

	@WriteTransaction
	@Override
	public FormData updateFormData(String id, String dataFileHandleId) {
		ValidateArgument.required(id, "id");
		ValidateArgument.required(dataFileHandleId, "dataFileHandleId");
		jdbcTemplate.update(
				"UPDATE " + TABLE_FORM_DATA + " SET " + COL_FORM_DATA_FILE_ID + " = ?, " + COL_FORM_DATA_ETAG
						+ " = UUID(), " + COL_FORM_DATA_MODIFIED_ON + " = NOW(3) WHERE " + COL_FORM_DATA_ID + " = ?",
				dataFileHandleId, id);
		return getFormData(id);
	}

	@Override
	public StateEnum getFormDataState(String id) {
		ValidateArgument.required(id, "id");
		try {
			return StateEnum.valueOf(jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_STATE + " FROM "
					+ TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID + " = ?", String.class, id));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, id));
		}
	}

	@Override
	public FormData getFormData(String id) {
		ValidateArgument.required(id, "id");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", id);
		DBOFormData dto = basicDao.getObjectByPrimaryKey(DBOFormData.class, param);
		return dtoToDbo(dto);
	}

	/**
	 * Convert from a DBO to dto.
	 * 
	 * @param dbo
	 * @return
	 */
	static FormData dtoToDbo(DBOFormData dbo) {
		FormData dto = new FormData();
		dto.setFormDataId(dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		dto.setName(dbo.getName());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setModifiedOn(dbo.getModifiedOn());
		dto.setGroupId(dbo.getGroupId().toString());
		dto.setDataFileHandleId(dbo.getFileHandleId().toString());
		SubmissionStatus status = new SubmissionStatus();
		if (dbo.getSubmittedOn() != null) {
			status.setSubmittedOn(dbo.getSubmittedOn());
		}
		if (dbo.getReviewedOn() != null) {
			status.setReviewedOn(dbo.getReviewedOn());
		}
		if (dbo.getReviewedBy() != null) {
			status.setReviewedBy(dbo.getReviewedBy().toString());
		}
		status.setState(StateEnum.valueOf(dbo.getState()));
		status.setRejectionMessage(dbo.getRejectionMessage());
		dto.setSubmissionStatus(status);
		return dto;
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID + " > 0");
		jdbcTemplate.update("DELETE FROM " + TABLE_FORM_GROUP + " WHERE " + COL_FORM_GROUP_ID + " > 0");
	}

	@WriteTransaction
	@Override
	public void deleteFormData(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		jdbcTemplate.update("DELETE FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID +" = ?", formDataId);
	}

}
