package org.sagebionetworks.repo.model.dbo.file.download;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummary;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.thoughtworks.xstream.XStream;

public class BulkDownloadDAOImpl implements BulkDownloadDAO {

	private static final String SQL_TRUNCATE_DOWNLOAD_ORDERS = "DELETE FROM "+TABLE_DOWNLOAD_ORDER+" WHERE "+COL_DOWNLOAD_ORDER_ID+" > 0";

	private static final String SQL_SELECT_DOWNLOAD_ORDER_SUMMARY = "SELECT " + COL_DOWNLOAD_ORDER_CREATED_BY + ", " + COL_DOWNLOAD_ORDER_CREATED_ON + ", "
			+ COL_DOWNLOAD_ORDER_ID + ", " + COL_DOWNLOAD_ORDER_TOTAL_NUM_FILES + ", "
			+ COL_DOWNLOAD_ORDER_TOTAL_SIZE_MB + ", " + COL_DOWNLOAD_ORDER_FILE_NAME + " FROM "
			+ TABLE_DOWNLOAD_ORDER + " WHERE " + COL_DOWNLOAD_ORDER_CREATED_BY + " = ? ORDER BY "
			+ COL_DOWNLOAD_ORDER_CREATED_ON + " DESC LIMIT ? OFFSET ?";

	public static final String UTF_8 = "UTF-8";

	public static final int MAX_NAME_CHARS = 256;

	public static final String MAX_NAME_MESSAGE = "File name must be " + MAX_NAME_CHARS + " characters or less";

	private static final String SQL_COUNT_USERS_DOWNLOAD_LIST_ITEMS = "SELECT COUNT(*) FROM " + TABLE_DOWNLOAD_LIST_ITEM
			+ " WHERE " + COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID + "  = ?";

	private static final String SQL_CLEAR_USERS_DOWNLOAD_LIST = "DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM + " WHERE "
			+ COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID + " = ?";

	private static final String SQL_TRUNCATE_ALL_DOWNLOAD_LISTS_FOR_ALL_USERS = "DELETE FROM " + TABLE_DOWNLOAD_LIST
			+ " WHERE " + COL_DOWNLOAD_LIST_PRINCIPAL_ID + " > 0";

	private static final String SQL_DELETE_PREFIX = "DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM + " WHERE ("
			+ COL_DOWNLOAD_LIST_PRINCIPAL_ID + "," + COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_ID + ","
			+ COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_TYPE + "," + COL_DOWNLOAD_LIST_ITEM_FILE_HANDLE_ID + ") IN (";

	private static final String SQL_SELECT_DOWNLOAD_LIST_ITEMS = "SELECT * FROM " + TABLE_DOWNLOAD_LIST_ITEM + " WHERE "
			+ COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID + " = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private IdGenerator idGenerator;

	@WriteTransactionReadCommitted
	@Override
	public DownloadList addFilesToDownloadList(String ownerId, List<FileHandleAssociation> toAdd) {
		ValidateArgument.required(ownerId, "ownerId");
		long principalId = Long.parseLong(ownerId);
		// touch the main row
		touchUsersDownloadList(principalId);
		List<DBODownloadListItem> itemDBOs = translateFromDTOtoDBO(principalId, toAdd);
		basicDao.createOrUpdateBatch(itemDBOs);
		return getUsersDownloadList(ownerId);
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList removeFilesFromDownloadList(String ownerId, final List<FileHandleAssociation> toRemove) {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(toRemove, "toRemove");
		final Long ownerIdLong = Long.parseLong(ownerId);
		// touch the main row
		touchUsersDownloadList(ownerIdLong);
		String deleteSQL = createDeleteSQL(toRemove.size());
		this.jdbcTemplate.update(deleteSQL, new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				int parameterIndex = 1;
				for (FileHandleAssociation fas : toRemove) {
					ps.setLong(parameterIndex++, ownerIdLong);
					ps.setLong(parameterIndex++, Long.parseLong(fas.getAssociateObjectId()));
					ps.setString(parameterIndex++, fas.getAssociateObjectType().name());
					ps.setLong(parameterIndex++, Long.parseLong(fas.getFileHandleId()));
				}
			}
		});

		return getUsersDownloadList(ownerId);
	}

