package org.sagebionetworks.repo.model.dbo.v2.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.V2WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiOwner;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiPage;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	private TagMessenger tagMessenger;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	/**
	 * Used to detect if a wiki object already exists.
	 */
	private static final String SQL_LOOKUP_WIKI_PAGE_KEY = "SELECT WO."+V2_COL_WIKI_ONWERS_OWNER_ID+", WO."+V2_COL_WIKI_ONWERS_OBJECT_TYPE+", WP."+V2_COL_WIKI_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WP, "+V2_TABLE_WIKI_OWNERS+" WO WHERE WP."+V2_COL_WIKI_ROOT_ID+" = WO."+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" AND WP."+V2_COL_WIKI_ID+" = ?";
	private static final String SQL_DOES_EXIST = "SELECT "+V2_COL_WIKI_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ?";
	private static final String SQL_SELECT_WIKI_ROOT_USING_OWNER_ID_AND_TYPE = "SELECT "+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" FROM "+V2_TABLE_WIKI_OWNERS+" WHERE "+V2_COL_WIKI_ONWERS_OWNER_ID+" = ? AND "+V2_COL_WIKI_ONWERS_OBJECT_TYPE+" = ?";
	private static final String SQL_SELECT_WIKI_USING_ID_AND_ROOT = "SELECT * FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String SQL_SELECT_WIKI_ATTACHMENT = "SELECT * FROM "+V2_TABLE_WIKI_ATTACHMENT_RESERVATION+" WHERE "+V2_COL_WIKI_ATTACHMENT_RESERVATION_ID+" = ? AND "+V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID+" = ?";
	private static final String SQL_GET_WIKI_MARKDOWN_ATTACHMENT_ID_LIST = "SELECT * FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	private static final String SQL_DELETE_USING_ID_AND_ROOT = "DELETE FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String WIKI_HEADER_SELECT = V2_COL_WIKI_ID+", "+V2_COL_WIKI_TITLE+", "+V2_COL_WIKI_PARENT_ID;
	private static final String SQL_SELECT_CHILDREN_HEADERS = "SELECT "+WIKI_HEADER_SELECT+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ROOT_ID+" = ? ORDER BY "+V2_COL_WIKI_PARENT_ID+", "+V2_COL_WIKI_TITLE;
	private static final String SQL_LOCK_FOR_UPDATE = "SELECT "+V2_COL_WIKI_ETAG+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? FOR UPDATE";
	private static final String SQL_COUNT_ALL_WIKIPAGES = "SELECT COUNT(*) FROM "+V2_TABLE_WIKI_PAGE;
	private static final String SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION = "SELECT * FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	private static final String SQL_GET_WIKI_ATTACHMENT_RESERVATION = "SELECT * FROM "+V2_TABLE_WIKI_ATTACHMENT_RESERVATION+" WHERE "+V2_COL_WIKI_ATTACHMENT_RESERVATION_ID+" = ?";
	private static final String SQL_GET_WIKI_HISTORY = "SELECT * FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? ORDER BY "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" DESC LIMIT ?";
	
	private static final TableMapping<V2DBOWikiAttachmentReservation> ATTACHMENT_ROW_MAPPER = new V2DBOWikiAttachmentReservation().getTableMapping();
	private static final TableMapping<V2DBOWikiMarkdown> WIKI_MARKDOWN_ROW_MAPPER = new V2DBOWikiMarkdown().getTableMapping();
	private static final TableMapping<V2DBOWikiPage> WIKI_PAGE_ROW_MAPPER = new V2DBOWikiPage().getTableMapping();	
	
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

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public V2WikiPage create(V2WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType) throws NotFoundException {
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
			dbo.setId(idGenerator.generateNewId(TYPE.WIKI_ID));
		} else {
			// If an id was provided then it must not exist
			if(doesExist(wikiPage.getId())) throw new IllegalArgumentException("A wiki page already exists with ID: "+wikiPage.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(new Long(wikiPage.getId()), TYPE.WIKI_ID);
		}
		
		// When we migrate we keep the original etag.  When it is null we set it.
		if(dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		dbo = create(ownerType, dbo, ownerIdLong);
		
		// Create the attachments
		long timeStamp = (currentTime/1000)*1000;
		List<V2DBOWikiAttachmentReservation> attachments = V2WikiTranslationUtils.createDBOAttachmentReservationFromDTO(fileNameToFileHandleMap, dbo.getId(), timeStamp);
		// Save them to the attachments archive
		if(attachments.size() > 0){
			basicDao.createBatch(attachments);
		}
		
		// Create the markdown snapshot
		Long markdownFileHandleId = Long.parseLong(wikiPage.getMarkdownFileHandleId());
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameToFileHandleMap, dbo.getId(), markdownFileHandleId);
		markdownDbo.setMarkdownVersion(new Long(0));
		markdownDbo.setModifiedOn(currentTime);
		markdownDbo.setModifiedBy(dbo.getModifiedBy());
		// Save this new version to the markdown DB
		basicDao.createNew(markdownDbo);
		
		// Send the create message
		tagMessenger.sendMessage(dbo.getId().toString(), dbo.getEtag(), ObjectType.WIKI, ChangeType.CREATE);
		
		try {
			return get(new WikiPageKey(ownerId, ownerType, dbo.getId().toString()));
		} catch (NotFoundException e) {
			// This should not occur.
			throw new RuntimeException(e);
		}
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
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public V2WikiPage updateWikiPage(V2WikiPage wikiPage,
			Map<String, FileHandle> fileNameToFileHandleMap, String ownerId,
			ObjectType ownerType, boolean keepEtag) throws NotFoundException {
		
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileHandleMap cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(wikiPage.getId() == null) throw new IllegalArgumentException("wikiPage.getID() cannot be null");
		
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		// Does this page exist?
		if(!doesExist(wikiPage.getId())) throw new NotFoundException("No WikiPage exists with id: "+wikiPage.getId());
		Long wikiId = new Long(wikiPage.getId());
		
		long currentTime = System.currentTimeMillis();

		V2DBOWikiPage oldDbo = getWikiPageDBO(ownerId, ownerType, wikiPage.getId());
		Long incrementedVersion = oldDbo.getMarkdownVersion() + 1;

		// Update this wiki's entry in the WikiPage database (update version)
		V2DBOWikiPage newDbo = V2WikiTranslationUtils.createDBOFromDTO(wikiPage);
		if(!keepEtag){
			// We keep the etag for migration scenarios.
			newDbo.setEtag(UUID.randomUUID().toString());
		}
		// Set the modifiedon to current.
		newDbo.setModifiedOn(currentTime);
		newDbo.setMarkdownVersion(incrementedVersion);
		
		update(ownerType, ownerIdLong, newDbo);

		// Create a new markdown snapshot/version
		Long markdownFileHandleId = Long.parseLong(wikiPage.getMarkdownFileHandleId());
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameToFileHandleMap, newDbo.getId(), markdownFileHandleId);
		markdownDbo.setMarkdownVersion(incrementedVersion);
		markdownDbo.setModifiedOn(currentTime);
		markdownDbo.setModifiedBy(newDbo.getModifiedBy());
		// Save this as a new entry of the markdown DB
		basicDao.createNew(markdownDbo);

		// Create the attachments
		long timeStamp = (currentTime/1000)*1000;
		List<V2DBOWikiAttachmentReservation> attachments = V2WikiTranslationUtils.createDBOAttachmentReservationFromDTO(fileNameToFileHandleMap, wikiId, timeStamp);
		
		// Compare all stored attachments for a given wiki to the list of attachments for this updated version
		// Insert only unique/new attachments into the reservation
		List<V2DBOWikiAttachmentReservation> reservation = simpleJdbcTemplate.query(SQL_GET_WIKI_ATTACHMENT_RESERVATION, ATTACHMENT_ROW_MAPPER, wikiId);
		Set<Long> reservationFileHandleIds = new HashSet<Long>();
		for(V2DBOWikiAttachmentReservation attach: reservation) {
			reservationFileHandleIds.add(attach.getFileHandleId());
		}
		List<V2DBOWikiAttachmentReservation> toInsert = new ArrayList<V2DBOWikiAttachmentReservation>();
		for(V2DBOWikiAttachmentReservation attach: attachments) {
			if(!reservationFileHandleIds.contains(attach.getFileHandleId())) {
				toInsert.add(attach);
			}
		}
		// Save them to the attachments archive
		if(toInsert.size() > 0) {
			basicDao.createBatch(toInsert);
		}
		
		// Send the change message
		tagMessenger.sendMessage(newDbo.getId().toString(), newDbo.getEtag(), ObjectType.WIKI, ChangeType.UPDATE);
		// Return the results.
		return get(new WikiPageKey(ownerId, ownerType, wikiPage.getId().toString()));
	}

	private void update(ObjectType ownerType, Long ownerIdLong,
			V2DBOWikiPage newDBO) throws NotFoundException {
		// Set the root
		setRoot(ownerIdLong, ownerType, newDBO);
		// Update
		basicDao.update(newDBO);
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
			return simpleJdbcTemplate.queryForLong(SQL_SELECT_WIKI_ROOT_USING_OWNER_ID_AND_TYPE, ownerId, ownerType.name());
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
		ownerEntry.setOwnerTypeEnum(ownerType);
		ownerEntry.setRootWikiId(rootWikiId);
		try{
			basicDao.createNew(ownerEntry);
		} catch (DatastoreException e) {
			throw new IllegalArgumentException("A root wiki already exists for ownerId: "+ownerId+" and ownerType: "+ownerType);
		} catch (DuplicateKeyException e) {
			throw new ConflictingUpdateException("The wiki you are attempting to create already exists.  Try fetching the Wiki and then updating it.");
		}

	}
	
	@Override
	public V2WikiPage get(WikiPageKey key) throws NotFoundException {
		// Get the Wikipage DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);
		// Now get the attachments
		List<V2DBOWikiAttachmentReservation> attachments = getAttachmentDbos(dbo);
		// Now get the markdown
		V2DBOWikiMarkdown markdownDbo = getWikiMarkdownDBO(dbo.getId(), dbo.getMarkdownVersion()); 
		return V2WikiTranslationUtils.createDTOfromDBO(dbo, attachments, markdownDbo.getFileHandleId());
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
		List<V2DBOWikiPage> list = simpleJdbcTemplate.query(SQL_SELECT_WIKI_USING_ID_AND_ROOT, WIKI_PAGE_ROW_MAPPER, new Long(wikiId), root);
		if(list.size() > 1) throw new DatastoreException("More than one Wiki page found with the id: " + wikiId);
		if(list.size() < 1) throw new NotFoundException("No wiki page found with id: " + wikiId);
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
		List<V2DBOWikiMarkdown> list = simpleJdbcTemplate.query(SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION, WIKI_MARKDOWN_ROW_MAPPER, wikiId, version);
		if(list.size() > 1) throw new DatastoreException("Wiki page has multiple versions of number: " + version);
		if(list.size() < 1) throw new NotFoundException("Wiki page of id: " + wikiId + " was not found with version: " + version);
		return list.get(0);
	}
	
	@Override
	public List<V2WikiPage> getWikiHistory(WikiPageKey key) throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("WikiPage key cannot be null");
		List<V2WikiPage> history = new ArrayList<V2WikiPage>();
		V2DBOWikiPage wikiDbo = getWikiPageDBO(key);
		
		// Get all versions of a wiki page
		List<V2DBOWikiMarkdown> snapshots = simpleJdbcTemplate.query(SQL_GET_WIKI_HISTORY, WIKI_MARKDOWN_ROW_MAPPER, key.getWikiPageId(), 100);
		
		// For each version, recreate the WikiPage and add to the history list
		for(V2DBOWikiMarkdown snapshot: snapshots) {
			wikiDbo.setMarkdownVersion(snapshot.getMarkdownVersion());
			wikiDbo.setModifiedBy(snapshot.getModifiedBy());
			wikiDbo.setModifiedOn(snapshot.getModifiedOn());

			// Get list of attachments for this snapshot/version
			List<V2DBOWikiAttachmentReservation> attachments = getAttachmentDbos(wikiDbo);
			
			V2WikiPage page = V2WikiTranslationUtils.createDTOfromDBO(wikiDbo, attachments, snapshot.getFileHandleId());
			history.add(page);
		}
		return history;
	}
	
	@Override
	public Long getRootWiki(String ownerId, ObjectType ownerType)
			throws NotFoundException {
		return getRootWiki(KeyFactory.stringToKey(ownerId), ownerType);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(WikiPageKey key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// In order to access a wiki you must know its owner.
		// If the root does not exist then the wiki does not exist.
		try{
			Long rootId = getRootWiki(key.getOwnerObjectId(), key.getOwnerObjectType());
			// Delete the wiki using both the root and the id 
			simpleJdbcTemplate.update(SQL_DELETE_USING_ID_AND_ROOT, new Long(key.getWikiPageId()), rootId);
		}catch(NotFoundException e){
			// Nothing to do if the wiki does not exist.
		}
	}

	@Override
	public List<V2WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType)
			throws DatastoreException, NotFoundException {
		// First look up the root for this owner
		Long root = getRootWiki(ownerId, ownerType);
		// Now use the root to the the full tree
		return simpleJdbcTemplate.query(SQL_SELECT_CHILDREN_HEADERS, WIKI_HEADER_ROW_MAPPER, root);
	}

	@Override
	public String lockForUpdate(String wikiId) {
		// Lock the wiki row and return current Etag.
		return simpleJdbcTemplate.queryForObject(SQL_LOCK_FOR_UPDATE, String.class, new Long(wikiId));
	}

	@Override
	public List<String> getWikiFileHandleIds(WikiPageKey key)
			throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// Get the DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);
		// Now get the attachment ids
		return getAttachmentFileHandleIds(dbo);
	}

	@Override
	public String getWikiAttachmentFileHandleForFileName(WikiPageKey key,
			String fileName) throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		// Get the DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);
		// Get the markdown version/snapshot and extract from the attachment id list column
		V2DBOWikiMarkdown markdownVersion = getVersionOfMarkdown(dbo.getId(), dbo.getMarkdownVersion());
		byte[] attachmentIdList = markdownVersion.getAttachmentIdList();
		if(attachmentIdList != null) {
			// Process the list of attachments into a map for easy searching
			Map<String, String> fileNameToIdMap;
			try {
				fileNameToIdMap = getFileNameAndHandleIdPairs(new String(attachmentIdList, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}

			// Return the associated file handle id if filename exists
			if(fileNameToIdMap.containsKey(fileName)) {
				return fileNameToIdMap.get(fileName);
			}
		}
		// No attachment with the file name exists for this wiki page
		throw new NotFoundException("Cannot find a wiki attachment for OwnerID: "+key.getOwnerObjectId()+", ObjectType: "+key.getOwnerObjectType()+", WikiPageId: "+key.getWikiPageId()+", fileName: "+fileName);
	}

	@Override
	public WikiPageKey lookupWikiKey(String wikiId) throws NotFoundException {
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null");
		long id = Long.parseLong(wikiId);
		try{
			return this.simpleJdbcTemplate.queryForObject(SQL_LOOKUP_WIKI_PAGE_KEY, new RowMapper<WikiPageKey>() {
				@Override
				public WikiPageKey mapRow(ResultSet rs, int rowNum) throws SQLException {
					String owner = rs.getString(V2_COL_WIKI_ONWERS_OWNER_ID);
					ObjectType type = ObjectType.valueOf(rs.getString(V2_COL_WIKI_ONWERS_OBJECT_TYPE));
					if(ObjectType.ENTITY == type){
						owner = KeyFactory.keyToString(Long.parseLong(owner));
					}
					String wikiId = rs.getString(V2_COL_WIKI_ID);
					return new WikiPageKey(owner, type, wikiId);
				}
			}, id);
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("Cannot find a wiki page with id: "+wikiId);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_ALL_WIKIPAGES);
	}
	
	/**
	 * Retrieve a version of a markdown as a DBO
	 * @param wikiId
	 * @param markdownVersion
	 * @return
	 * @throws NotFoundException 
	 */
	private V2DBOWikiMarkdown getVersionOfMarkdown(Long wikiId, Long markdownVersion) throws NotFoundException{
		if(wikiId == null) throw new IllegalArgumentException("Th ID cannot be null");
		if(markdownVersion == null) throw new IllegalArgumentException("Markdown Version cannot be null");
		List<V2DBOWikiMarkdown> markdownResult = simpleJdbcTemplate.query(SQL_GET_WIKI_MARKDOWN_ATTACHMENT_ID_LIST, WIKI_MARKDOWN_ROW_MAPPER, wikiId, markdownVersion);
		if(markdownResult.size() > 1) throw new DatastoreException("More than one markdown version found with the wiki page id: " + wikiId + ", and version number: " + markdownVersion);
		if(markdownResult.size() < 1) throw new NotFoundException("No markdown version found for version: " + markdownVersion + ", for the wiki page with id: " + wikiId);
		return markdownResult.get(0);
	}
	
	/*
	 * Private methods to help process the attachment id list/column of the markdown database table
	 */
	
	/**
	 * Process the list into a mapping of file names to file handle ids
	 * @param attachmentIdList
	 * @return
	 */
	private Map<String, String> getFileNameAndHandleIdPairs(String attachmentIdList) {
		Map<String, String> fileNameToIdMap = new HashMap<String, String>();
		String[] attachments = attachmentIdList.split(",");

		for(String attachment: attachments) {
			String idNamePair = attachment.substring(1, attachment.length() - 1);
			String[] idName = idNamePair.split(":");
			//should only be length 2
			fileNameToIdMap.put(idName[1], idName[0]);
		}
		return fileNameToIdMap;
	}
	
	/**
	 * Process into a list of file handle ids
	 * @param attachmentIdList
	 * @return
	 */
	private List<String> getFileHandleIdsFromAttachmentList(String attachmentIdList) {
		List<String> fileHandleIds = new ArrayList<String>();
		String[] attachments = attachmentIdList.split(",");

		for(String attachment: attachments) {
			String idNamePair = attachment.substring(1, attachment.length() - 1);
			String[] idName = idNamePair.split(":");
			//should only be length 2
			fileHandleIds.add(idName[0]);
		}
		
		return fileHandleIds;
	}
	
	/**
	 * Return the file handle ids for the attachments for a given wiki
	 * @param pageDbo
	 * @return
	 * @throws NotFoundException 
	 */
	private List<String> getAttachmentFileHandleIds(V2DBOWikiPage pageDbo) throws NotFoundException {
		// Get the specific versioin of the wiki and extract from the attachment list column 
		V2DBOWikiMarkdown versionOfMarkdown = getVersionOfMarkdown(pageDbo.getId(), pageDbo.getMarkdownVersion());
		byte[] attachmentIdList = versionOfMarkdown.getAttachmentIdList();
		List<String> fileIds = new ArrayList<String>();
		if(attachmentIdList.length > 0) {
			try {
				fileIds = getFileHandleIdsFromAttachmentList(new String(attachmentIdList, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return fileIds;
	}
	
	/**
	 * Get a list of attachment DBOs for a given wiki
	 * @param pageDbo
	 * @return
	 * @throws NotFoundException 
	 */
	private List<V2DBOWikiAttachmentReservation> getAttachmentDbos(V2DBOWikiPage pageDbo) throws NotFoundException{
		if(pageDbo == null) throw new IllegalArgumentException("The Wiki page DBO is null");
		// Process which file handle ids this wiki needs
		List<String> fileHandleIds = getAttachmentFileHandleIds(pageDbo);
		List<V2DBOWikiAttachmentReservation> results = new ArrayList<V2DBOWikiAttachmentReservation>();
		// For each file handle id, query the attachment archive with the wiki id and file handle id
		for(String fileHandleId: fileHandleIds) {
			List<V2DBOWikiAttachmentReservation> attachmentList = simpleJdbcTemplate.query(SQL_SELECT_WIKI_ATTACHMENT, ATTACHMENT_ROW_MAPPER, pageDbo.getId(), fileHandleId);
			if(attachmentList.size() > 1) throw new DatastoreException("More than one attachment was found with the file handle id: " + fileHandleId + ", for the wiki page id: " + pageDbo.getId());
			if(attachmentList.size() < 1) throw new NotFoundException("No attachment was found for the file handle id: " + fileHandleId + ", for the wiki page id: " + pageDbo.getId());
			results.add(attachmentList.get(0));
		}
		return results;
	}
	
	private boolean doesExist(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		try{
			// Is this in the database.
			simpleJdbcTemplate.queryForLong(SQL_DOES_EXIST, id);
			return true;
		}catch(EmptyResultDataAccessException e){
			return false;
		}
	}
}
