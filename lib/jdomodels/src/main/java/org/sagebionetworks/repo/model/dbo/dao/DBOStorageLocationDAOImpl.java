package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_UPLOAD_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class DBOStorageLocationDAOImpl implements StorageLocationDAO, InitializingBean {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	public static final Long DEFAULT_STORAGE_LOCATION_ID = 1L;

	private static final String STORAGE_LOCATION_IDS_PARAM = "udl_ids";

	private static final String SELECT_STORAGE_LOCATIONS = "SELECT " + COL_STORAGE_LOCATION_ID + ", " + COL_STORAGE_LOCATION_DESCRIPTION
			+ ", " + COL_STORAGE_LOCATION_UPLOAD_TYPE + " FROM " + TABLE_STORAGE_LOCATION + " WHERE " + COL_STORAGE_LOCATION_ID + " IN (:"
			+ STORAGE_LOCATION_IDS_PARAM + ")";
	private static final String SELECT_STORAGE_LOCATIONS_BY_OWNER = "SELECT * FROM " + TABLE_STORAGE_LOCATION + " WHERE "
			+ COL_STORAGE_LOCATION_CREATED_BY + " = ?";

	private static final RowMapper<DBOStorageLocation> StorageLocationRowMapper = (new DBOStorageLocation()).getTableMapping();

	public static UploadDestination getDefaultUploadDestination() {
		S3UploadDestination defaultUploadDestination = new S3UploadDestination();
		defaultUploadDestination.setStorageLocationId(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
		defaultUploadDestination.setUploadType(UploadType.S3);
		return defaultUploadDestination;
	}

	private static StorageLocationSetting getDefaultStorageLocationSetting() {
		StorageLocationSetting storageLocationSetting = new S3StorageLocationSetting();
		storageLocationSetting.setStorageLocationId(DEFAULT_STORAGE_LOCATION_ID);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		storageLocationSetting.setCreatedOn(new Date());
		return storageLocationSetting;
	}

	private static final Function<DBOStorageLocation, StorageLocationSetting> CONVERT_DBO_TO_STORAGE_LOCATION = new Function<DBOStorageLocation, StorageLocationSetting>() {
		@Override
		public StorageLocationSetting apply(DBOStorageLocation dbo) {
			StorageLocationSetting setting = dbo.getData();
			setting.setStorageLocationId(dbo.getId());
			setting.setDescription(dbo.getDescription());
			setting.setUploadType(dbo.getUploadType());
			setting.setEtag(dbo.getEtag());
			setting.setCreatedBy(dbo.getCreatedBy());
			setting.setCreatedOn(dbo.getCreatedOn());
			return setting;
		}
	};
	private static final Function<StorageLocationSetting, DBOStorageLocation> CONVERT_STORAGE_LOCATION_TO_DBO = new Function<StorageLocationSetting, DBOStorageLocation>() {
		@Override
		public DBOStorageLocation apply(StorageLocationSetting setting) {
			DBOStorageLocation dbo = new DBOStorageLocation();
			dbo.setId(setting.getStorageLocationId());
			dbo.setDescription(setting.getDescription());
			dbo.setUploadType(setting.getUploadType());
			dbo.setEtag(setting.getEtag());
			dbo.setData(setting);
			dbo.setCreatedBy(setting.getCreatedBy());
			dbo.setCreatedOn(setting.getCreatedOn());
			return dbo;
		}
	};

	@Override
	public void afterPropertiesSet() throws Exception {
		SinglePrimaryKeySqlParameterSource params = new SinglePrimaryKeySqlParameterSource(
				DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
		if (basicDao.getObjectByPrimaryKeyIfExists(DBOStorageLocation.class, params) == null) {
			try {
				// make sure we skip the first couple of IDs
				idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID);
				idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID);
				S3StorageLocationSetting defaultStorageLocationSetting = (S3StorageLocationSetting) DBOStorageLocationDAOImpl
						.getDefaultStorageLocationSetting();
				defaultStorageLocationSetting.setCreatedOn(new Date());
				defaultStorageLocationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
				defaultStorageLocationSetting.setStorageLocationId(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
				create(defaultStorageLocationSetting);
			} catch (DuplicateKeyException e2) {
				// someone else got there first
			} catch (IllegalArgumentException e2) {
				if (!(e2.getCause() instanceof DuplicateKeyException)) {
					throw e2;
				}
			}
		}
	}

	@WriteTransaction
	@Override
	public Long create(StorageLocationSetting dto) {
		DBOStorageLocation dbo = CONVERT_STORAGE_LOCATION_TO_DBO.apply(dto);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID));
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		dbo = basicDao.createNew(dbo);
		return dbo.getId();
	}

	@SuppressWarnings("unchecked")
	@WriteTransaction
	@Override
	public <T extends StorageLocationSetting> T update(T dto) throws DatastoreException, InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOStorageLocation dbo = basicDao.getObjectByPrimaryKey(DBOStorageLocation.class,
				new SinglePrimaryKeySqlParameterSource(dto.getStorageLocationId()));

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.getEtag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException(
					"Project setting was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		dbo.setDescription(dto.getDescription());
		dbo.setData(dto);
		// Update with a new e-tag
		dbo.setEtag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success)
			throw new DatastoreException("Unsuccessful updating project setting in database.");

		// re-get, so we don't clobber the object we put in the dbo directly with setData
		dbo = basicDao.getObjectByPrimaryKey(DBOStorageLocation.class,
				new SinglePrimaryKeySqlParameterSource(dto.getStorageLocationId()));
		return (T) CONVERT_DBO_TO_STORAGE_LOCATION.apply(dbo);
	}

	@Override
	public StorageLocationSetting get(Long storageLocationId) throws DatastoreException, NotFoundException {
		if (storageLocationId == null || DEFAULT_STORAGE_LOCATION_ID.equals(storageLocationId)) {
			return getDefaultStorageLocationSetting();
		}

		DBOStorageLocation dbo = basicDao.getObjectByPrimaryKey(DBOStorageLocation.class, new SinglePrimaryKeySqlParameterSource(
				storageLocationId));
		return CONVERT_DBO_TO_STORAGE_LOCATION.apply(dbo);
	}

	@Override
	public List<StorageLocationSetting> getByOwner(Long id) throws DatastoreException, NotFoundException {
		List<DBOStorageLocation> dboStorageLocations = jdbcTemplate.query(SELECT_STORAGE_LOCATIONS_BY_OWNER, StorageLocationRowMapper, id);
		return Lists.newArrayList(Lists.transform(dboStorageLocations, CONVERT_DBO_TO_STORAGE_LOCATION));
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> locations) throws DatastoreException, NotFoundException {
		return namedParameterJdbcTemplate.query(SELECT_STORAGE_LOCATIONS, Collections.singletonMap(STORAGE_LOCATION_IDS_PARAM, locations),
				new RowMapper<UploadDestinationLocation>() {
					@Override
					public UploadDestinationLocation mapRow(ResultSet rs, int rowNum) throws SQLException {
						UploadDestinationLocation location = new UploadDestinationLocation();
						location.setStorageLocationId(rs.getLong(COL_STORAGE_LOCATION_ID));
						location.setDescription(rs.getString(COL_STORAGE_LOCATION_DESCRIPTION));
						location.setUploadType(UploadType.valueOf(rs.getString(COL_STORAGE_LOCATION_UPLOAD_TYPE)));
						return location;
					}
				});
	}

	// temporary for migration
	@Override
	public List<StorageLocationSetting> getAllStorageLocationSettings() {
		List<DBOStorageLocation> result = jdbcTemplate.query("select * from " + TABLE_STORAGE_LOCATION,
				new DBOStorageLocation().getTableMapping());
		return Transform.toList(result, CONVERT_DBO_TO_STORAGE_LOCATION);
	}
}
