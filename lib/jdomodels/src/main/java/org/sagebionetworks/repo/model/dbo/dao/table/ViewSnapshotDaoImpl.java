package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SNAPSHOT;

import java.sql.Timestamp;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.apigatewayv2.model.NotFoundException;

@Repository
public class ViewSnapshotDaoImpl implements ViewSnapshotDao {
	
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	DBOBasicDao basicDao;
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	private static RowMapper<DBOViewSnapshot> MAPPER = new DBOViewSnapshot().getTableMapping();

	@Override
	public ViewSnapshot createSnapshot(ViewSnapshot snapshot) {
		DBOViewSnapshot dbo = translate(snapshot);
		dbo.setSnapshotId(idGenerator.generateNewId(IdType.VIEW_SNAPSHOT_ID));
		return translate(basicDao.createNew(dbo));
	}
	
	@Override
	public ViewSnapshot getSnapshot(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(idAndVersion.getVersion().isPresent(), "version");
		try {
			return translate(jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_VIEW_SNAPSHOT + " WHERE " + COL_VIEW_SNAPSHOT_VIEW_ID + " = ? AND "
							+ COL_VIEW_SNAPSHOT_VERSION + " = ?",
					MAPPER, idAndVersion.getId(), idAndVersion.getVersion().get()));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Snapshot not found for: "+idAndVersion.toString());
		}
	}
	
	static DBOViewSnapshot translate(ViewSnapshot dto) {
		DBOViewSnapshot dbo = new DBOViewSnapshot();
		dbo.setSnapshotId(dto.getSnapshotId());
		dbo.setViewId(dto.getViewId());
		dbo.setVersion(dto.getVersion());
		dbo.setCreatedBy(dto.getCreatedBy());
		dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		dbo.setBucket(dto.getBucket());
		dbo.setKey(dto.getKey());
		return dbo;
	}
	
	static ViewSnapshot translate(DBOViewSnapshot in) {
		return new ViewSnapshot()
				.withSnapshotId(in.getSnapshotId())
				.withViewId(in.getViewId())
				.withVersion(in.getVersion())
				.withCreatedBy(in.getCreatedBy())
				.withCreatedOn(in.getCreatedOn())
				.withBucket(in.getBucket())
				.withKey(in.getKey());
	}

}
