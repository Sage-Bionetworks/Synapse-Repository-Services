package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UPLOAD_DESTINATION_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UPLOAD_DESTINATION_LOCATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;

public class DBOUploadDestinationLocationDAOImpl implements UploadDestinationLocationDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final String UPLOAD_DESTINATION_LOCATION_IDS_PARAM = "udl_ids";

	private static final String SELECT_UPLOAD_DESTINATION_LOCATIONS = "SELECT " + COL_UPLOAD_DESTINATION_LOCATION_ID + ", "
			+ COL_UPLOAD_DESTINATION_LOCATION_DESCRIPTION + " FROM " + TABLE_UPLOAD_DESTINATION_LOCATION + " WHERE "
			+ COL_UPLOAD_DESTINATION_LOCATION_ID + " IN (:" + UPLOAD_DESTINATION_LOCATION_IDS_PARAM + ")";

	private static final RowMapper<DBOUploadDestinationLocation> uploadDestinationLocationRowMapper = (new DBOUploadDestinationLocation())
			.getTableMapping();

	private static final Function<DBOUploadDestinationLocation, UploadDestinationLocationSetting> CONVERT_DBO_TO_UPLOAD_DESTINATION_LOCATION = new Function<DBOUploadDestinationLocation, UploadDestinationLocationSetting>() {
		@Override
		public UploadDestinationLocationSetting apply(DBOUploadDestinationLocation dbo) {
			UploadDestinationLocationSetting setting = dbo.getData();
			setting.setUploadId(dbo.getId());
			setting.setDescription(dbo.getDescription());
			setting.setEtag(dbo.getEtag());
			setting.setCreatedBy(dbo.getCreatedBy());
			setting.setCreatedOn(dbo.getCreatedOn());
			return setting;
		}
	};
	private static final Function<UploadDestinationLocationSetting, DBOUploadDestinationLocation> CONVERT_UPLOAD_DESTINATION_LOCATION_TO_DBO = new Function<UploadDestinationLocationSetting, DBOUploadDestinationLocation>() {
		@Override
		public DBOUploadDestinationLocation apply(UploadDestinationLocationSetting setting) {
			DBOUploadDestinationLocation dbo = new DBOUploadDestinationLocation();
			dbo.setDescription(setting.getDescription());
			dbo.setEtag(setting.getEtag());
			dbo.setData(setting);
			dbo.setCreatedBy(setting.getCreatedBy());
			dbo.setCreatedOn(setting.getCreatedOn());
			return dbo;
		}
	};

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long create(UploadDestinationLocationSetting dto) {
		DBOUploadDestinationLocation dbo = CONVERT_UPLOAD_DESTINATION_LOCATION_TO_DBO.apply(dto);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId());
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		dbo = basicDao.createNew(dbo);
		return dbo.getId();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends UploadDestinationLocationSetting> T update(T dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		try {
			DBOUploadDestinationLocation dbo = basicDao.getObjectByPrimaryKey(DBOUploadDestinationLocation.class,
					new SinglePrimaryKeySqlParameterSource(dto.getUploadId()));

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
			dbo = basicDao.getObjectByPrimaryKey(DBOUploadDestinationLocation.class,
					new SinglePrimaryKeySqlParameterSource(dto.getUploadId()));
			return (T) CONVERT_DBO_TO_UPLOAD_DESTINATION_LOCATION.apply(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
	}

	@Override
	public UploadDestinationLocationSetting get(Long id) throws DatastoreException, NotFoundException {
		DBOUploadDestinationLocation dbo = basicDao.getObjectByPrimaryKey(DBOUploadDestinationLocation.class,
				new SinglePrimaryKeySqlParameterSource(id));
		return CONVERT_DBO_TO_UPLOAD_DESTINATION_LOCATION.apply(dbo);
	}

	@Override
	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> locations) {
		return namedParameterJdbcTemplate.query(SELECT_UPLOAD_DESTINATION_LOCATIONS,
				Collections.singletonMap(UPLOAD_DESTINATION_LOCATION_IDS_PARAM, locations), new RowMapper<UploadDestinationLocation>() {
					@Override
					public UploadDestinationLocation mapRow(ResultSet rs, int rowNum) throws SQLException {
						UploadDestinationLocation location = new UploadDestinationLocation();
						location.setUploadId(rs.getLong(COL_UPLOAD_DESTINATION_LOCATION_ID));
						location.setDescription(rs.getString(COL_UPLOAD_DESTINATION_LOCATION_DESCRIPTION));
						return location;
					}
				});
	}

	// temporary for migration
	@Override
	public List<UploadDestinationLocationSetting> getAllUploadDestinationLocationSettings() {
		List<DBOUploadDestinationLocation> result = jdbcTemplate.query("select * from " + TABLE_UPLOAD_DESTINATION_LOCATION,
				new DBOUploadDestinationLocation().getTableMapping());
		return Transform.toList(result, CONVERT_DBO_TO_UPLOAD_DESTINATION_LOCATION);
	}
}
