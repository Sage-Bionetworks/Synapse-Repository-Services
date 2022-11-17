package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_SNAPSHOT;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

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
public class TableSnapshotDaoImpl implements TableSnapshotDao {

	@Autowired
	IdGenerator idGenerator;
	@Autowired
	DBOBasicDao basicDao;
	@Autowired
	JdbcTemplate jdbcTemplate;

	private static RowMapper<DBOTableSnapshot> MAPPER = new DBOTableSnapshot().getTableMapping();

	@WriteTransaction
	@Override
	public TableSnapshot createSnapshot(TableSnapshot snapshot) {
		ValidateArgument.required(snapshot, "snapshot");
		ValidateArgument.required(snapshot.getTableId(), "snapshot.tableId");
		ValidateArgument.required(snapshot.getVersion(), "snapshot.version");
		ValidateArgument.required(snapshot.getCreatedBy(), "snapshot.createdBy");
		ValidateArgument.required(snapshot.getCreatedOn(), "snapshot.createdOn");
		ValidateArgument.required(snapshot.getBucket(), "snapshot.bucket");
		ValidateArgument.required(snapshot.getKey(), "snapshot.key");
		DBOTableSnapshot dbo = translate(snapshot);
		dbo.setSnapshotId(idGenerator.generateNewId(IdType.VIEW_SNAPSHOT_ID));
		try {
			return translate(basicDao.createNew(dbo));
		} catch (IllegalArgumentException e) {
			if(DuplicateKeyException.class.equals(e.getCause().getClass())) {
				throw new IllegalArgumentException("Snapshot already exists for: " + IdAndVersion.newBuilder()
				.setId(snapshot.getTableId()).setVersion(snapshot.getVersion()).build().toString(), e);
			}else {
				throw e;
			}
		}
	}

	@Override
	public Optional<TableSnapshot> getSnapshot(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(idAndVersion.getVersion().isPresent(), "version");
		try {
			TableSnapshot snapshot = translate(jdbcTemplate.queryForObject(
					"SELECT * FROM " + TABLE_TABLE_SNAPSHOT + " WHERE " + COL_TABLE_SNAPSHOT_TABLE_ID + " = ? AND "
							+ COL_TABLE_SNAPSHOT_VERSION + " = ?",
					MAPPER, idAndVersion.getId(), idAndVersion.getVersion().get()));
			return Optional.of(snapshot);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	static DBOTableSnapshot translate(TableSnapshot dto) {
		DBOTableSnapshot dbo = new DBOTableSnapshot();
		dbo.setSnapshotId(dto.getSnapshotId());
		dbo.setTableId(dto.getTableId());
		dbo.setVersion(dto.getVersion());
		dbo.setCreatedBy(dto.getCreatedBy());
		dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		dbo.setBucket(dto.getBucket());
		dbo.setKey(dto.getKey());
		return dbo;
	}

	static TableSnapshot translate(DBOTableSnapshot in) {
		return new TableSnapshot().withSnapshotId(in.getSnapshotId()).withTableId(in.getTableId())
				.withVersion(in.getVersion()).withCreatedBy(in.getCreatedBy())
				.withCreatedOn(new Date(in.getCreatedOn().getTime())).withBucket(in.getBucket()).withKey(in.getKey());
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_TABLE_SNAPSHOT + " WHERE " + COL_TABLE_SNAPSHOT_ID + " > 0");
	}

	@Override
	public long getSnapshotId(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.requirement(idAndVersion.getVersion().isPresent(), "version");
		try {
			return jdbcTemplate.queryForObject(
					"SELECT "+COL_TABLE_SNAPSHOT_ID+" FROM " + TABLE_TABLE_SNAPSHOT + " WHERE " + COL_TABLE_SNAPSHOT_TABLE_ID + " = ? AND "
							+ COL_TABLE_SNAPSHOT_VERSION + " = ?",
					Long.class, idAndVersion.getId(), idAndVersion.getVersion().get());
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Snapshot not found for: " + idAndVersion.toString(), e);
		}
	}

}
