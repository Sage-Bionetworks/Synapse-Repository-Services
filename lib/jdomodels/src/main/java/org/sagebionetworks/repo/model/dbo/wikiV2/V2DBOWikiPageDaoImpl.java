package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_OWNERS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.table.TableConstants.PARENT_ID_PARAMETER_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.P_LIMIT;
import static org.sagebionetworks.repo.model.table.TableConstants.P_OFFSET;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiMarkdownVersion;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

/**
 * The basic implementation of the V2WikiPageDao.
 * (Derived from org.sagebionetworks.repo.model.dbo.dao.DBOWikiPageDaoImpl)
 * 
 * @author hso
 *
 */

public class V2DBOWikiPageDaoImpl implements V2WikiPageDao {
	

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private NamedParameterJdbcTemplate namedTemplate;
	
	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private FileHandleDao fileMetadataDao;	

	/**
	 * Used to detect if a wiki object already exists.
	 */
	private static final String SQL_LOOKUP_WIKI_PAGE_KEY = "SELECT WO."+V2_COL_WIKI_ONWERS_OWNER_ID+", WO."+V2_COL_WIKI_ONWERS_OBJECT_TYPE+", WP."+V2_COL_WIKI_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WP, "+V2_TABLE_WIKI_OWNERS+" WO WHERE WP."+V2_COL_WIKI_ROOT_ID+" = WO."+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" AND WP."+V2_COL_WIKI_ID+" = ?";
	private static final String SQL_DOES_EXIST = "SELECT "+V2_COL_WIKI_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ?";
	private static final String SQL_SELECT_WIKI_ROOT_USING_OWNER_ID_AND_TYPE = "SELECT "+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" FROM "+V2_TABLE_WIKI_OWNERS+" WHERE "+V2_COL_WIKI_ONWERS_OWNER_ID+" = ? AND "+V2_COL_WIKI_ONWERS_OBJECT_TYPE+" = ?";
	private static final String SQL_SELECT_WIKI_OWNER_USING_ROOT_WIKI_ID = "SELECT * FROM "+V2_TABLE_WIKI_OWNERS+" WHERE "+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" = ?";
	private static final String SQL_SELECT_WIKI_USING_ID_AND_ROOT = "SELECT * FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String SQL_SELECT_WIKI_VERSION_USING_ID_AND_ROOT = "SELECT "+V2_COL_WIKI_MARKDOWN_VERSION+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String SQL_GET_CURRENT_WIKI_MARKDOWN_ATTACHMENT_LIST = "SELECT WM."+V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST+" FROM "+V2_TABLE_WIKI_MARKDOWN+" WM, "+V2_TABLE_WIKI_PAGE+" WP WHERE WP."+V2_COL_WIKI_ID+" = ? AND WM."+V2_COL_WIKI_MARKDOWN_ID+" = WP."+V2_COL_WIKI_ID+" AND WP."+V2_COL_WIKI_MARKDOWN_VERSION+" = WM."+V2_COL_WIKI_MARKDOWN_VERSION_NUM;
	private static final String SQL_DELETE_USING_ID_AND_ROOT = "DELETE FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String WIKI_HEADER_SELECT = V2_COL_WIKI_ID+", "+V2_COL_WIKI_TITLE+", "+V2_COL_WIKI_PARENT_ID;
	private static final String SQL_SELECT_CHILDREN_HEADERS = "SELECT "+WIKI_HEADER_SELECT+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ROOT_ID+" = ? ORDER BY "+V2_COL_WIKI_PARENT_ID+", "+V2_COL_WIKI_TITLE+" LIMIT ? OFFSET ?";
	private static final String SQL_LOCK_FOR_UPDATE = "SELECT "+V2_COL_WIKI_ETAG+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? FOR UPDATE";
	private static final String SQL_LOCK_OWNERS_FOR_UPDATE = "SELECT "+V2_COL_WIKI_OWNERS_ETAG+" FROM "+V2_TABLE_WIKI_OWNERS+" WHERE "+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" = ? FOR UPDATE";
	private static final String SQL_COUNT_ALL_WIKIPAGES = "SELECT COUNT(*) FROM "+V2_TABLE_WIKI_PAGE;
	private static final String SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION = "SELECT * FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	private static final String SQL_GET_RESERVATION_OF_ATTACHMENT_IDS = "SELECT "+V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID+" FROM "+V2_TABLE_WIKI_ATTACHMENT_RESERVATION+" WHERE "+V2_COL_WIKI_ATTACHMENT_RESERVATION_ID+" = ?";
	private static final String SQL_GET_WIKI_HISTORY = "SELECT WM."+V2_COL_WIKI_MARKDOWN_VERSION_NUM+", WM."+V2_COL_WIKI_MARKDOWN_MODIFIED_ON+", WM."+V2_COL_WIKI_MARKDOWN_MODIFIED_BY+" FROM "+V2_TABLE_WIKI_MARKDOWN+" WM WHERE WM."+V2_COL_WIKI_MARKDOWN_ID+" = ? ORDER BY "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" DESC LIMIT ?, ?";
	private static final String SQL_COUNT_WIKI_HISTORY = "SELECT COUNT(*) FROM " + V2_TABLE_WIKI_MARKDOWN + " WM WHERE WM." + V2_COL_WIKI_MARKDOWN_ID + " = ?";
	private static final String SQL_GET_MARKDOWN_IDS = "SELECT DISTINCT "+V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID+" FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ?";
	private static final String SQL_GET_CONTENT_FOR_VERSION = "SELECT "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+", "+V2_COL_WIKI_MARKDOWN_TITLE+", "+V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST+", "+V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID+" FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	private static final String SQL_GET_ALL_WIKI_MARKDOWN_ATTACHMENT_LIST = 
			"SELECT "+V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST
			+" FROM "+V2_TABLE_WIKI_MARKDOWN
			+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ?";
	private static final String SQL_UPDATE_WIKI_ETAG =
			"UPDATE " + V2_TABLE_WIKI_PAGE + " SET " + V2_COL_WIKI_ETAG + " = ? " +
			"WHERE " + V2_COL_WIKI_ID + " = ?";
	private static final String SQL_GET_WIKI_VERSION_NUM_BY_RANK =
			"SELECT " + V2_COL_WIKI_MARKDOWN_VERSION +
			" FROM " + V2_TABLE_WIKI_MARKDOWN +
			" WHERE " + V2_COL_WIKI_MARKDOWN_ID + " = :wikiPageId " +
			" ORDER BY " + V2_COL_WIKI_MARKDOWN_VERSION + " DESC" +
			" LIMIT 1 OFFSET :rank";
	private static final String SQL_DELETE_WIKI_VERSIONS2 =
			"DELETE FROM " + V2_TABLE_WIKI_MARKDOWN +
			" WHERE " + V2_COL_WIKI_MARKDOWN_ID + " = :wikiPageId AND " +
			V2_COL_WIKI_MARKDOWN_VERSION + " < :version";
			
