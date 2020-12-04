package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_V2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.IntStream;

import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.Sort;
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
	public long addBatchOfFilesToDownloadList(Long userId, List<DownloadListItem> batchToAdd) {
		ValidateArgument.required(userId, "User Id");
		if (batchToAdd == null || batchToAdd.isEmpty()) {
			return 0;
		}
		final Timestamp now = new Timestamp(System.currentTimeMillis());
		createOrUpdateDownloadList(userId);
		DownloadListItem[] batchArray = batchToAdd.toArray(new DownloadListItem[batchToAdd.size()]);
		int[] updates = jdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " (" + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID
						+ "," + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + "," + COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER
						+ "," + COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON + ")  VALUES(?,?,?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						DownloadListItem idAndVersion = batchArray[i];

						if (idAndVersion == null) {
							throw new IllegalArgumentException("Null Item found at index: " + i);
						}
						if (idAndVersion.getFileEntityId() == null) {
							throw new IllegalArgumentException("Null fileEntityId at index: " + i);
						}
						ps.setLong(1, userId);

						ps.setLong(2, KeyFactory.stringToKey(idAndVersion.getFileEntityId()));
						Long versionNumber = NULL_VERSION_NUMBER;
						if (idAndVersion.getVersionNumber() != null) {
							versionNumber = idAndVersion.getVersionNumber();
						}
						ps.setLong(3, versionNumber);
						ps.setTimestamp(4, now);
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
	public long removeBatchOfFilesFromDownloadList(Long userId, List<DownloadListItem> batchToRemove) {
		ValidateArgument.required(userId, "User Id");
		if (batchToRemove == null || batchToRemove.isEmpty()) {
			return 0;
		}
		createOrUpdateDownloadList(userId);
		DownloadListItem[] batchArray = batchToRemove.toArray(new DownloadListItem[batchToRemove.size()]);
		int[] updates = jdbcTemplate.batchUpdate("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ? AND " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " = ? AND "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER + " = ?", new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						DownloadListItem item = batchArray[i];

						if (item == null) {
							throw new IllegalArgumentException("Null Item at index: " + i);
						}
						if (item.getFileEntityId() == null) {
							throw new IllegalArgumentException("Null fileEntityId at index: " + i);
						}
						ps.setLong(1, userId);
						ps.setLong(2, KeyFactory.stringToKey(item.getFileEntityId()));
						Long versionNumber = NULL_VERSION_NUMBER;
						if (item.getVersionNumber() != null) {
							versionNumber = item.getVersionNumber();
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

	private void createOrUpdateDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		jdbcTemplate.update("INSERT INTO " + TABLE_DOWNLOAD_LIST_V2 + "(" + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + ", "
				+ COL_DOWNLOAD_LIST_V2_UPDATED_ON + ", " + COL_DOWNLOAD_LIST_V2_ETAG
				+ ") VALUES (?, NOW(3), UUID()) ON DUPLICATE KEY UPDATE " + COL_DOWNLOAD_LIST_V2_UPDATED_ON
				+ " = NOW(3), " + COL_DOWNLOAD_LIST_V2_ETAG + " = UUID()", userId);
	}

	@Override
	public void clearDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		createOrUpdateDownloadList(userId);
		jdbcTemplate.update("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ?", userId);
	}

	@Override
	public void truncateAllData() {
		jdbcTemplate.update(
				"DELETE FROM " + TABLE_DOWNLOAD_LIST_V2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " > -1");
	}

	@Override
	public DBODownloadList getDBODownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return jdbcTemplate.queryForObject(
				"SELECT * FROM " + TABLE_DOWNLOAD_LIST_V2 + " WHERE " + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ?",
				LIST_MAPPER, userId);
	}

	@Override
	public List<DBODownloadListItem> getDBODownloadListItems(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return jdbcTemplate.query("SELECT * FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ? ORDER BY " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + ", "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER, LIST_ITEM_MAPPER, userId);
	}

	@Override
	public List<DownloadListItemResult> getFilesAvailableToDownloadFromDownloadList(Long userId, List<Sort> sort,
			Long limit, Long offset) {
		// TODO Auto-generated method stub
		return null;
	}

}
