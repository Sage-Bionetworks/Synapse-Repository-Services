package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_PROJECT_AR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_BINDING_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT_PROJECTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT_REVISION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBJECT_ACCESS_REQUIREMENT;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementInfoForUpdate;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchSort;
import org.sagebionetworks.repo.model.dataaccess.BindingType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TemporaryCode;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DBOAccessRequirementDAOImpl implements AccessRequirementDAO {
	public static final String ACCESS_REQUIREMENT_DOES_NOT_EXIST = "Access Requirement: '%s' does not exist";
	public static final String LIMIT_PARAM = "LIMIT";
	public static final String OFFSET_PARAM = "OFFSET";
	public static final Long DEFAULT_VERSION = 0L;
	
	private DBOBasicDao basicDao;
	private IdGenerator idGenerator;
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public DBOAccessRequirementDAOImpl(DBOBasicDao basicDao, IdGenerator idGenerator,
			NamedParameterJdbcTemplate namedJdbcTemplate, JdbcTemplate jdbcTemplate) {
		super();
		this.basicDao = basicDao;
		this.idGenerator = idGenerator;
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	private static final String UPDATE_ACCESS_REQUIREMENT_SQL = "UPDATE " + TABLE_ACCESS_REQUIREMENT + " SET "
			+ COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER + " = :" + COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER
			+ ", " + COL_ACCESS_REQUIREMENT_ETAG + " = :" + COL_ACCESS_REQUIREMENT_ETAG + ", "
			+ COL_ACCESS_REQUIREMENT_CONCRETE_TYPE + " = :" + COL_ACCESS_REQUIREMENT_CONCRETE_TYPE + ", "
			+ COL_ACCESS_REQUIREMENT_NAME + " = :" + COL_ACCESS_REQUIREMENT_NAME + ", "
			+ COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED + " = :" + COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED 
			+ " WHERE " + COL_ACCESS_REQUIREMENT_ID + " = :" + COL_ACCESS_REQUIREMENT_ID;

	private static final String SELECT_CURRENT_REQUIREMENTS_BY_ID = "SELECT *" + " FROM " + TABLE_ACCESS_REQUIREMENT
			+ " REQ" + " JOIN " + TABLE_ACCESS_REQUIREMENT_REVISION + " REV" + " ON (REQ." + COL_ACCESS_REQUIREMENT_ID
			+ " = REV." + COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID + " AND REQ."
			+ COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER + " = REV." + COL_ACCESS_REQUIREMENT_REVISION_NUMBER + ")"
			+ " WHERE REQ." + COL_ACCESS_REQUIREMENT_ID + " IN (:" + COL_ACCESS_REQUIREMENT_ID.toLowerCase() + ")"
			+ " ORDER BY REQ." + COL_ACCESS_REQUIREMENT_ID;
	
	private static final String SELECT_REQUIREMENT_BY_ID_AND_VERSION = "SELECT * FROM " + TABLE_ACCESS_REQUIREMENT
			+ " REQ JOIN " + TABLE_ACCESS_REQUIREMENT_REVISION + " REV" + " ON (REQ." + COL_ACCESS_REQUIREMENT_ID
			+ " = REV." + COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID + ")"
			+ " WHERE REQ." + COL_ACCESS_REQUIREMENT_ID + " =:" + COL_ACCESS_REQUIREMENT_ID
			+ " AND REV." + COL_ACCESS_REQUIREMENT_REVISION_NUMBER + "=:" + COL_ACCESS_REQUIREMENT_REVISION_NUMBER;

	private static final String GET_ACCESS_REQUIREMENTS_IDS_FOR_SUBJECTS_SQL = "SELECT DISTINCT "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + " FROM " + TABLE_SUBJECT_ACCESS_REQUIREMENT + " WHERE "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + " IN (:" + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + ") "
			+ " AND " + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE + "=:"
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;

	private static final String GET_SUBJECT_ACCESS_REQUIREMENT_SQL = "SELECT *" + " FROM "
			+ TABLE_SUBJECT_ACCESS_REQUIREMENT + " WHERE " + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + "=:"
			+ COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID;

	private static final String GET_SUBJECT_ACCESS_REQUIREMENT_WITH_LIMIT_AND_OFFSET = GET_SUBJECT_ACCESS_REQUIREMENT_SQL
			+ " " + LIMIT_PARAM + " :" + LIMIT_PARAM + " " + OFFSET_PARAM + " :" + OFFSET_PARAM;

	private static final String SELECT_INFO_FOR_UPDATE_SQL = "SELECT " + COL_ACCESS_REQUIREMENT_ID + ", "
			+ COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER + ", " + COL_ACCESS_REQUIREMENT_ETAG + ", "
			+ COL_ACCESS_REQUIREMENT_ACCESS_TYPE + ", " + COL_ACCESS_REQUIREMENT_CONCRETE_TYPE + " FROM "
			+ TABLE_ACCESS_REQUIREMENT + " WHERE " + COL_ACCESS_REQUIREMENT_ID + "=:" + COL_ACCESS_REQUIREMENT_ID
			+ " FOR UPDATE";

	private static final String SELECT_FOR_UPDATE_SQL = "SELECT *" + " FROM " + TABLE_ACCESS_REQUIREMENT + " REQ"
			+ " JOIN " + TABLE_ACCESS_REQUIREMENT_REVISION + " REV" + " ON (REQ." + COL_ACCESS_REQUIREMENT_ID
			+ " = REV." + COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID + " AND REQ."
			+ COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER + " = REV." + COL_ACCESS_REQUIREMENT_REVISION_NUMBER + ")"
			+ " WHERE " + COL_ACCESS_REQUIREMENT_ID + "=:" + COL_ACCESS_REQUIREMENT_ID + " FOR UPDATE";

	private static final String DELETE_SUBJECT_ACCESS_REQUIREMENTS_SQL = "DELETE FROM "
			+ TABLE_SUBJECT_ACCESS_REQUIREMENT + " WHERE " + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + "=:"
			+ COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID +" AND "+COL_SUBJECT_ACCESS_REQUIREMENT_BINDING_TYPE+" =:bindingType";

	private static final String GET_ACCESS_REQUIREMENTS_IDS_PAGE_SQL = GET_ACCESS_REQUIREMENTS_IDS_FOR_SUBJECTS_SQL
			+ " " + LIMIT_PARAM + " :" + LIMIT_PARAM + " " + OFFSET_PARAM + " :" + OFFSET_PARAM;

	private static final String SELECT_CONCRETE_TYPE_SQL = "SELECT " + COL_ACCESS_REQUIREMENT_CONCRETE_TYPE + " FROM "
			+ TABLE_ACCESS_REQUIREMENT + " WHERE " + COL_ACCESS_REQUIREMENT_ID + " = ?";

	private static final String SELECT_ACCESS_REQUIREMENT_STATS = "SELECT " + COL_ACCESS_REQUIREMENT_ID + ", "
			+ COL_ACCESS_REQUIREMENT_CONCRETE_TYPE + " FROM " + TABLE_ACCESS_REQUIREMENT + ", "
			+ TABLE_SUBJECT_ACCESS_REQUIREMENT + " WHERE " + COL_ACCESS_REQUIREMENT_ID + " = "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + " AND " + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID
			+ " IN (:" + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + ")" + " AND "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE + " = :" + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE;

	private static final String SOURCE_SUBJECTS = "SOURCE_SUBJECTS";
	private static final String DEST_SUBJECTS = "DEST_SUBJECTS";
	private static final String SELECT_ACCESS_REQUIREMENT_DIFF = "SELECT DISTINCT "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + " FROM " + TABLE_SUBJECT_ACCESS_REQUIREMENT + " WHERE "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + " IN (:" + SOURCE_SUBJECTS + ")" + " AND "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE + " = :" + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE
			+ " AND " + COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + " NOT IN (" + "SELECT DISTINCT "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID + " FROM " + TABLE_SUBJECT_ACCESS_REQUIREMENT + " WHERE "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID + " IN (:" + DEST_SUBJECTS + ")" + " AND "
			+ COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE + " = :" + COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE + ")";

	private static final RowMapper<DBOAccessRequirement> accessRequirementRowMapper = (new DBOAccessRequirement())
			.getTableMapping();
	private static final RowMapper<DBOSubjectAccessRequirement> subjectAccessRequirementRowMapper = (new DBOSubjectAccessRequirement())
			.getTableMapping();
	private static final RowMapper<DBOAccessRequirementRevision> revisionRowMapper = new DBOAccessRequirementRevision()
			.getTableMapping();

	/*
	 * This mapper can be used for the join of requirement and revision.
	 */
	private static final RowMapper<AccessRequirement> requirementMapper = new RowMapper<AccessRequirement>() {
		@Override
		public AccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAccessRequirement dboRequirement = accessRequirementRowMapper.mapRow(rs, rowNum);
			DBOAccessRequirementRevision dboRevision = revisionRowMapper.mapRow(rs, rowNum);
			return AccessRequirementUtils.copyDboToDto(dboRequirement, dboRevision);
		}
	};

	@WriteTransaction
	@Override
	public void delete(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), id);
		try {
			basicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, param);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("The access requirement with id " + id
					+ " cannot be deleted as it is referenced by another object.", e);
		}
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T create(T dto) {
		dto.setId(idGenerator.generateNewId(IdType.ACCESS_REQUIREMENT_ID));
		dto.setEtag(UUID.randomUUID().toString());
		dto.setVersionNumber(DEFAULT_VERSION);
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo, dboRevision);
		try {
			dbo = basicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			translateException(e, dto.getName());
		}
		basicDao.createNew(dboRevision);
		populateSubjectAccessRequirement(dbo.getId(), dto.getSubjectIds());
		return (T) get(dbo.getId().toString());
	}


	/**
	 * Attempt to translate the given exception.
	 * 
	 * @param e
	 * @param dto
	 */
	static void translateException(IllegalArgumentException e, String name) {
		if (e.getMessage().contains("AR_NAME")) {
			throw new NameConflictException(
					String.format("An AccessRequirement with the name: '%s' already exists", name), e);
		} else {
			throw e;
		}
	}

	/**
	 * Get the fully populated DTO for the given IDs.
	 * 
	 * @param requirementIds
	 * @return
	 */
	private List<AccessRequirement> getAccessRequirements(List<Long> requirementIds) {
		if (requirementIds.isEmpty()) {
			return new LinkedList<>();
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID.toLowerCase(), requirementIds);
		return namedJdbcTemplate.query(SELECT_CURRENT_REQUIREMENTS_BY_ID, param, requirementMapper);
	}

	private void populateSubjectAccessRequirement(Long accessRequirementId,
			List<RestrictableObjectDescriptor> rodList) {
		if (rodList == null || rodList.isEmpty()) {
			return;
		}
		List<DBOSubjectAccessRequirement> batch = AccessRequirementUtils
				.createBatchDBOSubjectAccessRequirement(accessRequirementId, rodList);
		if (batch.size() > 0) {
			basicDao.createBatch(batch);
		}
	}

	private void clearSubjectAccessRequirement(Long accessRequirementId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		param.addValue("bindingType", BindingType.MANUAL.name());
		namedJdbcTemplate.update(DELETE_SUBJECT_ACCESS_REQUIREMENTS_SQL, param);
	}

	@Override
	public AccessRequirement get(String id) throws NotFoundException {
		List<Long> ids = new LinkedList<>();
		ids.add(Long.parseLong(id));
		List<AccessRequirement> results = getAccessRequirements(ids);
		if (results.isEmpty()) {
			throw new NotFoundException("An access requirement with id " + id + " cannot be found.");
		}
		return results.get(0);
	}
	
	@Override
	public Optional<AccessRequirement> getVersion(String id, Long versionNumber) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource()
				.addValue(COL_ACCESS_REQUIREMENT_ID, id)
				.addValue(COL_ACCESS_REQUIREMENT_REVISION_NUMBER, versionNumber);

		try {
			AccessRequirement accessRequirement = namedJdbcTemplate.queryForObject(SELECT_REQUIREMENT_BY_ID_AND_VERSION, param, requirementMapper);
			return Optional.of(accessRequirement);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<RestrictableObjectDescriptor> getSubjects(long accessRequirementId, long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID, accessRequirementId);
		param.addValue(LIMIT_PARAM, limit);
		param.addValue(OFFSET_PARAM, offset);
		List<DBOSubjectAccessRequirement> nars = namedJdbcTemplate
				.query(GET_SUBJECT_ACCESS_REQUIREMENT_WITH_LIMIT_AND_OFFSET, param, subjectAccessRequirementRowMapper);
		return AccessRequirementUtils.copyDBOSubjectsToDTOSubjects(nars);
	}

	@MandatoryWriteTransaction
	@Override
	public AccessRequirementInfoForUpdate getForUpdate(String accessRequirementId) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, accessRequirementId);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_INFO_FOR_UPDATE_SQL, param,
					new RowMapper<AccessRequirementInfoForUpdate>() {

						@Override
						public AccessRequirementInfoForUpdate mapRow(ResultSet rs, int rowNum) throws SQLException {
							AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
							info.setAccessRequirementId(rs.getLong(COL_ACCESS_REQUIREMENT_ID));
							info.setEtag(rs.getString(COL_ACCESS_REQUIREMENT_ETAG));
							info.setCurrentVersion(rs.getLong(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER));
							info.setAccessType(ACCESS_TYPE.valueOf(rs.getString(COL_ACCESS_REQUIREMENT_ACCESS_TYPE)));
							info.setConcreteType(rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE));
							return info;
						}

					});
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(ACCESS_REQUIREMENT_DOES_NOT_EXIST, accessRequirementId));
		}
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T update(T dto) {

		DBOAccessRequirement toUpdate = new DBOAccessRequirement();
		DBOAccessRequirementRevision revision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDtoToDbo(dto, toUpdate, revision);

		// update the etag and version of the requirement
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, toUpdate.getId());
		param.addValue(COL_ACCESS_REQUIREMENT_ETAG, UUID.randomUUID().toString());
		param.addValue(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER, toUpdate.getCurrentRevNumber());
		param.addValue(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE, toUpdate.getConcreteType());
		param.addValue(COL_ACCESS_REQUIREMENT_NAME, toUpdate.getName());
		param.addValue(COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED, toUpdate.getIsTwoFaRequired());
		try {
			namedJdbcTemplate.update(UPDATE_ACCESS_REQUIREMENT_SQL, param);
		} catch (DataIntegrityViolationException e) {
			translateException(new IllegalArgumentException(e), dto.getName());
		}

		// Create the new revision.
		basicDao.createNew(revision);

		clearSubjectAccessRequirement(dto.getId());
		populateSubjectAccessRequirement(dto.getId(), dto.getSubjectIds());

		return (T) get(dto.getId().toString());
	}

	@Override
	public List<AccessRequirement> getAccessRequirementsForSubject(List<Long> subjectIds, RestrictableObjectType type,
			long limit, long offset) throws DatastoreException {
		List<AccessRequirement> dtos = new ArrayList<AccessRequirement>();
		if (subjectIds.isEmpty()) {
			return dtos;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIds);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		param.addValue(LIMIT_PARAM, limit);
		param.addValue(OFFSET_PARAM, offset);
		List<Long> ids = namedJdbcTemplate.queryForList(GET_ACCESS_REQUIREMENTS_IDS_PAGE_SQL, param, Long.class);
		return getAccessRequirements(ids);
	}

	@Override
	public String getConcreteType(String accessRequirementId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_CONCRETE_TYPE_SQL, String.class, accessRequirementId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(ACCESS_REQUIREMENT_DOES_NOT_EXIST, accessRequirementId));
		}
	}

	@Override
	public AccessRequirementStats getAccessRequirementStats(List<Long> subjectIds, RestrictableObjectType type) {
		ValidateArgument.requirement(subjectIds != null && !subjectIds.isEmpty(),
				"subjectIds must contain at least one ID.");
		ValidateArgument.required(type, "type");
		final AccessRequirementStats stats = new AccessRequirementStats();
		stats.setHasACT(false);
		stats.setHasToU(false);
		stats.setHasLock(false);
		final Set<String> requirementIdSet = new HashSet<String>();
		stats.setRequirementIdSet(requirementIdSet);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID, subjectIds);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		namedJdbcTemplate.query(SELECT_ACCESS_REQUIREMENT_STATS, param, new RowMapper<Void>() {

			@Override
			public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
				requirementIdSet.add(rs.getString(COL_ACCESS_REQUIREMENT_ID));
				String type = rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE);
				if (type.equals(TermsOfUseAccessRequirement.class.getName())
						|| type.equals(SelfSignAccessRequirement.class.getName())) {
					stats.setHasToU(true);
				} else if (type.equals(ACTAccessRequirement.class.getName())
						|| type.equals(ManagedACTAccessRequirement.class.getName())) {
					stats.setHasACT(true);
				} else if (type.equals(LockAccessRequirement.class.getName())) {
					stats.setHasLock(true);
				}
				return null;
			}
		});
		return stats;
	}

	@Override
	public List<String> getAccessRequirementDiff(List<Long> sourceSubjects, List<Long> destSubjects,
			RestrictableObjectType type) {
		ValidateArgument.required(type, "type");
		ValidateArgument.required(sourceSubjects, "sourceSubjects");
		ValidateArgument.required(destSubjects, "destSubjects");
		ValidateArgument.requirement(!sourceSubjects.isEmpty(), "Need at least one source subject.");
		ValidateArgument.requirement(!destSubjects.isEmpty(), "Need at least one destination subject.");

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SOURCE_SUBJECTS, sourceSubjects);
		param.addValue(DEST_SUBJECTS, destSubjects);
		param.addValue(COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE, type.name());
		List<String> ids = namedJdbcTemplate.queryForList(SELECT_ACCESS_REQUIREMENT_DIFF, param, String.class);
		return ids;
	}

	@MandatoryWriteTransaction
	@Override
	public AccessRequirement getAccessRequirementForUpdate(String accessRequirementId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACCESS_REQUIREMENT_ID, accessRequirementId);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, param, requirementMapper);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(ACCESS_REQUIREMENT_DOES_NOT_EXIST, accessRequirementId));
		}
	}

	@Override
	public Map<Long, String> getAccessRequirementNames(Set<Long> accessRequirementIds) {
		
		if (accessRequirementIds == null || accessRequirementIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String sql = "SELECT " + COL_ACCESS_REQUIREMENT_ID + ", " + COL_ACCESS_REQUIREMENT_NAME
			+ " FROM " + TABLE_ACCESS_REQUIREMENT
			+ " WHERE " + COL_ACCESS_REQUIREMENT_ID + " IN (:" + COL_ACCESS_REQUIREMENT_ID + ")";
				
		return namedJdbcTemplate.query(sql, Map.of(COL_ACCESS_REQUIREMENT_ID, accessRequirementIds), rs -> {
			Map<Long, String> namesMap = new HashMap<>(accessRequirementIds.size());
			while (rs.next()) {
				namesMap.put(rs.getLong(COL_ACCESS_REQUIREMENT_ID), rs.getString(COL_ACCESS_REQUIREMENT_NAME));
			}
			return namesMap;
		});
	}
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_ACCESS_REQUIREMENT+ " WHERE ID > ?", AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID);
	}

	@WriteTransaction
	@Override
	public void mapAccessRequirmentsToProject(Long[] arIds, Long projectId) {
		if(arIds == null || arIds.length <1) {
			return;
		}
		jdbcTemplate.batchUpdate("INSERT IGNORE INTO " + TABLE_ACCESS_REQUIREMENT_PROJECTS + " ("
				+ COL_ACCESS_REQUIREMENT_PROJECT_AR_ID + ", " + COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID + ") VALUES (?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						ps.setLong(1, arIds[i]);
						ps.setLong(2, projectId);
					}

					@Override
					public int getBatchSize() {
						return arIds.length;
					}
				});

	}
	
	@Override
	public Map<Long, List<Long>> getAccessRequirementProjectsMap(Set<Long> arIds) {
		if (arIds == null || arIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String sql = "SELECT " + COL_ACCESS_REQUIREMENT_PROJECT_AR_ID + ", " + COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID 
				+ " FROM " + TABLE_ACCESS_REQUIREMENT_PROJECTS 
				+ " WHERE " + COL_ACCESS_REQUIREMENT_PROJECT_AR_ID + " IN (" + String.join(",", Collections.nCopies(arIds.size(), "?")) + ")";
		
		return jdbcTemplate.query(sql, (ResultSet rs) -> {
			Map<Long, List<Long>> projectsMap = new HashMap<>(arIds.size());
			
			while(rs.next()) {
				Long id = rs.getLong(COL_ACCESS_REQUIREMENT_PROJECT_AR_ID);
				Long projectId = rs.getLong(COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID);
				
				List<Long> projects = projectsMap.get(id);
				
				if (projects == null) {
					projectsMap.put(id, projects = new ArrayList<>());
				}
				
				projects.add(projectId);
			}
			
			return projectsMap;
		}, arIds.toArray());
	}
	
	@Override
	public List<AccessRequirement> searchAccessRequirements(List<AccessRequirementSearchSort> sort, String nameContains, List<Long> arIds, String reviewerId,
			Long projectId, ACCESS_TYPE accessType, long limit, long offset) {
		ValidateArgument.requiredNotEmpty(sort, "sort");
		
		String sqlQuery = "SELECT AR.*, R.*" + " FROM " + TABLE_ACCESS_REQUIREMENT
				+ " AR JOIN " + TABLE_ACCESS_REQUIREMENT_REVISION + " R ON (AR." + COL_ACCESS_REQUIREMENT_ID
				+ " = R." + COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID + " AND AR." + COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER + " = R." + COL_ACCESS_REQUIREMENT_REVISION_NUMBER + ")";
		
		List<Object> queryParams = new ArrayList<>();
		
		if (projectId != null) {
			sqlQuery += " JOIN " + TABLE_ACCESS_REQUIREMENT_PROJECTS + " P ON AR." + COL_ACCESS_REQUIREMENT_ID + " = P." + COL_ACCESS_REQUIREMENT_PROJECT_AR_ID
				+ " AND P." + COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID + " = ?";
			queryParams.add(projectId);
		}
		
		if (reviewerId != null) {
			sqlQuery += " JOIN " + TABLE_ACCESS_CONTROL_LIST + " A ON AR." + COL_ACCESS_REQUIREMENT_ID + " = A." + COL_ACL_OWNER_ID + " AND A." + COL_ACL_OWNER_TYPE + " = '" + ObjectType.ACCESS_REQUIREMENT.name() + "'"
				+ " JOIN " + TABLE_RESOURCE_ACCESS + " RA ON RA." + COL_RESOURCE_ACCESS_OWNER + " = A." + COL_ACL_ID + " AND RA." + COL_RESOURCE_ACCESS_GROUP_ID + " = ?"
				+ " JOIN " + TABLE_RESOURCE_ACCESS_TYPE + " AT ON RA." + COL_RESOURCE_ACCESS_ID + " = AT." + COL_RESOURCE_ACCESS_TYPE_ID + " AND AT." + COL_RESOURCE_ACCESS_TYPE_ELEMENT + " = '" + ACCESS_TYPE.REVIEW_SUBMISSIONS + "'";
			queryParams.add(reviewerId);
		}
		
		List<String> filters = new ArrayList<>();
		
		if (nameContains != null) {
			filters.add("AR." + COL_ACCESS_REQUIREMENT_NAME + " LIKE ?");
			queryParams.add("%" + nameContains + "%");
		}
		
		if (accessType != null) {
			filters.add("AR." + COL_ACCESS_REQUIREMENT_ACCESS_TYPE + " = ?");
			queryParams.add(accessType.name());
		}
		
		if (arIds != null && !arIds.isEmpty()) {
			filters.add("AR." + COL_ACCESS_REQUIREMENT_ID + " IN (" + String.join(",", Collections.nCopies(arIds.size(), "?")) + ")");
			queryParams.addAll(arIds);
		}
		
		if (!filters.isEmpty()) {
			sqlQuery += " WHERE " + String.join(" AND ", filters);
		}
		
		sqlQuery += " ORDER BY " + String.join(",", sort.stream().map(s-> {
			ValidateArgument.required(s.getField(), "sort.field");
			return s.getField().name() + (s.getDirection() == null ? "" : " " + s.getDirection().name());
		}).collect(Collectors.toList()));
		
		sqlQuery += " LIMIT ? OFFSET ?";
			
		queryParams.add(limit);
		queryParams.add(offset);
		
		return jdbcTemplate.query(sqlQuery, requirementMapper, queryParams.toArray());
	}
	
	/**
	 * Validate the provided subject and return the subject ID as a {@link Long}.
	 * @param subject
	 * @return
	 */
	static Long validateSubject(RestrictableObjectDescriptor subject) {
		ValidateArgument.required(subject, "subject");
		ValidateArgument.required(subject.getId(), "subject.id");
		ValidateArgument.required(subject.getType(), "subject.type");
		return KeyFactory.stringToKey(subject.getId());
	}

	@Override
	public List<Long> getDynamicallyBoundAccessRequirementIdsForSubject(RestrictableObjectDescriptor subject) {
		Long subjectId = validateSubject(subject);
		return jdbcTemplate.queryForList(
				"SELECT REQUIREMENT_ID FROM NODE_ACCESS_REQUIREMENT WHERE SUBJECT_ID = ? AND SUBJECT_TYPE = ? AND BINDING_TYPE = ? ",
				Long.class, subjectId, subject.getType().name(), BindingType.DYNAMIC.name());
	}
	
	@WriteTransaction
	@Override
	public void addDynamicallyBoundAccessRequirmentsToSubject(RestrictableObjectDescriptor subject, List<Long> arIds) {
		Long subjectId = validateSubject(subject);
		ValidateArgument.required(arIds, "arIds");
		if (arIds.isEmpty()) {
			return;
		}
		try {
			jdbcTemplate.batchUpdate(
					"INSERT INTO NODE_ACCESS_REQUIREMENT"
							+ " (SUBJECT_ID, SUBJECT_TYPE, REQUIREMENT_ID, BINDING_TYPE) VALUES (?,?,?,?)",
					new BatchPreparedStatementSetter() {

						@Override
						public void setValues(PreparedStatement ps, int i) throws SQLException {
							ps.setLong(1, subjectId);
							ps.setString(2, subject.getType().name());
							ps.setLong(3, arIds.get(i));
							ps.setString(4, BindingType.DYNAMIC.name());
						}

						@Override
						public int getBatchSize() {
							return arIds.size();
						}
					});

			// needed to ensure that this change migrates.
			updateAccessRequirmentEtags(arIds);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(
					"One or more access requirement is already dynamically bound to this subject.", e);
		} catch (DataIntegrityViolationException e) {
			if (e.getMessage().contains("`SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID_FK` ")) {
				throw new NotFoundException(String.format(
						"Cannot bind access requirements to: '%s' because one or more of the provided access requirement IDs does not exist: '%s'",
						subject.getId(), arIds));
			} else {
				throw e;
			}
		}
	}

	@WriteTransaction
	@Override
	public void removeDynamicallyBoundAccessRequirementsFromSubject(RestrictableObjectDescriptor subject,
			List<Long> arIds) {
		Long subjectId = validateSubject(subject);
		ValidateArgument.required(arIds, "arIds");
		if (arIds.isEmpty()) {
			return;
		}
		jdbcTemplate.batchUpdate(
				"DELETE FROM NODE_ACCESS_REQUIREMENT "
				+ "WHERE SUBJECT_ID = ? AND SUBJECT_TYPE = ? AND REQUIREMENT_ID = ? AND BINDING_TYPE = ?",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						ps.setLong(1, subjectId);
						ps.setString(2, subject.getType().name());
						ps.setLong(3, arIds.get(i));
						ps.setString(4, BindingType.DYNAMIC.name());
					}

					@Override
					public int getBatchSize() {
						return arIds.size();
					}
				});
		// needed to ensure that this change migrates.
		updateAccessRequirmentEtags(arIds);
	}

	private void updateAccessRequirmentEtags(List<Long> arIds) {
		ValidateArgument.required(arIds, "arIds");
		if (arIds.isEmpty()) {
			return;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("ids", arIds);
		// Note: The rows must be updated in order to prevent deadlock with other threads.
		namedJdbcTemplate.update("UPDATE ACCESS_REQUIREMENT SET ETAG = UUID() WHERE ID IN (:ids) ORDER BY ID", param);
	}

	@WriteTransaction
	@Override
	public void bootstrap() {
		String creator = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		Date date = Date.from(OffsetDateTime.of(2022, 8, 31, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
		LockAccessRequirement lock = new LockAccessRequirement()
				.setId(AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID)
				.setVersionNumber(1L)
				.setAccessType(ACCESS_TYPE.DOWNLOAD)
				.setCreatedBy(creator)
				.setCreatedOn(date)
				.setModifiedBy(creator)
				.setModifiedOn(date)
				.setEtag("start")
				.setName("Invalid Annotations Lock");
		bootstrap(lock);
	}
	
	@Override
	@TemporaryCode(author = "Marco Marasca", comment = "Temp code used to backfill AR snapshots")
	public List<ChangeMessage> getMissingArChangeMessages(long limit) {
		String sql = "SELECT R.OWNER_ID, R.NUMBER, R.MODIFIED_ON, R.MODIFIED_BY"
				+ "	FROM ACCESS_REQUIREMENT_REVISION R"
				+ " LEFT JOIN CHANGES C ON (R.OWNER_ID = C.OBJECT_ID AND R.NUMBER = C.OBJECT_VERSION AND C.OBJECT_TYPE = 'ACCESS_REQUIREMENT')"
				+ "	WHERE C.OBJECT_ID IS NULL"
				+ " ORDER BY R.OWNER_ID, R.NUMBER"
				+ " LIMIT :limit";
		
		return namedJdbcTemplate.query(sql, Map.of("limit", limit), (rs, rowNumber) -> {
			Long id = rs.getLong("OWNER_ID");
			Long version = rs.getLong("NUMBER");
			Long modifiedOn = rs.getLong("MODIFIED_ON");
			Long modifiedBy = rs.getLong("MODIFIED_BY");
			
			ChangeMessage message = new ChangeMessage()
				.setObjectId(id.toString())
				.setObjectVersion(version)
				.setObjectType(ObjectType.ACCESS_REQUIREMENT)
				.setUserId(modifiedBy)				
				.setTimestamp(new Date(modifiedOn));
			
			// The invalid annotation ar id starts from 1 instead of the default 0
			if (id.equals(INVALID_ANNOTATIONS_LOCK_ID) || DEFAULT_VERSION.equals(version)) {
				message.setChangeType(ChangeType.CREATE);
			} else {
				message.setChangeType(ChangeType.UPDATE);
			}
			
			return message;
		});
	}
	
	/**
	 * Bootstrapping inserts the AR with "insert ignore" accepting the provided ID.
	 * @param dto
	 */
	void bootstrap(AccessRequirement dto) {
		DBOAccessRequirement dbo = new DBOAccessRequirement();
		DBOAccessRequirementRevision dboRevision = new DBOAccessRequirementRevision();
		AccessRequirementUtils.copyDtoToDbo(dto, dbo, dboRevision);
		basicDao.insertIgnore(dbo);
		basicDao.insertIgnore(dboRevision);
	}

}
