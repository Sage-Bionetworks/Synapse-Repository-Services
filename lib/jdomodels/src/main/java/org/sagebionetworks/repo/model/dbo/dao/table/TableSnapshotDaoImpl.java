package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SNAPSHOT_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
	
	@Override
	public Optional<TableSnapshot> getMostRecentTableSnapshot(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		try {
			List<Object> args = new ArrayList<>();
			
			StringBuilder sql = new StringBuilder()
				.append("SELECT S.* FROM ").append(TABLE_TABLE_SNAPSHOT).append(" S") 
				// Make sure to join on the table transaction version tables so that we know the version still exists
				.append(" JOIN ").append(TABLE_TABLE_TRANSACTION).append(" T ON S.").append(COL_TABLE_SNAPSHOT_TABLE_ID).append(" = T.").append(COL_TABLE_TRX_TABLE_ID)
				.append(" JOIN ").append(TABLE_TABLE_TRX_TO_VERSION).append(" V ON T.").append(COL_TABLE_TRX_ID).append(" = V.").append(COL_TABLE_TRX_TO_VER_TRX_ID).append(" AND S.").append(COL_TABLE_SNAPSHOT_VERSION).append(" = V.").append(COL_TABLE_TRX_TO_VER_VER_NUM)
				.append(" WHERE S.").append(COL_TABLE_SNAPSHOT_TABLE_ID).append(" = ?");
			
			args.add(idAndVersion.getId());

			idAndVersion.getVersion().ifPresent( version -> {
				sql.append(" AND S.").append(COL_TABLE_SNAPSHOT_VERSION).append(" <= ?");
				args.add(version);
			});
			
			sql.append(" ORDER BY S.").append(COL_TABLE_SNAPSHOT_TABLE_ID).append(", ").append(COL_TABLE_SNAPSHOT_VERSION).append(" DESC LIMIT 1");
			
			TableSnapshot snapshot = translate(
				jdbcTemplate.queryForObject(sql.toString(), MAPPER, args.toArray())
			);
			
			return Optional.of(snapshot);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

}