	private static final String SQL_INTERSECTION_MARKDOWN_FILEHANDLES = "SELECT DISTINCT "
			+ V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID
			+ " FROM "
			+ V2_TABLE_WIKI_MARKDOWN
			+ " WHERE "
			+ V2_COL_WIKI_MARKDOWN_ID
			+ " = :wikiId AND "
			+ V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID
			+ " IN (:fileHandleIds)";

	private static final TableMapping<V2DBOWikiMarkdown> WIKI_MARKDOWN_ROW_MAPPER = new V2DBOWikiMarkdown().getTableMapping();
	private static final TableMapping<V2DBOWikiPage> WIKI_PAGE_ROW_MAPPER = new V2DBOWikiPage().getTableMapping();
	private static final TableMapping<V2DBOWikiOwner> WIKI_OWNER_ROW_MAPPER = new V2DBOWikiOwner().getTableMapping();
	
	/**
	 * Maps to a simple wiki header.
	 */
	private static final RowMapper<V2WikiHeader> WIKI_HEADER_ROW_MAPPER = new RowMapper<V2WikiHeader>() {
		@Override
		public V2WikiHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
			V2WikiHeader header = new V2WikiHeader();
			header.setId(""+rs.getLong(V2_COL_WIKI_ID));
			header.setTitle(rs.getString(V2_COL_WIKI_TITLE));
			header.setParentId(rs.getString(V2_COL_WIKI_PARENT_ID));
			return header;
		}
	};
	
	/**
	 * Maps to a version/row of a wiki's history
	 */
	private static final RowMapper<V2WikiHistorySnapshot> WIKI_HISTORY_SNAPSHOT_MAPPER = new RowMapper<V2WikiHistorySnapshot>() {
		@Override
		public V2WikiHistorySnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
			V2WikiHistorySnapshot snapshot = new V2WikiHistorySnapshot();
			snapshot.setVersion("" + rs.getLong(V2_COL_WIKI_MARKDOWN_VERSION_NUM));
			snapshot.setModifiedOn(new Date(rs.getLong(V2_COL_WIKI_MARKDOWN_MODIFIED_ON)));
			snapshot.setModifiedBy("" + rs.getLong(V2_COL_WIKI_MARKDOWN_MODIFIED_BY));
			return snapshot;
		}
	};

	/**
	 * Maps to the content (title/markdown id/attachment list) of a wiki's version
	 */
	private static final RowMapper<V2WikiMarkdownVersion> WIKI_MARKDOWN_VERSION_MAPPER = new RowMapper<V2WikiMarkdownVersion>() {
		@Override
		public V2WikiMarkdownVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
			V2WikiMarkdownVersion versionOfMarkdown = new V2WikiMarkdownVersion();
			versionOfMarkdown.setVersion("" + rs.getLong(V2_COL_WIKI_MARKDOWN_VERSION_NUM));
			versionOfMarkdown.setMarkdownFileHandleId(rs.getString(V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID));
			versionOfMarkdown.setTitle(rs.getString(V2_COL_WIKI_MARKDOWN_TITLE));
			// Extract the attachment list in byte[] state
			java.sql.Blob blob = rs.getBlob(V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST);
			if(blob != null){
				List<WikiAttachment> attachmentList = V2WikiTranslationUtils.convertByteArrayToWikiAttachmentList(blob.getBytes(1, (int) blob.length()));
				List<String> fileHandleIds = V2WikiTranslationUtils.getFileHandleIdList(attachmentList);
				versionOfMarkdown.setAttachmentFileHandleIds(fileHandleIds);
			} else {
				versionOfMarkdown.setAttachmentFileHandleIds(null);
			}
			return versionOfMarkdown;
		}
	};

	/**
	 * Map a wiki attachment blob to a list of WikiAttachment objects
	 */
	private static final RowMapper<List<WikiAttachment>> WIKI_ATTACHMENT_MAPPER = new RowMapper<List<WikiAttachment>>() {
		@Override
		public List<WikiAttachment> mapRow(ResultSet rs, int rowNumber) throws SQLException {
			java.sql.Blob blob = rs.getBlob(V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST);
			if(blob != null){
				return V2WikiTranslationUtils.convertByteArrayToWikiAttachmentList(blob.getBytes(1, (int) blob.length()));
			}
			return null;
		}
	};
	
	@WriteTransaction
	@Override
	public V2WikiPage create(V2WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException {
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		
		// Convert to a DBO
		V2DBOWikiPage dbo = V2WikiTranslationUtils.createDBOFromDTO(wikiPage);
		long currentTime = System.currentTimeMillis();
		dbo.setCreatedOn(currentTime);
		dbo.setModifiedOn(dbo.getCreatedOn());
		// We're creating a new wiki page so it has the first version of the markdown
		dbo.setMarkdownVersion(new Long(0));
		
		if(wikiPage.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.WIKI_ID));
		} else {
			// If an id was provided then it must not exist
			if(doesExist(wikiPage.getId())) throw new IllegalArgumentException("A wiki page already exists with ID: "+wikiPage.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(new Long(wikiPage.getId()), IdType.WIKI_ID);
		}
		
		// Check for parent cycle
		if(wikiPage.getParentWikiId() != null) {
			if(checkForParentCycle(WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiPage.getParentWikiId()), wikiPage.getId())) {
				throw new IllegalArgumentException("There will be a cycle if this wiki is updated. Put in valid parentId");
			}
		}
		
		// When we migrate we keep the original etag.  When it is null we set it.
		if(dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		dbo = create(ownerType, dbo, ownerIdLong);
		
		// Create the attachments
		long timeStamp = (currentTime/1000)*1000;
		List<V2DBOWikiAttachmentReservation> attachments = V2WikiTranslationUtils.createDBOAttachmentReservationFromDTO(newFileHandleIds, dbo.getId(), timeStamp);
		// Save them to the attachments archive
		if(attachments.size() > 0){
			basicDao.createBatch(attachments);
		}
		
		// Create the markdown snapshot
		Long markdownFileHandleId = Long.parseLong(wikiPage.getMarkdownFileHandleId());
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameToFileHandleMap, dbo.getId(), markdownFileHandleId, wikiPage.getTitle());
		markdownDbo.setMarkdownVersion(new Long(0));
		markdownDbo.setModifiedOn(currentTime);
		markdownDbo.setModifiedBy(dbo.getModifiedBy());
		// Save this new version to the markdown DB
		basicDao.createNew(markdownDbo);
		
		// Send the create message
		transactionalMessenger.sendMessageAfterCommit(dbo, ChangeType.CREATE);
		
		return get(WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, dbo.getId().toString()), null);
	}
	
	private V2DBOWikiPage create(ObjectType ownerType, V2DBOWikiPage dbo,
			Long ownerIdLong) throws NotFoundException {
		// If the parentID is null then this is a root wiki
		setRoot(ownerIdLong, ownerType, dbo);
		// Save it to the DB
		dbo = basicDao.createNew(dbo);
		// If the parentID is null then this must be a root.
		if(dbo.getParentId() == null){
			// Set the root entry.
			createRootOwnerEntry(ownerIdLong, ownerType, dbo.getId());
		}
		return dbo;
	}
	
	@WriteTransaction
	@Override
	public V2WikiPage updateWikiPage(V2WikiPage wikiPage,
			Map<String, FileHandle> fileNameToFileHandleMap, String ownerId,
			ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException {
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileHandleMap cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(wikiPage.getId() == null) throw new IllegalArgumentException("wikiPage.getID() cannot be null");
		
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		// Does this page exist?
		if(!doesExist(wikiPage.getId())) throw new NotFoundException("No WikiPage exists with id: "+wikiPage.getId());
		Long wikiId = new Long(wikiPage.getId());
		
		// Check for parent cycle
		if(wikiPage.getParentWikiId() != null) {
			if(checkForParentCycle(WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiPage.getParentWikiId()), wikiPage.getId())) {
				throw new IllegalArgumentException("There will be a cycle if this wiki is updated. Put in valid parentId");
			}
		}

		long currentTime = System.currentTimeMillis();

		V2DBOWikiPage oldDbo = getWikiPageDBO(ownerId, ownerType, wikiPage.getId());
		Long incrementedVersion = oldDbo.getMarkdownVersion() + 1;

	    // Update this wiki's entry in the WikiPage database (update version)
	    V2DBOWikiPage newDbo = V2WikiTranslationUtils.createDBOFromDTO(wikiPage);
	    // Set the modifiedon to current.
	    newDbo.setModifiedOn(currentTime);
	    newDbo.setMarkdownVersion(incrementedVersion);
		
		update(ownerType, ownerIdLong, newDbo);

		// Create a new markdown snapshot/version
		Long markdownFileHandleId = Long.parseLong(wikiPage.getMarkdownFileHandleId());
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameToFileHandleMap, newDbo.getId(), markdownFileHandleId, wikiPage.getTitle());
		markdownDbo.setMarkdownVersion(incrementedVersion);
		markdownDbo.setModifiedOn(currentTime);
		markdownDbo.setModifiedBy(newDbo.getModifiedBy());
		// Save this as a new entry of the markdown DB
		basicDao.createNew(markdownDbo);

		// Create the attachments
		long timeStamp = (currentTime/1000)*1000;
		List<V2DBOWikiAttachmentReservation> attachmentsToInsert = V2WikiTranslationUtils.createDBOAttachmentReservationFromDTO(newFileHandleIds, wikiId, timeStamp);
		// Insert only unique/new attachments into the reservation
		// Save them to the attachments archive
		if(attachmentsToInsert.size() > 0) {
			basicDao.createBatch(attachmentsToInsert);
		}
		
		// Send the change message
		transactionalMessenger.sendMessageAfterCommit(newDbo, ChangeType.UPDATE);
		// Return the results.
		return get(WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiPage.getId().toString()), null);
	}
	
	@WriteTransaction
	@Override
	public V2WikiOrderHint updateOrderHint(V2WikiOrderHint orderHint, WikiPageKey key) throws NotFoundException {
		if (key == null) throw new IllegalArgumentException("Key cannot be null");
		if (orderHint == null) throw new IllegalArgumentException("OrderHint cannot be null");
		
		// Get the WikiOwner DBO
		V2DBOWikiOwner dbo = V2WikiTranslationUtils.createWikiOwnerDBOfromOrderHintDTO(orderHint, key.getWikiPageId());
		
		basicDao.update(dbo);
		
		return getWikiOrderHint(key);
		
	}

	private void update(ObjectType ownerType, Long ownerIdLong,
			V2DBOWikiPage newDBO) throws NotFoundException {
		// Set the root
		setRoot(ownerIdLong, ownerType, newDBO);
		// Update
		basicDao.update(newDBO);
	}
	
	private boolean checkForParentCycle(WikiPageKey parentKey, String childId) throws NotFoundException {
		if(parentKey == null) {
			return false;
		} else if(parentKey.getWikiPageId().equals(childId)) {
			return true;
		} else {
			V2WikiPage parent = get(parentKey, null);
			WikiPageKey nextParentKey;
			if(parent.getParentWikiId() == null) {
				nextParentKey = null;
			} else {
				nextParentKey = WikiPageKeyHelper.createWikiPageKey(parentKey.getOwnerObjectId(), parentKey.getOwnerObjectType(), parent.getParentWikiId());
			}
			return checkForParentCycle(nextParentKey, childId);
		}
	}
	
	@Override
	public List<Long> getFileHandleReservationForWiki(WikiPageKey key) {
		return jdbcTemplate.query(SQL_GET_RESERVATION_OF_ATTACHMENT_IDS, new RowMapper<Long>() {
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				Long id = rs.getLong(V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID);
				return id;
			}
		}, key.getWikiPageId());
	}
	
	@Override
	public List<Long> getMarkdownFileHandleIdsForWiki(WikiPageKey key) {
		return jdbcTemplate.query(SQL_GET_MARKDOWN_IDS, new RowMapper<Long>() {
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				Long id = rs.getLong(V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID);
				return id;
			}
		}, key.getWikiPageId());
	}
	
	/**
	 * Validate the owner and the root. An owner can only have one root.
	 * @param ownerId
	 * @param ownerType
	 * @param dbo
	 * @throws NotFoundException
	 */
	private void setRoot(Long ownerId, ObjectType ownerType, V2DBOWikiPage dbo) throws NotFoundException {
		// If a parent ID was provide then this is not a root.
		if(dbo.getParentId() == null){
			// This wiki is the root
			dbo.setRootId(dbo.getId());
		}else{
			// Look up the root
			Long rootWikiId = getRootWiki(ownerId, ownerType);
			dbo.setRootId(rootWikiId);
		}
	}
	
	/**
	 * Lookup the root wiki for a given type.
	 * @param ownerId
	 * @param ownerType
	 * @throws NotFoundException
	 */
	private Long getRootWiki(Long ownerId, ObjectType ownerType) throws NotFoundException {
		try{
			return jdbcTemplate.queryForObject(SQL_SELECT_WIKI_ROOT_USING_OWNER_ID_AND_TYPE, Long.class, ownerId, ownerType.name());
		}catch(DataAccessException e){
			throw new NotFoundException("A root wiki does not exist for ownerId: "+ownerId+" and ownerType: "+ownerType);
		}
	}
	
	/**
	 * Create the root owner entry.
	 * @param ownerId
	 * @param ownerType
	 * @param wikiId
	 * Throws IllegalArgumentException if a root wiki already exists for the given owner.
	 */
	private void createRootOwnerEntry(Long ownerId, ObjectType ownerType, Long rootWikiId){
		// Create the root owner entry
		V2DBOWikiOwner ownerEntry = new V2DBOWikiOwner();
		ownerEntry.setOwnerId(new Long(ownerId));
		ownerEntry.setOwnerType(ownerType);
		ownerEntry.setRootWikiId(rootWikiId);
		ownerEntry.setEtag(UUID.randomUUID().toString());
		
		try{
			basicDao.createNew(ownerEntry);
		} catch (DatastoreException e) {
			throw new IllegalArgumentException("A root wiki already exists for ownerId: "+ownerId+" and ownerType: "+ownerType);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new NameConflictException("An owner already exists with the ownerId: " + ownerId + " and ownerType: " + ownerType);
			} else {
				throw e;
			}
		}

	}
	
	@Override
	public V2WikiPage get(WikiPageKey key, Long version) throws NotFoundException {
		// Get the Wikipage DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);	
		// Now get the markdown
		V2DBOWikiMarkdown markdownDbo;
		if(version == null) {
			markdownDbo = getWikiMarkdownDBO(dbo.getId(), dbo.getMarkdownVersion()); 
		} else {
			markdownDbo = getWikiMarkdownDBO(dbo.getId(), version); 
		}
		List<WikiAttachment> wikiAttachmentList = V2WikiTranslationUtils.convertByteArrayToWikiAttachmentList(markdownDbo.getAttachmentIdList());
		List<String> fileHandleIds = V2WikiTranslationUtils.getFileHandleIdList(wikiAttachmentList);
		return V2WikiTranslationUtils.createDTOfromDBO(dbo, fileHandleIds, markdownDbo);
	}
	
	@Override
	public V2WikiOrderHint getWikiOrderHint(WikiPageKey key) throws NotFoundException {
		// Get the WikiOwner DBO
		V2DBOWikiOwner dbo = getWikiOwnerDBO(key.getWikiPageId());
		
		return V2WikiTranslationUtils.createWikiOrderHintDTOfromDBO(dbo);
		
	}
	
	@Override
	public String getMarkdown(WikiPageKey key, Long version) throws IOException, NotFoundException {
		V2WikiPage wiki = get(key, version);
		S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(wiki.getMarkdownFileHandleId());
		S3Object s3Object = s3Client.getObject(markdownHandle.getBucketName(), markdownHandle.getKey());
		InputStream in = s3Object.getObjectContent();
		Charset charset = ContentTypeUtil.getCharsetFromS3Object(s3Object);
		try{
			return FileUtils.readStreamAsString(in, charset, /*gunzip*/true);
		}finally{
			in.close();
		}
	}
	
	@Override
	public String getMarkdownHandleId(WikiPageKey key, Long version) throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(version == null) {
			// Get the markdown file handle id of the wiki's current version
			version = getCurrentWikiVersion(key.getOwnerObjectId(), key.getOwnerObjectType(), key.getWikiPageId());
		}
		// Lookup the version of the markdown
		V2DBOWikiMarkdown markdownDbo = getWikiMarkdownDBO(Long.parseLong(key.getWikiPageId()), version);
		return String.valueOf(markdownDbo.getFileHandleId());
	}
	
	@Override
	public V2WikiMarkdownVersion getVersionOfWikiContent(WikiPageKey key, Long version) throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(version == null) {
			// Get the current version number
			version = getCurrentWikiVersion(key.getOwnerObjectId(), key.getOwnerObjectType(), key.getWikiPageId());
		}
		String wikiId = key.getWikiPageId();
		List<V2WikiMarkdownVersion> versionOfContent = jdbcTemplate.query(SQL_GET_CONTENT_FOR_VERSION, WIKI_MARKDOWN_VERSION_MAPPER, wikiId, version);
		if(versionOfContent.size() > 1) throw new DatastoreException("More than one Wiki page found with the id: " + wikiId + " and version: " + version);
		if(versionOfContent.size() < 1) throw new NotFoundException("No wiki page found with id: " + wikiId + " and version: " + version);
		return versionOfContent.get(0);
	}
	
	/**
	 * Return the current version of the wiki's markdown/attachments with the given id
	 * @param ownerId
	 * @param ownerType
	 * @param wikiId
	 * @return
	 * @throws NotFoundException
	 */
	@Override
	public Long getCurrentWikiVersion(String ownerId, ObjectType ownerType, String wikiId) throws NotFoundException {
		Long root = getRootWiki(ownerId, ownerType);
		try{
			return jdbcTemplate.queryForObject(SQL_SELECT_WIKI_VERSION_USING_ID_AND_ROOT, Long.class, new Long(wikiId), root);
		}catch(DataAccessException e){
			throw new NotFoundException("A wiki does not exist for id: "+wikiId);
		}
	}
	
	/**
	 * Get the DBOWikiPage using its key.
	 * @param key
	 * @throws NotFoundException
	 */
	private V2DBOWikiPage getWikiPageDBO(WikiPageKey key) throws NotFoundException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		return getWikiPageDBO(key.getOwnerObjectId(), key.getOwnerObjectType(), key.getWikiPageId());
	}
	
	/**
	 * Get the DBOWikiPage using ownerId, ownerType, and wikiId.
	 * @param ownerId
	 * @param ownerType
	 * @param wikiId
	 * @throws NotFoundException
	 */
	private V2DBOWikiPage getWikiPageDBO(String ownerId, ObjectType ownerType, String wikiId) throws NotFoundException {
		// In order to access a wiki you must know its owner.
		// If the root does not exist then the wiki does not exist.
		Long root = getRootWiki(ownerId, ownerType);
		// We use the root in addition to the primary key (id) to enforce they are not out of sych.
		List<V2DBOWikiPage> list = jdbcTemplate.query(SQL_SELECT_WIKI_USING_ID_AND_ROOT, WIKI_PAGE_ROW_MAPPER, new Long(wikiId), root);
		if(list.size() > 1) throw new DatastoreException("More than one Wiki page found with the id: " + wikiId);
		if(list.size() < 1) throw new NotFoundException("No wiki page found with id: " + wikiId);
		return list.get(0);
	}
	
	private V2DBOWikiOwner getWikiOwnerDBO(String rootWikiId) throws NotFoundException {
		List<V2DBOWikiOwner> list = jdbcTemplate.query(SQL_SELECT_WIKI_OWNER_USING_ROOT_WIKI_ID, WIKI_OWNER_ROW_MAPPER, Long.parseLong(rootWikiId));
		if(list.size() > 1) throw new DatastoreException("More than one Wiki owner found with the root wiki ID: " + rootWikiId);
		if(list.size() < 1) throw new NotFoundException("No wiki page found with the root wiki ID: " + rootWikiId);
		return list.get(0);
	}
	
	/**
	 * Get the DBOWikiMarkdown using the wiki id and specific version.
	 * @param wikiId
	 * @param version
	 * @throws NotFoundException
	 */
	private V2DBOWikiMarkdown getWikiMarkdownDBO(Long wikiId, Long version) throws NotFoundException {
		if(wikiId == null) throw new IllegalArgumentException("Wiki id cannot be null");
		if(version == null) throw new IllegalArgumentException("Markdown version cannot be null");
		List<V2DBOWikiMarkdown> list = jdbcTemplate.query(SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION, WIKI_MARKDOWN_ROW_MAPPER, wikiId, version);
		if(list.size() > 1) throw new DatastoreException("Wiki page has multiple versions of number: " + version);
		if(list.size() < 1) throw new NotFoundException("Wiki page of id: " + wikiId + " was not found with version: " + version);
		return list.get(0);
	}
	
	@Override
	public List<V2WikiHistorySnapshot> getWikiHistory(WikiPageKey key, Long limit, Long offset) throws DatastoreException, NotFoundException {
		if(key == null) throw new IllegalArgumentException("WikiPage key cannot be null");
		if(doesExist(key.getWikiPageId())) {
			// Get all versions of a wiki page
			return jdbcTemplate.query(SQL_GET_WIKI_HISTORY, WIKI_HISTORY_SNAPSHOT_MAPPER, key.getWikiPageId(), offset, limit);
		} else {
			throw new NotFoundException("Wiki page with id: " + key.getWikiPageId() + " does not exist.");
	
		}
	}
	
	@Override
	public Long getRootWiki(String ownerId, ObjectType ownerType)
			throws NotFoundException {
		return getRootWiki(KeyFactory.stringToKey(ownerId), ownerType);
	}

	@WriteTransaction
	@Override
	public void delete(WikiPageKey key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// In order to access a wiki you must know its owner.
		// If the root does not exist then the wiki does not exist.
		try{
			Long rootId = getRootWiki(key.getOwnerObjectId(), key.getOwnerObjectType());
			// Delete the wiki using both the root and the id 
			jdbcTemplate.update(SQL_DELETE_USING_ID_AND_ROOT, new Long(key.getWikiPageId()), rootId);
		}catch(NotFoundException e){
			// Nothing to do if the wiki does not exist.
		}
	}

	@Override
	public List<V2WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType, Long limit, Long offset)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(ownerId, "ownerId");
		ValidateArgument.required(ownerType, "ownerType");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		// First look up the root for this owner
		Long root = getRootWiki(ownerId, ownerType);
		// Now use the root to the the full tree
		return jdbcTemplate.query(SQL_SELECT_CHILDREN_HEADERS, WIKI_HEADER_ROW_MAPPER, root, limit, offset);
	}
	
	/**
	 * Propagation should be mandatory because this method should be called from within a transaction,
	 * otherwise the lock won't be held. Not mandatory for testing.
	 */
	@WriteTransaction		
	@Override																	
	public String lockForUpdate(String wikiId) {
		// Lock the wiki row and return current Etag.
		return jdbcTemplate.queryForObject(SQL_LOCK_FOR_UPDATE, String.class, new Long(wikiId));
	}
	
	/**
	 * Propagation should be mandatory because this method should be called from within a transaction,
	 * otherwise the lock won't be held. Not mandatory for testing.
	 */
	@WriteTransaction
	@Override									
	public String lockWikiOwnersForUpdate(String rootWikiId) {
		// Lock the wiki owner row and return current Etag.
		return jdbcTemplate.queryForObject(SQL_LOCK_OWNERS_FOR_UPDATE, String.class, Long.parseLong(rootWikiId));
	}
	
	/**
	 * Retrieves the current attachments list for the given wikiPageId
	 * @param wikiPageId
	 * @return
	 */
	private List<WikiAttachment> getCurrentAttachmentsListFromMarkdownTable(String wikiPageId) {
		ValidateArgument.required(wikiPageId, "wikiPageId");
		List<List<WikiAttachment>> queryResult = jdbcTemplate.query(SQL_GET_CURRENT_WIKI_MARKDOWN_ATTACHMENT_LIST, WIKI_ATTACHMENT_MAPPER, wikiPageId);
		if (queryResult.size() != 1) {
			throw new NotFoundException();
		}
		return queryResult.get(0);
	}

	@Override
	public List<String> getWikiFileHandleIds(WikiPageKey key, Long version) throws NotFoundException {
		ValidateArgument.required(key, "key");
		List<WikiAttachment> attachmentList;
		if(version == null) {
			// Get the attachments for the current wiki
			attachmentList = getCurrentAttachmentsListFromMarkdownTable(key.getWikiPageId());
		} else {
			// Lookup the attachments for another version of the wiki
			V2DBOWikiMarkdown markdownDbo = getWikiMarkdownDBO(Long.parseLong(key.getWikiPageId()), version);
			attachmentList = V2WikiTranslationUtils.convertByteArrayToWikiAttachmentList(markdownDbo.getAttachmentIdList());
		}
		return V2WikiTranslationUtils.getFileHandleIdList(attachmentList);
	}

	@Override
	public String getWikiAttachmentFileHandleForFileName(WikiPageKey key,
			String fileName, Long version) throws NotFoundException {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(fileName, "fileName");
		List<WikiAttachment> attachmentList;
		if(version == null) {
			attachmentList = getCurrentAttachmentsListFromMarkdownTable(key.getWikiPageId());
		} else {
			// Lookup the attachments for another version of the wiki
			V2DBOWikiMarkdown markdownDbo = getWikiMarkdownDBO(Long.parseLong(key.getWikiPageId()), version);
			attachmentList = V2WikiTranslationUtils.convertByteArrayToWikiAttachmentList(markdownDbo.getAttachmentIdList());
		}
		return V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(attachmentList, fileName, 
				new NotFoundException("Cannot find a wiki attachment for OwnerID: "+key.getOwnerObjectId()+", ObjectType: "+key.getOwnerObjectType()+", WikiPageId: "+key.getWikiPageId()+", fileName: "+fileName));
	}

	@Override
	public WikiPageKey lookupWikiKey(String wikiId) throws NotFoundException {
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null");
		long id = Long.parseLong(wikiId);
		try{
			return this.jdbcTemplate.queryForObject(SQL_LOOKUP_WIKI_PAGE_KEY, new RowMapper<WikiPageKey>() {
				@Override
				public WikiPageKey mapRow(ResultSet rs, int rowNum) throws SQLException {
					String owner = rs.getString(V2_COL_WIKI_ONWERS_OWNER_ID);
					ObjectType type = ObjectType.valueOf(rs.getString(V2_COL_WIKI_ONWERS_OBJECT_TYPE));
					if(ObjectType.ENTITY == type){
						owner = KeyFactory.keyToString(Long.parseLong(owner));
					}
					String wikiId = rs.getString(V2_COL_WIKI_ID);
					return WikiPageKeyHelper.createWikiPageKey(owner, type, wikiId);
				}
			}, id);
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("Cannot find a wiki page with id: "+wikiId);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return jdbcTemplate.queryForObject(SQL_COUNT_ALL_WIKIPAGES, Long.class);
	}

	private boolean doesExist(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		try{
			// Is this in the database.
			jdbcTemplate.queryForObject(SQL_DOES_EXIST, Long.class, id);
			return true;
		}catch(EmptyResultDataAccessException e){
			return false;
		}
	}

	@Override
	public Set<String> getFileHandleIdsAssociatedWithWikiAttachments(List<String> fileHandleIds, String wikiPageId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ValidateArgument.required(wikiPageId, "wikiPageId");
		Set<String> results = new HashSet<String>();
		if (fileHandleIds.isEmpty()) {
			return results;
		}
		results.addAll(fileHandleIds);
		List<WikiAttachment> attachmentList = getAllAttachmentsListFromMarkdownTable(wikiPageId);
		List<String> foundFileHandleIds = V2WikiTranslationUtils.getFileHandleIdList(attachmentList);
		results.retainAll(foundFileHandleIds);
		return results;
	}

	/**
	 * Get all attachments that associate with a wikiPageId
	 * 
	 * @param wikiPageId
	 * @return
	 */
	private List<WikiAttachment> getAllAttachmentsListFromMarkdownTable(String wikiPageId) {
		List<WikiAttachment> allAttachments = new ArrayList<WikiAttachment>();
		List<List<WikiAttachment>> queryResult = jdbcTemplate.query(SQL_GET_ALL_WIKI_MARKDOWN_ATTACHMENT_LIST, WIKI_ATTACHMENT_MAPPER, wikiPageId);
		for(List<WikiAttachment> list: queryResult) {
			allAttachments.addAll(list);
		}
		return allAttachments;
	}

	@WriteTransaction
	@Override
	public void deleteWikiVersions(WikiPageKey key, Long minVersionToKeep) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null.");
		}
		if (minVersionToKeep == null) {
			throw new IllegalArgumentException("MinVersionToKeep cannot be null.");
		}
		String wikiId = key.getWikiPageId();
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("wikiPageId", wikiId);
		params.addValue("version", minVersionToKeep);
		try {
			namedTemplate.update(SQL_DELETE_WIKI_VERSIONS2, params);
		} catch (NotFoundException e) {
			// Nothing to do if none found
		}
	}

	@WriteTransaction
	@Override
	public void updateWikiEtag(WikiPageKey key, String etag) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null.");
		}
		if (etag == null) {
			throw new IllegalArgumentException("Etag cannot be null.");
		}
		String wikiId = key.getWikiPageId();
		int ra = jdbcTemplate.update(SQL_UPDATE_WIKI_ETAG, etag, wikiId);
		return;
	}

	@Override
	public Long getNumberOfVersions(WikiPageKey key) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		String wikiId = key.getWikiPageId();
		Long numVersions = jdbcTemplate.queryForObject(SQL_COUNT_WIKI_HISTORY, Long.class, wikiId);
		return numVersions;
	}

	@Override
	public Long getWikiVersionByRank(WikiPageKey key, Long rank) throws NotFoundException {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null.");
		}
		if (rank == null) {
			throw new IllegalArgumentException(("Rank cannot be null."));
		}
		if (rank < 0) {
			throw new IllegalArgumentException("Rank must be positive.");
		}
		String wikiId = key.getWikiPageId();
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("wikiPageId", wikiId);
		params.addValue("rank", rank);
		Long v = 0L;
		try {
			v = namedTemplate.queryForObject(SQL_GET_WIKI_VERSION_NUM_BY_RANK, params, Long.class);
		} catch (EmptyResultDataAccessException e) {
			// Just return 0
			// Note: This is fine for use case (<= getWikiVersionByRank()) but
			// we should return the min(version) or raise an exception if we were
			// to use the version returned.
		}
		return v;
	}

	public Set<String> getFileHandleIdsAssociatedWithWikiMarkdown (
			List<String> fileHandleIds, String wikiId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		final Set<String> results = new HashSet<String>();
		if(fileHandleIds.isEmpty()){
			return results;
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("fileHandleIds", fileHandleIds);
		param.addValue("wikiId", wikiId);
		namedTemplate.query(SQL_INTERSECTION_MARKDOWN_FILEHANDLES, param, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				results.add(rs.getString(1));
			}
		});
		return results;
	}
	
}
