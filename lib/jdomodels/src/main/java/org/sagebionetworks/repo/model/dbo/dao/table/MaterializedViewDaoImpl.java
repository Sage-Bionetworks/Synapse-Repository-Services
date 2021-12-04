package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.dbo.persistence.table.DBOMaterializedViewId.DEFAULT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_ID_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_MV_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_MV_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_SOURCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_SOURCE_TABLE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MV_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MV_TABLES;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionBuilder;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MaterializedViewDaoImpl implements MaterializedViewDao {
	
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public MaterializedViewDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void addSourceTables(IdAndVersion viewId, Set<IdAndVersion> sourceTableIds) {
		String etagUpdateSql = "INSERT INTO " + TABLE_MV_ID + " VALUES(?, UUID()) ON DUPLICATE KEY UPDATE " + COL_MV_ID_ETAG + " = UUID()";
		
		jdbcTemplate.update(etagUpdateSql, viewId.getId());
		
		String inserSql = "INSERT IGNORE INTO " +TABLE_MV_TABLES + " VALUES(?,?,?,?)";
		
		List<IdAndVersion> batch = new ArrayList<>(sourceTableIds);
		
		jdbcTemplate.batchUpdate(inserSql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				IdAndVersion sourceTableId = batch.get(i);
				int index = 1;
				ps.setLong(index++, viewId.getId());
				ps.setLong(index++, viewId.getVersion().orElse(DEFAULT_VERSION));
				ps.setLong(index++, sourceTableId.getId());
				ps.setLong(index, sourceTableId.getVersion().orElse(DEFAULT_VERSION));
			}
			
			@Override
			public int getBatchSize() {
				return sourceTableIds.size();
			}
		});
		
	}

	@Override
	public Set<IdAndVersion> getSourceTables(IdAndVersion viewId) {
		String selectSql = "SELECT " + COL_MV_TABLES_SOURCE_TABLE_ID + ", " + COL_MV_TABLES_SOURCE_TABLE_VERSION + " FROM "
				+ TABLE_MV_TABLES + " WHERE " + COL_MV_TABLES_MV_ID + " = ? AND " + COL_MV_TABLES_MV_VERSION + " = ?";
		
		List<IdAndVersion> sourceTableIds = jdbcTemplate.query(selectSql, (rs, i) -> {
			
			Long id = rs.getLong(1);
			Long version = rs.getLong(2);
			
			IdAndVersionBuilder builder = IdAndVersion.newBuilder()
					.setId(id);
			
			if (!DEFAULT_VERSION.equals(version)) {
				builder.setVersion(version);
			}
			
			return builder.build();
		}, viewId.getId(), viewId.getVersion().orElse(DEFAULT_VERSION));
		
		return new HashSet<>(sourceTableIds);
	}

	@Override
	@WriteTransaction
	public void deleteSourceTables(IdAndVersion viewId, Set<IdAndVersion> sourceTableIds) {
		// TODO Auto-generated method stub
		
	}

}
