package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_SETTING;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectSetting;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Lists;

public class DBOProjectSettingsDAOImpl implements ProjectSettingsDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private TransactionalMessenger transactionalMessenger;

	private static final String TYPE_PARAM_NAME = "type_param";
	private static final String PARENT_IDS_PARAM_NAME = "parent_ids_param";

	private static final String SELECT_SETTING = "SELECT * FROM " + TABLE_PROJECT_SETTING + " WHERE " + COL_PROJECT_SETTING_PROJECT_ID
			+ " = ? and " + COL_PROJECT_SETTING_TYPE + " = ?";
	private static final String SELECT_SETTINGS_BY_PROJECT = "SELECT * FROM " + TABLE_PROJECT_SETTING + " WHERE "
			+ COL_PROJECT_SETTING_PROJECT_ID + " = ?";
	private static final String SELECT_SETTINGS_BY_TYPE = "SELECT * FROM " + TABLE_PROJECT_SETTING + " WHERE " + COL_PROJECT_SETTING_TYPE
			+ " = ?";

	private static final String SELECT_SETTING_FROM_PARENTS = "SELECT * FROM " + TABLE_PROJECT_SETTING + " WHERE "
			+ COL_PROJECT_SETTING_PROJECT_ID + " IN ( :" + PARENT_IDS_PARAM_NAME + " ) and " + COL_PROJECT_SETTING_TYPE + " = :"
			+ TYPE_PARAM_NAME + " ORDER BY FIELD( " + COL_PROJECT_SETTING_PROJECT_ID + ", :" + PARENT_IDS_PARAM_NAME + " ) LIMIT 1";

	private static final RowMapper<DBOProjectSetting> projectSettingRowMapper = (new DBOProjectSetting()).getTableMapping();

	public DBOProjectSettingsDAOImpl(){}

	// for test only
	public DBOProjectSettingsDAOImpl(DBOBasicDao mockBasicDao,
			IdGenerator mockIdGenerator,
			TransactionalMessenger mockTransactionalMessenger) {
		this.basicDao = mockBasicDao;
		this.idGenerator = mockIdGenerator;
		this.transactionalMessenger = mockTransactionalMessenger;
	}

	@WriteTransaction
	@Override
	public String create(ProjectSetting dto) throws DatastoreException, InvalidModelException {
		DBOProjectSetting dbo = new DBOProjectSetting();
		copyDtoToDbo(dto, dbo);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.PROJECT_SETTINGS_ID));
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		try{
			dbo = basicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			// we want to catch the common case of an existing setting and tell the user nicely about that
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException("A project setting of type '" + dto.getSettingsType().name() + "' for project "
						+ dto.getProjectId() + " already exists.");
			} else {
				throw e;
			}
		}
		String projectSettingsId = dbo.getId().toString();
		transactionalMessenger.sendMessageAfterCommit(projectSettingsId, ObjectType.PROJECT_SETTING, dbo.getEtag(), ChangeType.CREATE);

		return projectSettingsId;
	}

	@Override
	public ProjectSetting get(String projectId, ProjectSettingsType type) throws DatastoreException {
		try {
			DBOProjectSetting projectSetting = jdbcTemplate.queryForObject(SELECT_SETTING, projectSettingRowMapper,
					KeyFactory.stringToKey(projectId), type.name());
			ProjectSetting dto = convertDboToDto(projectSetting);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			// not having a setting is normal
			return null;
		}
	}

	@Override
	public ProjectSetting get(List<Long> parentIds, ProjectSettingsType type) throws DatastoreException {
		try {
			MapSqlParameterSource parameterSource = new MapSqlParameterSource();
			parameterSource.addValue(TYPE_PARAM_NAME, type.name());
			parameterSource.addValue(PARENT_IDS_PARAM_NAME, Lists.reverse(parentIds));
			DBOProjectSetting projectSetting = namedParameterJdbcTemplate.queryForObject(SELECT_SETTING_FROM_PARENTS, parameterSource,
					projectSettingRowMapper);
			ProjectSetting dto = convertDboToDto(projectSetting);
			return dto;
		} catch (EmptyResultDataAccessException e) {
			// not having a setting is normal
			return null;
		}
	}

	@Override
	public ProjectSetting get(String id) throws DatastoreException, NotFoundException {
		DBOProjectSetting projectSetting = basicDao
				.getObjectByPrimaryKey(DBOProjectSetting.class, new SinglePrimaryKeySqlParameterSource(id));
		ProjectSetting dto = convertDboToDto(projectSetting);
		return dto;
	}

	@Override
	public List<ProjectSetting> getAllForProject(String projectId) throws DatastoreException, NotFoundException {
		List<DBOProjectSetting> projectSettings = jdbcTemplate.query(SELECT_SETTINGS_BY_PROJECT, projectSettingRowMapper,
				KeyFactory.stringToKey(projectId));
		List<ProjectSetting> result = Lists.newArrayListWithCapacity(projectSettings.size());
		for (DBOProjectSetting projectSetting : projectSettings) {
			result.add(convertDboToDto(projectSetting));
		}
		return result;
	}

	@Override
	public List<ProjectSetting> getByType(ProjectSettingsType projectSettingsType) throws DatastoreException, NotFoundException {
		List<DBOProjectSetting> projectSettings = jdbcTemplate.query(SELECT_SETTINGS_BY_TYPE, projectSettingRowMapper,
				projectSettingsType.name());
		List<ProjectSetting> result = Lists.newArrayListWithCapacity(projectSettings.size());
		for (DBOProjectSetting projectSetting : projectSettings) {
			result.add(convertDboToDto(projectSetting));
		}
		return result;
	}

	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		basicDao.deleteObjectByPrimaryKey(DBOProjectSetting.class, new SinglePrimaryKeySqlParameterSource(id));
		transactionalMessenger.sendMessageAfterCommit(id, ObjectType.PROJECT_SETTING, ChangeType.DELETE);
	}

	@WriteTransaction
	@Override
	public ProjectSetting update(ProjectSetting dto) throws DatastoreException, InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOProjectSetting dbo = basicDao.getObjectByPrimaryKey(DBOProjectSetting.class,
				new SinglePrimaryKeySqlParameterSource(dto.getId()));

		if (!dbo.getProjectId().equals(KeyFactory.stringToKey(dto.getProjectId()).longValue())) {
			throw new IllegalArgumentException(
					"You cannot change the project id with the update project settings call. Create a new project settings instead");
		}
		if (!dbo.getType().equals(dto.getSettingsType())) {
			throw new IllegalArgumentException(
					"You cannot change the settings type with the update project settings call. Create a new project settings instead");
		}

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.getEtag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException(
					"Project setting was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		dbo.setData(dto);
		// Update with a new e-tag
		dbo.setEtag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success)
			throw new DatastoreException("Unsuccessful updating project setting in database.");
		// re-get, so we don't clobber the object we put in the dbo directly with setData
		dbo = basicDao.getObjectByPrimaryKey(DBOProjectSetting.class, new SinglePrimaryKeySqlParameterSource(dto.getId()));
		transactionalMessenger.sendMessageAfterCommit(dbo.getId().toString(), ObjectType.PROJECT_SETTING, dbo.getEtag(), ChangeType.UPDATE);
		return convertDboToDto(dbo);
	}

	private static void copyDtoToDbo(ProjectSetting dto, DBOProjectSetting dbo) {
		if (dto.getProjectId() == null) {
			throw new InvalidModelException("projectId must be specified");
		}
		if (dto.getSettingsType() == null) {
			throw new InvalidModelException("settingsType must be specified");
		}
		dbo.setId(dto.getId() != null ? KeyFactory.stringToKey(dto.getId()) : null);
		dbo.setProjectId(KeyFactory.stringToKey(dto.getProjectId()));
		dbo.setType(dto.getSettingsType());
		dbo.setEtag(dto.getEtag());
		dbo.setData(dto);
	}

	private static ProjectSetting convertDboToDto(DBOProjectSetting dbo) {
		ProjectSetting dto = dbo.getData();
		dto.setId(dbo.getId().toString());
		dto.setProjectId(KeyFactory.keyToString(dbo.getProjectId()));
		dto.setSettingsType(dbo.getType());
		dto.setEtag(dbo.getEtag());
		return dto;
	}

}
