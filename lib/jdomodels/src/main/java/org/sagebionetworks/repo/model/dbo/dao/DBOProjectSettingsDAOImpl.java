package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_SETTING;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectSetting;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.sagebionetworks.repo.transactions.WriteTransaction;

import com.google.common.collect.Lists;

public class DBOProjectSettingsDAOImpl implements ProjectSettingsDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final String SELECT_SETTING = "SELECT * FROM " + TABLE_PROJECT_SETTING + " WHERE " + COL_PROJECT_SETTING_PROJECT_ID
			+ " = ? and " + COL_PROJECT_SETTING_TYPE + " = ?";
	private static final String SELECT_SETTINGS_BY_PROJECT = "SELECT * FROM " + TABLE_PROJECT_SETTING + " WHERE "
			+ COL_PROJECT_SETTING_PROJECT_ID + " = ?";

	private static final RowMapper<DBOProjectSetting> projectSettingRowMapper = (new DBOProjectSetting()).getTableMapping();

	@WriteTransaction
	@Override
	public String create(ProjectSetting dto) throws DatastoreException, InvalidModelException {
		DBOProjectSetting dbo = new DBOProjectSetting();
		copyDtoToDbo(dto, dbo);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId());
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		dbo = basicDao.createNew(dbo);
		return dbo.getId().toString();
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

	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		basicDao.deleteObjectByPrimaryKey(DBOProjectSetting.class, new SinglePrimaryKeySqlParameterSource(id));
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
