package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SNAPSHOT_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SNAPSHOT;

import java.sql.Timestamp;
import java.util.Date;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ViewSnapshotDaoImpl implements ViewSnapshotDao {

	@Autowired
	IdGenerator idGenerator;
	@Autowired
	DBOBasicDao basicDao;
	@Autowired
	JdbcTemplate jdbcTemplate;

	private static RowMapper<DBOViewSnapshot> MAPPER = new DBOViewSnapshot().getTableMapping();

	@WriteTransaction
	@Override
	public ViewSnapshot createSnapshot(ViewSnapshot snapshot) {
		ValidateArgument.required(snapshot, "snapshot");
		ValidateArgument.required(snapshot.getViewId(), "snapshot.viewId");
		ValidateArgument.required(snapshot.getVersion(), "snapshot.version");
		ValidateArgument.required(snapshot.getCreatedBy(), "snapshot.createdBy");
		ValidateArgument.required(snapshot.getCreatedOn(), "snapshot.createdOn");
		ValidateArgument.required(snapshot.getBucket(), "snapshot.bucket");
		ValidateArgument.required(snapshot.getKey(), "snapshot.key");
		DBOViewSnapshot dbo = translate(snapshot);
		dbo.setSnapshotId(idGenerator.generateNewId(IdType.VIEW_SNAPSHOT_ID));
		try {
			return translate(basicDao.createNew(dbo));
		} catch (IllegalArgumentException e) {
			if(DuplicateKeyException.class.equals(e.getCause().getClass())) {
				throw new IllegalArgumentException("Snapshot already exists for: " + IdAndVersion.newBuilder()
				.setId(snapshot.getViewId()).setVersion(snapshot.getVersion()).build().toString(), e);
			}else {
				throw e;
			}
		}
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
			throw new NotFoundException("Snapshot not found for: " + idAndVersion.toString(), e);
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
		return new ViewSnapshot().withSnapshotId(in.getSnapshotId()).withViewId(in.getViewId())
				.withVersion(in.getVersion()).withCreatedBy(in.getCreatedBy())
				.withCreatedOn(new Date(in.getCreatedOn().getTime())).withBucket(in.getBucket()).withKey(in.getKey());
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_VIEW_SNAPSHOT + " WHERE " + COL_VIEW_SNAPSHOT_ID + " > 0");
	}

	@Override
	public long getSnapshotId(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.requirement(idAndVersion.getVersion().isPresent(), "version");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT "+COL_VIEW_SNAPSHOT_ID+" FROM " + TABLE_VIEW_SNAPSHOT + " WHERE " + COL_VIEW_SNAPSHOT_VIEW_ID + " = ? AND "
							+ COL_VIEW_SNAPSHOT_VERSION + " = ?",
					Long.class, idAndVersion.getId(), idAndVersion.getVersion().get());
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Snapshot not found for: " + idAndVersion.toString(), e);
		}
	}

}
