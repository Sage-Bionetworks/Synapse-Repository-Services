package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_2_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_2_VERION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOWNLOAD_LIST_ITEM_2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM_2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.IntStream;

import org.sagebionetworks.repo.model.download.ColumnName;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.IdAndVersion;
import org.sagebionetworks.repo.model.download.SortDirection;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DownloadListDAOImpl implements DownloadListDAO {

	public static final Long NULL_VERSION_NUMBER = -1L;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final RowMapper<DBODownloadList> LIST_MAPPER = new DBODownloadList().getTableMapping();
	private static final RowMapper<DBODownloadListItem> LIST_ITEM_MAPPER = new DBODownloadListItem().getTableMapping();

	@WriteTransaction
	@Override
	public long addBatchOfFilesToDownloadList(Long userId, List<IdAndVersion> batchToAdd) {
		ValidateArgument.required(userId, "User Id");
		if (batchToAdd == null || batchToAdd.isEmpty()) {
			return 0;
		}
		createOrUpdateDownloadList(userId);
		IdAndVersion[] batchArray = batchToAdd.toArray(new IdAndVersion[batchToAdd.size()]);
		int[] updates = jdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO " + TABLE_DOWNLOAD_LIST_ITEM_2 + " (" + COL_DOWNLOAD_LIST_ITEM_2_PRINCIPAL_ID + ","
						+ COL_DOWNLOAD_LIST_ITEM_2_ENTITY_ID + "," + COL_DOWNLOAD_LIST_ITEM_2_VERION_NUMBER + ","
						+ COL_DOWNLOAD_LIST_ITEM_2_ADDED_ON + ")  VALUES(?,?,?,NOW())",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						IdAndVersion idAndVersion = batchArray[i];

						if (idAndVersion == null) {
							throw new IllegalArgumentException("Null file ID passed at index: " + i);
						}
						if (idAndVersion.getEntityId() == null) {
							throw new IllegalArgumentException("Null entityId at index: " + i);
						}
						ps.setLong(1, userId);

						ps.setLong(2, KeyFactory.stringToKey(idAndVersion.getEntityId()));
						Long versionNumber = NULL_VERSION_NUMBER;
						if (idAndVersion.getVersionNumber() != null) {
							versionNumber = idAndVersion.getVersionNumber();
						}
						ps.setLong(3, versionNumber);
					}

					@Override
					public int getBatchSize() {
						return batchArray.length;
					}
				});
		return IntStream.of(updates).sum();
	}

	@WriteTransaction
	@Override
	public long removeBatchOfFilesFromDownloadList(Long userId, List<IdAndVersion> batchToRemove) {
		ValidateArgument.required(userId, "User Id");
		if (batchToRemove == null || batchToRemove.isEmpty()) {
			return 0;
		}
		createOrUpdateDownloadList(userId);
		IdAndVersion[] batchArray = batchToRemove.toArray(new IdAndVersion[batchToRemove.size()]);
		int[] updates = jdbcTemplate
				.batchUpdate(
						"DELETE FROM " + DDL_DOWNLOAD_LIST_ITEM_2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_2_ENTITY_ID
								+ " = ? AND " + COL_DOWNLOAD_LIST_ITEM_2_VERION_NUMBER + " = ?",
						new BatchPreparedStatementSetter() {

							@Override
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								IdAndVersion idAndVersion = batchArray[i];

								if (idAndVersion == null) {
									throw new IllegalArgumentException("Null file ID passed at index: " + i);
								}
								if (idAndVersion.getEntityId() == null) {
									throw new IllegalArgumentException("Null entityId at index: " + i);
								}

								ps.setLong(1, KeyFactory.stringToKey(idAndVersion.getEntityId()));
								Long versionNumber = NULL_VERSION_NUMBER;
								if (idAndVersion.getVersionNumber() != null) {
									versionNumber = idAndVersion.getVersionNumber();
								}
								ps.setLong(2, versionNumber);
							}

							@Override
							public int getBatchSize() {
								return batchArray.length;
							}
						});
		return IntStream.of(updates).sum();
	}

	private void createOrUpdateDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		jdbcTemplate.update("INSERT INTO " + TABLE_DOWNLOAD_LIST_2 + "(" + COL_DOWNLOAD_LIST_2_PRINCIPAL_ID + ", "
				+ COL_DOWNLOAD_LIST_2_UPDATED_ON + ", " + COL_DOWNLOAD_LIST_2_ETAG
				+ ") VALUES (?, NOW(), UUID()) ON DUPLICATE KEY UPDATE " + COL_DOWNLOAD_LIST_2_UPDATED_ON + " = NOW(), "
				+ COL_DOWNLOAD_LIST_2_ETAG + " = UUID()", userId);
	}

	@Override
	public void clearDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		createOrUpdateDownloadList(userId);
		jdbcTemplate.update("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_2 + " WHERE "
				+ COL_DOWNLOAD_LIST_ITEM_2_PRINCIPAL_ID + " = ?", userId);
	}

	@Override
	public List<DownloadListItem> getFilesAvailableToDownloadFromDownloadList(Long userId, ColumnName sortColumn,
			SortDirection sortDirection, Long limit, Long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void truncateAllData() {
		jdbcTemplate.update(
				"DELETE FROM " + TABLE_DOWNLOAD_LIST_2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_2_PRINCIPAL_ID + " > -1");
	}

	@Override
	public DBODownloadList getDBODownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return jdbcTemplate.queryForObject(
				"SELECT * FROM " + TABLE_DOWNLOAD_LIST_2 + " WHERE " + COL_DOWNLOAD_LIST_2_PRINCIPAL_ID + " = ?",
				LIST_MAPPER, userId);
	}

	@Override
	public List<DBODownloadListItem> getDBODownloadListItems(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return jdbcTemplate.query(
				"SELECT * FROM " + TABLE_DOWNLOAD_LIST_ITEM_2 + " WHERE " + COL_DOWNLOAD_LIST_2_PRINCIPAL_ID + " = ?",
				LIST_ITEM_MAPPER, userId);
	}

}
