package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_FILE_ASSOC_FILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_FILE_ASSOC_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_FILE_ASSOCIATION;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dao.table.TableFileAssociationDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableFileAssociation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Lists;

public class TableFileAssociationDaoImpl implements TableFileAssociationDao {
	
	TableMapping<DBOTableFileAssociation> tableMapping = new DBOTableFileAssociation().getTableMapping();

	private static final String SQL_SELECT_FILES_ASSOCIATED_WITH_TABLE = "SELECT "
			+ COL_TABLE_FILE_ASSOC_FILE_ID
			+ " FROM "
			+ TABLE_TABLE_FILE_ASSOCIATION
			+ " WHERE "
			+ COL_TABLE_FILE_ASSOC_FILE_ID
			+ " IN ( :fileIds ) AND "
			+ COL_TABLE_FILE_ASSOC_TABLE_ID + " = :tableId";

	private static final String SQL_INSERT_INGORE = "INSERT IGNORE INTO "
			+ TABLE_TABLE_FILE_ASSOCIATION + " ("
			+ COL_TABLE_FILE_ASSOC_TABLE_ID + ", "
			+ COL_TABLE_FILE_ASSOC_FILE_ID + ") VALUES (?,?)";

	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	

	@WriteTransaction
	@Override
	public void bindFileHandleIdsToTable(String tableIdString,
			Set<String> fileHandleIds) {
		if (tableIdString == null) {
			throw new IllegalArgumentException("TableId cannot be null");
		}
		if (fileHandleIds == null) {
			throw new IllegalArgumentException("FileHandlIds cannot be null");
		}
		if (fileHandleIds.isEmpty()) {
			// nothing to do.
			return;
		}
		final long tableId = KeyFactory.stringToKey(tableIdString);
		// Convert to longs
		final long[] fileHandleIdArray = new long[fileHandleIds.size()];
		int index = 0;
		for (String fileString : fileHandleIds) {
			fileHandleIdArray[index] = Long.parseLong(fileString);
			index++;
		}

		jdbcTemplate.batchUpdate(SQL_INSERT_INGORE,
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {
						ps.setLong(1, tableId);
						ps.setLong(2, fileHandleIdArray[i]);
					}

					@Override
					public int getBatchSize() {
						return fileHandleIdArray.length;
					}
				});

	}

	@Override
	public Set<String> getFileHandleIdsAssociatedWithTable(
			List<String> fileHandleIds, String tableIdString) {
		if (tableIdString == null) {
			throw new IllegalArgumentException("TableId cannot be null");
		}
		Long tableId = KeyFactory.stringToKey(tableIdString);
		if (fileHandleIds == null) {
			throw new IllegalArgumentException("FileHandlIds cannot be null");
		}
		final Set<String> results = new HashSet<String>();
		for (List<String> fileHandleIdsBatch : Lists.partition(fileHandleIds,
				SqlConstants.MAX_LONGS_PER_IN_CLAUSE / 2)) {
			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("tableId", tableId);
			parameters.addValue("fileIds", fileHandleIdsBatch); 
			namedParameterJdbcTemplate.query(SQL_SELECT_FILES_ASSOCIATED_WITH_TABLE,parameters,
					new RowMapper<Void>() {
						@Override
						public Void mapRow(ResultSet rs, int rowNum)
								throws SQLException {
							String fileHandleId = rs
									.getString(COL_TABLE_FILE_ASSOC_FILE_ID);
							results.add(fileHandleId);
							return null;
						}
					});
		}
		return results;
	}

}