	/**
	 * The delete SQL includes bind variables for each row to delete.
	 * 
	 * @param count
	 * @return
	 */
	public static String createDeleteSQL(int count) {
		StringBuilder deleteSQL = new StringBuilder(SQL_DELETE_PREFIX);
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				deleteSQL.append(",");
			}
			deleteSQL.append("(?,?,?,?)");
		}
		deleteSQL.append(") LIMIT ");
		deleteSQL.append(count);
		return deleteSQL.toString();
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList clearDownloadList(String ownerId) {
		ValidateArgument.required(ownerId, "ownerId");
		final Long ownerIdLong = Long.parseLong(ownerId);
		// touch the main row
		touchUsersDownloadList(ownerIdLong);
		this.jdbcTemplate.update(SQL_CLEAR_USERS_DOWNLOAD_LIST, ownerIdLong);
		return getUsersDownloadList(ownerId);
	}

	@Override
	public long getDownloadListFileCount(String ownerId) {
		ValidateArgument.required(ownerId, "ownerId");
		final Long ownerIdLong = Long.parseLong(ownerId);
		return jdbcTemplate.queryForObject(SQL_COUNT_USERS_DOWNLOAD_LIST_ITEMS, Long.class, ownerIdLong);
	}

	@Override
	public DownloadList getUsersDownloadList(String ownerPrincipalId) {
		ValidateArgument.required(ownerPrincipalId, "ownerPrincipalId");
		try {
			// load the main row
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue("principalId", ownerPrincipalId);
			DBODownloadList dbo = basicDao.getObjectByPrimaryKey(DBODownloadList.class, param);
			// load the items
			List<DBODownloadListItem> items = jdbcTemplate.query(SQL_SELECT_DOWNLOAD_LIST_ITEMS,
					new DBODownloadListItem().getTableMapping(), ownerPrincipalId);
			return translateFromDBOtoDTO(dbo, items);
		} catch (NotFoundException e) {
			DownloadList list = new DownloadList();
			list.setOwnerId(ownerPrincipalId);
			list.setUpdatedOn(new Date(System.currentTimeMillis()));
			list.setEtag(UUID.randomUUID().toString());
			return list;
		}
	}

	/**
	 * Update the etag, and updateOn for a user's download list.
	 * 
	 * @param principalId
	 */
	@WriteTransactionReadCommitted
	public void touchUsersDownloadList(long principalId) {
		DBODownloadList toUpdate = new DBODownloadList();
		toUpdate.setEtag(UUID.randomUUID().toString());
		toUpdate.setUpdatedOn(System.currentTimeMillis());
		toUpdate.setPrincipalId(principalId);
		basicDao.createOrUpdate(toUpdate);
	}

	/**
	 * Create a DTO from the given DBO.
	 * 
	 * @param dbo
	 * @return
	 */
	static DownloadList translateFromDBOtoDTO(DBODownloadList dbo, List<DBODownloadListItem> items) {
		DownloadList dto = new DownloadList();
		dto.setOwnerId("" + dbo.getPrincipalId());
		dto.setUpdatedOn(new Date(dbo.getUpdatedOn()));
		dto.setEtag(dbo.getEtag());
		dto.setFilesToDownload(translateFromDBOtoDTO(items));
		return dto;
	}

	/**
	 * Create a list of DTOs from the list of DBOs
	 * 
	 * @param items
	 * @return
	 */
	static List<FileHandleAssociation> translateFromDBOtoDTO(List<DBODownloadListItem> items) {
		if (items == null) {
			return null;
		}
		List<FileHandleAssociation> dtos = new LinkedList<>();
		for (DBODownloadListItem item : items) {
			FileHandleAssociation dto = translateFromDBOtoDTO(item);
			dtos.add(dto);
		}
		return dtos;
	}

	/**
	 * Create a DTO from the DBO.
	 * 
	 * @param item
	 * @return
	 */
	static FileHandleAssociation translateFromDBOtoDTO(DBODownloadListItem item) {
		FileHandleAssociation dto = new FileHandleAssociation();
		dto.setAssociateObjectId("" + item.getAssociatedObjectId());
		dto.setAssociateObjectType(FileHandleAssociateType.valueOf(item.getAssociatedObjectType()));
		dto.setFileHandleId("" + item.getFileHandleId());
		return dto;
	}

	/**
	 * Create a List of DBOs from the passed DTOs
	 * 
	 * @param principalId
	 * @param toAdd
	 * @return
	 */
	static List<DBODownloadListItem> translateFromDTOtoDBO(long principalId, List<FileHandleAssociation> toAdd) {
		ValidateArgument.required(toAdd, "toAdd");
		if (toAdd.isEmpty()) {
			throw new IllegalArgumentException("Must include at least one file to add");
		}
		List<DBODownloadListItem> items = new LinkedList<>();
		for (FileHandleAssociation fha : toAdd) {
			DBODownloadListItem item = translateFromDTOtoDBO(principalId, fha);
			items.add(item);
		}
		return items;
	}

	/**
	 * Create a DBO from the passed DTO
	 * 
	 * @param principalId
	 * @param fha
	 * @return
	 */
	static DBODownloadListItem translateFromDTOtoDBO(long principalId, FileHandleAssociation fha) {
		ValidateArgument.required(fha, "FileHandleAssociation");
		ValidateArgument.required(fha.getAssociateObjectId(), "FileHandleAssociation.associateObjectId");
		ValidateArgument.required(fha.getAssociateObjectType(), "FileHandleAssociation.ssociateObjectType");
		ValidateArgument.required(fha.getFileHandleId(), "FileHandleAssociation.fileHandleId");
		DBODownloadListItem item = new DBODownloadListItem();
		item.setPrincipalId(principalId);
		item.setAssociatedObjectId(Long.parseLong(fha.getAssociateObjectId()));
		item.setAssociatedObjectType(fha.getAssociateObjectType().name());
		item.setFileHandleId(Long.parseLong(fha.getFileHandleId()));
		return item;
	}

	@WriteTransactionReadCommitted
	@Override
	public void truncateAllDownloadDataForAllUsers() {
		this.jdbcTemplate.update(SQL_TRUNCATE_ALL_DOWNLOAD_LISTS_FOR_ALL_USERS);
		this.jdbcTemplate.update(SQL_TRUNCATE_DOWNLOAD_ORDERS);
	}

	@Override
	public DownloadOrder createDownloadOrder(DownloadOrder toCreate) {
		ValidateArgument.required(toCreate, "DownloadOrder");
		Long orderId = idGenerator.generateNewId(IdType.DOWNLOAD_ORDER_ID);
		String orderIdString = "" + orderId;
		toCreate.setOrderId(orderIdString);
		DBODownloadOrder dbo = translateFromDTOtoDBO(toCreate);
		basicDao.createNew(dbo);
		return getDownloadOrder(orderIdString);
	}

	@Override
	public DownloadOrder getDownloadOrder(String orderId) {
		ValidateArgument.required(orderId, "orderId");
		Long orderIdLong = Long.parseLong(orderId);
		DBODownloadOrder dbo = basicDao.getObjectByPrimaryKey(DBODownloadOrder.class,
				new SinglePrimaryKeySqlParameterSource(orderIdLong));
		return translateFromDBOtoDTO(dbo);
	}

	/**
	 * Translate the given DBO to DTO.
	 * 
	 * @param dbo
	 * @return
	 */
	static DownloadOrder translateFromDBOtoDTO(DBODownloadOrder dbo) {
		ValidateArgument.required(dbo, "DBODownloadOrder");
		DownloadOrder dto = new DownloadOrder();
		dto.setCreatedBy("" + dbo.getCreatedBy());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setFiles(translateBytesToFiles(dbo.getFiles()));
		dto.setOrderId("" + dbo.getOrdeId());
		dto.setTotalNumberOfFiles(dbo.getTotalNumberOfFiles());
		dto.setTotalSizeMB(dbo.getTotalSizeMB());
		dto.setZipFileName(dbo.getZipFileName());
		return dto;
	}

	/**
	 * Translate the given DTO to DBO.
	 * 
	 * @param order
	 * @return
	 */
	static DBODownloadOrder translateFromDTOtoDBO(DownloadOrder order) {
		ValidateArgument.required(order, "DownloadOrder");
		ValidateArgument.required(order.getCreatedBy(), "downloadOrder.createdBy");
		ValidateArgument.required(order.getCreatedOn(), "downloadOrder.createdOne");
		ValidateArgument.required(order.getFiles(), "downloadOrder.files");
		ValidateArgument.required(order.getOrderId(), "downloadOrder.orderId");
		ValidateArgument.required(order.getTotalNumberOfFiles(), "order.totalNumberOfFiles");
		ValidateArgument.required(order.getTotalSizeMB(), "order.totalSizeMB");
		ValidateArgument.required(order.getZipFileName(), "order.zipFileName");
		if (order.getZipFileName().length() > MAX_NAME_CHARS) {
			throw new IllegalArgumentException(MAX_NAME_MESSAGE);
		}
		DBODownloadOrder dbo = new DBODownloadOrder();
		dbo.setCreatedBy(Long.parseLong(order.getCreatedBy()));
		dbo.setCreatedOn(order.getCreatedOn().getTime());
		dbo.setFiles(translateFilesToBytes(order.getFiles()));
		dbo.setOrdeId(Long.parseLong(order.getOrderId()));
		dbo.setTotalNumberOfFiles(order.getTotalNumberOfFiles());
		dbo.setTotalSizeMB(order.getTotalSizeMB());
		dbo.setZipFileName(order.getZipFileName());
		return dbo;
	}

	/**
	 * Translate the given list of files to XML as UTF-8 bytes.
	 * 
	 * @param files
	 * @return
	 * @throws IOException
	 */
	static byte[] translateFilesToBytes(List<FileHandleAssociation> files) {
		ValidateArgument.required(files, "files");
		if (files.isEmpty()) {
			throw new IllegalArgumentException("Download list must include at least one file");
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
			GZIPOutputStream zip = new GZIPOutputStream(out);
			Writer writer = new OutputStreamWriter(zip, UTF_8);
			XStream xstream = new XStream();
			xstream.toXML(files, writer);
			IOUtils.closeQuietly(writer);
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Translate the given UTF-8 XML bytes to a list of files.
	 * 
	 * @param files
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static List<FileHandleAssociation> translateBytesToFiles(byte[] files) {
		ValidateArgument.required(files, "files");
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(files);
			GZIPInputStream zip = new GZIPInputStream(in);
			Reader reader = new InputStreamReader(zip, UTF_8);
			XStream xstream = new XStream();
			return (List<FileHandleAssociation>) xstream.fromXML(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<DownloadOrderSummary> getUsersDownloadOrders(String ownerPrincipalId, Long limit, Long offset) {
		ValidateArgument.required(ownerPrincipalId, "ownerPrincipalId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return jdbcTemplate.query(
				SQL_SELECT_DOWNLOAD_ORDER_SUMMARY,
				new RowMapper<DownloadOrderSummary>() {

					@Override
					public DownloadOrderSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
						DownloadOrderSummary summary = new DownloadOrderSummary();
						summary.setCreatedBy("" + rs.getLong(COL_DOWNLOAD_ORDER_CREATED_BY));
						summary.setCreatedOn(new Date(rs.getLong(COL_DOWNLOAD_ORDER_CREATED_ON)));
						summary.setOrderId("" + rs.getLong(COL_DOWNLOAD_ORDER_ID));
						summary.setTotalNumberOfFiles(rs.getLong(COL_DOWNLOAD_ORDER_TOTAL_NUM_FILES));
						summary.setTotalSizeMB(rs.getLong(COL_DOWNLOAD_ORDER_TOTAL_SIZE_MB));
						summary.setZipFileName(rs.getString(COL_DOWNLOAD_ORDER_FILE_NAME));
						return summary;
					}
				}, ownerPrincipalId, limit, offset);
	}

}
