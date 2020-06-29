package org.sagebionetworks.repo.model.dbo.form;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_FILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_REJECTION_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_REVIEWED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_REVIEWED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_DATA_SUBMITTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORM_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORM_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORM_GROUP;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.ListRequest;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
	@Autowired
	NamedParameterJdbcTemplate namedTemplate;

	private static RowMapper<DBOFormGroup> GROUP_MAPPER = new DBOFormGroup().getTableMapping();
	private static RowMapper<DBOFormData> FORM_DATA_MAPPER = new DBOFormData().getTableMapping();

	private static RowMapper<SubmissionStatus> STATUS_MAPPER = (ResultSet rs, int rowNum) -> {
		SubmissionStatus status = new SubmissionStatus();
		status.setSubmittedOn(rs.getTimestamp(COL_FORM_DATA_SUBMITTED_ON));
		status.setReviewedOn(rs.getTimestamp(COL_FORM_DATA_REVIEWED_ON));
		status.setReviewedBy(rs.getString(COL_FORM_DATA_REVIEWED_BY));
		status.setState(StateEnum.valueOf(rs.getString(COL_FORM_DATA_STATE)));
		status.setRejectionMessage(rs.getString(COL_FORM_DATA_REJECTION_MESSAGE));
		return status;
	};
	
	private static RowMapper<FormGroup> FORM_GROUP_MAPPER = (ResultSet rs, int rowNum) -> {
		FormGroup group = new FormGroup();
		group.setGroupId(rs.getString(COL_FORM_GROUP_ID));
		group.setName(rs.getString(COL_FORM_GROUP_NAME));
		group.setCreatedBy(rs.getString(COL_FORM_GROUP_CREATED_BY));
		group.setCreatedOn(rs.getTimestamp(COL_FORM_GROUP_CREATED_ON));
		return group;
	};

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
	public long getFormDataCreator(String formDataId) {
		ValidateArgument.required(formDataId, "id");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_CREATED_BY + " FROM " + TABLE_FORM_DATA
					+ " WHERE " + COL_FORM_DATA_ID + " = ?", Long.class, formDataId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, formDataId));
		}
	}

	@Override
	public String getFormDataGroupId(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_GROUP_ID + " FROM " + TABLE_FORM_DATA
					+ " WHERE " + COL_FORM_DATA_ID + " = ?", String.class, formDataId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, formDataId));
		}
	}

	@WriteTransaction
	@Override
	public FormData updateFormData(String formDataId, String name, String dataFileHandleId) {
		ValidateArgument.required(formDataId, "formDataId");
		ValidateArgument.required(name, "name");
		ValidateArgument.required(dataFileHandleId, "dataFileHandleId");
		jdbcTemplate.update("UPDATE " + TABLE_FORM_DATA + " SET " + COL_FORM_DATA_NAME + " = ?, "
				+ COL_FORM_DATA_FILE_ID + " = ?, " + COL_FORM_DATA_ETAG + " = UUID(), " + COL_FORM_DATA_MODIFIED_ON
				+ " = NOW(3) WHERE " + COL_FORM_DATA_ID + " = ?", name, dataFileHandleId, formDataId);
		return getFormData(formDataId);
	}

	@WriteTransaction
	@Override
	public FormData updateFormData(String formDataId, String dataFileHandleId) {
		ValidateArgument.required(formDataId, "formDataId");
		ValidateArgument.required(dataFileHandleId, "dataFileHandleId");
		jdbcTemplate.update(
				"UPDATE " + TABLE_FORM_DATA + " SET " + COL_FORM_DATA_FILE_ID + " = ?, " + COL_FORM_DATA_ETAG
						+ " = UUID(), " + COL_FORM_DATA_MODIFIED_ON + " = NOW(3) WHERE " + COL_FORM_DATA_ID + " = ?",
				dataFileHandleId, formDataId);
		return getFormData(formDataId);
	}

	@Override
	public FormData getFormData(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", formDataId);
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

	/**
	 * Convert a list of DBOs to DTOs
	 * 
	 * @param dbos
	 * @return
	 */
	static List<FormData> dboToDbo(List<DBOFormData> dbos) {
		return dbos.stream().map(FormDaoImpl::dtoToDbo).collect(Collectors.toList());
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID + " > 0");
		jdbcTemplate.update("DELETE FROM " + TABLE_FORM_GROUP + " WHERE " + COL_FORM_GROUP_ID + " > 0");
	}

	@WriteTransaction
	@Override
	public boolean deleteFormData(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		int updateCount = jdbcTemplate.update("DELETE FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID + " = ?",
				formDataId);
		return updateCount > 0;
	}

	@Override
	public StateEnum getFormDataState(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		try {
			return StateEnum.valueOf(jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_STATE + " FROM "
					+ TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID + " = ?", String.class, formDataId));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, formDataId));
		}
	}

	@WriteTransaction
	@Override
	public SubmissionStatus getFormDataStatusForUpdate(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_SUBMITTED_ON + ", " + COL_FORM_DATA_REVIEWED_ON
					+ ", " + COL_FORM_DATA_REVIEWED_BY + ", " + COL_FORM_DATA_STATE + ", "
					+ COL_FORM_DATA_REJECTION_MESSAGE + " FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID
					+ " = ? FOR UPDATE", STATUS_MAPPER, formDataId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, formDataId));
		}
	}

	@WriteTransaction
	@Override
	public FormData updateStatus(String formDataId, SubmissionStatus status) {
		ValidateArgument.required(formDataId, "formDataId");
		ValidateArgument.required(status, "status");
		ValidateArgument.required(status.getState(), "status.state");
		jdbcTemplate.update(
				"UPDATE " + TABLE_FORM_DATA + " SET " + COL_FORM_DATA_ETAG + " = UUID(), " + COL_FORM_DATA_SUBMITTED_ON
						+ " = ?, " + COL_FORM_DATA_REVIEWED_ON + " = ?, " + COL_FORM_DATA_REVIEWED_BY + " = ?, "
						+ COL_FORM_DATA_STATE + " = ?, " + COL_FORM_DATA_REJECTION_MESSAGE + " = ?" + " WHERE "
						+ COL_FORM_DATA_ID + " = ?",
				status.getSubmittedOn(), status.getReviewedOn(), status.getReviewedBy(), status.getState().name(),
				status.getRejectionMessage(), formDataId);
		return getFormData(formDataId);
	}

	/**
	 * Convert from list of enums to list of strings.
	 * 
	 * @param states
	 * @return
	 */
	List<String> enumToString(Set<StateEnum> states) {
		return states.stream().map(StateEnum::name).collect(Collectors.toList());
	}

	@Override
	public List<FormData> listFormDataByCreator(Long creatorId, ListRequest request, long limit, long offset) {
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("creatorId", creatorId);
		paramSource.addValue("groupId", request.getGroupId());
		paramSource.addValue("limit", limit);
		paramSource.addValue("offset", offset);
		// States are converted from enums to strings.
		paramSource.addValue("states", enumToString(request.getFilterByState()));
		return dboToDbo(namedTemplate.query(
				"SELECT * FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_CREATED_BY + " = :creatorId AND "
						+ COL_FORM_GROUP_ID + " = :groupId AND " + COL_FORM_DATA_STATE + " IN (:states) ORDER BY "
						+ COL_FORM_DATA_MODIFIED_ON + " DESC LIMIT :limit OFFSET :offset",
				paramSource, FORM_DATA_MAPPER));
	}

	@Override
	public List<FormData> listFormDataForReviewer(ListRequest request, long limit, long offset) {
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("groupId", request.getGroupId());
		paramSource.addValue("limit", limit);
		paramSource.addValue("offset", offset);
		// States are converted from enums to strings.
		paramSource.addValue("states", enumToString(request.getFilterByState()));
		return dboToDbo(namedTemplate.query("SELECT * FROM " + TABLE_FORM_DATA + " WHERE " + COL_FORM_GROUP_ID
				+ " = :groupId AND " + COL_FORM_DATA_STATE + " IN (:states) ORDER BY " + COL_FORM_DATA_MODIFIED_ON
				+ " DESC LIMIT :limit OFFSET :offset", paramSource, FORM_DATA_MAPPER));
	}

	@Override
	public String getFormDataFileHandleId(String formDataId) {
		ValidateArgument.required(formDataId, "formDataId");
		try {
			return jdbcTemplate.queryForObject("SELECT " + COL_FORM_DATA_FILE_ID + " FROM "
					+ TABLE_FORM_DATA + " WHERE " + COL_FORM_DATA_ID + " = ?", String.class, formDataId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(FORM_DATA_DOES_NOT_EXIST_FOR_S, formDataId));
		}
	}

	@Override
	public FormGroup getFormGroup(String id) {
		ValidateArgument.required(id, "id");
		try {
			return jdbcTemplate.queryForObject("SELECT * FROM "+TABLE_FORM_GROUP+" WHERE "+COL_FORM_GROUP_ID+" = ?", FORM_GROUP_MAPPER, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("FormGroup does not exist for id: "+id);
		}
	}
	
}
