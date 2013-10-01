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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dao.V2WikiPageDao;
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
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.WikiPage;
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
	private static final String SQL_GET_ALL_WIKI_ATTACHMENTS = "SELECT * FROM "+V2_TABLE_WIKI_ATTACHMENT_RESERVATION+" WHERE "+V2_COL_WIKI_ATTACHMENT_RESERVATION_ID+" = ? AND "+V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID+" = ?";
	private static final String SQL_GET_WIKI_MARKDOWN_ATTACHMENT_ID_LIST = "SELECT * FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	private static final String SQL_DELETE_USING_ID_AND_ROOT = "DELETE FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String WIKI_HEADER_SELECT = V2_COL_WIKI_ID+", "+V2_COL_WIKI_TITLE+", "+V2_COL_WIKI_PARENT_ID;
	private static final String SQL_SELECT_CHILDREN_HEADERS = "SELECT "+WIKI_HEADER_SELECT+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ROOT_ID+" = ? ORDER BY "+V2_COL_WIKI_PARENT_ID+", "+V2_COL_WIKI_TITLE;
	private static final String SQL_LOCK_FOR_UPDATE = "SELECT "+V2_COL_WIKI_ETAG+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? FOR UPDATE";
	private static final String SQL_COUNT_ALL_WIKIPAGES = "SELECT COUNT(*) FROM "+V2_TABLE_WIKI_PAGE;
	
	private static final TableMapping<V2DBOWikiAttachmentReservation> ATTACHMENT_ROW_MAPPER = new V2DBOWikiAttachmentReservation().getTableMapping();
	private static final TableMapping<V2DBOWikiMarkdown> WIKI_MARKDOWN_ROW_MAPPER = new V2DBOWikiMarkdown().getTableMapping();
	private static final TableMapping<V2DBOWikiPage> WIKI_PAGE_ROW_MAPPER = new V2DBOWikiPage().getTableMapping();	
	
	/**
	 * Maps to a simple wiki header.
	 */
	private static final RowMapper<WikiHeader> WIKI_HEADER_ROW_MAPPER = new RowMapper<WikiHeader>() {
		@Override
		public WikiHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
			WikiHeader header = new WikiHeader();
			header.setId(""+rs.getLong(V2_COL_WIKI_ID));
			header.setTitle(rs.getString(V2_COL_WIKI_TITLE));
			header.setParentId(rs.getString(V2_COL_WIKI_PARENT_ID));
			return header;
		}
	};

	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage create(WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType) throws NotFoundException {
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
		// Save them to the DB
		if(attachments.size() > 0){
			basicDao.createBatch(attachments, false);
		}
		
		// Create the markdown snapshot
		Long markdownFileHandleId = Long.parseLong(wikiPage.getMarkdownFileHandleId());
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameToFileHandleMap, dbo.getId(), markdownFileHandleId);
		markdownDbo.setMarkdownVersion(new Long(0));
		markdownDbo.setModifiedOn(currentTime);
		markdownDbo.setModifiedBy(dbo.getModifiedBy());
		// Save this as an entry of the markdown DB
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
	
	/**
	 * Validate the owner and the root. An owner can only have one root.
	 * 
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
	 * @return
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
	 * Throws IllegalArgumentException if a root wiki already exists for the given owner.
	 * @param ownerId
	 * @param ownerType
	 * @param rootWikiId
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
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(WikiPage wikiPage,
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

		// Update this particular wiki's entry in the WikiPage database (update version)
		V2DBOWikiPage newDbo = V2WikiTranslationUtils.createDBOFromDTO(wikiPage);
		// Now update the wiki
		if(!keepEtag){
			// We keep the etag for migration scenarios.
			newDbo.setEtag(UUID.randomUUID().toString());
		}
		// Set the modified on to current.
		newDbo.setModifiedOn(currentTime);
		newDbo.setMarkdownVersion(incrementedVersion);
		
		// Update this wiki's entry in wiki page table
		update(ownerType, ownerIdLong, newDbo);

		// Create new markdown snapshot as a new version
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
		
		// Save them to the DB
		if(attachments.size() > 0){
			basicDao.createBatchOrUpdate(attachments);
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
	
	@Override
	public WikiPage get(WikiPageKey key) throws NotFoundException {
		// Get the DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);
		// Now get the attachments
		List<V2DBOWikiAttachmentReservation> attachments = getAttachmentDbos(dbo.getId(), dbo.getMarkdownVersion());
		return V2WikiTranslationUtils.createDTOfromDBO(dbo, attachments);
	}

	/**
	 * Get the DBOWikiPage using its key.
	 * @param key
	 * @return
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
	 * @return
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
	 * Retrieve a version of a markdown
	 * @param wikiId
	 * @param markdownVersion
	 * @return
	 */
	private V2DBOWikiMarkdown getMarkdownVersion(Long wikiId, Long markdownVersion) throws NotFoundException{
		if(wikiId == null) throw new IllegalArgumentException("Th ID cannot be null");
		if(markdownVersion == null) throw new IllegalArgumentException("Markdown Version cannot be null");
		// get attachmentIdList col value from markdown db by using the wikiId and markdownVersion
		List<V2DBOWikiMarkdown> markdownResult = simpleJdbcTemplate.query(SQL_GET_WIKI_MARKDOWN_ATTACHMENT_ID_LIST, WIKI_MARKDOWN_ROW_MAPPER, wikiId, markdownVersion);
		if(markdownResult.size() > 1) throw new DatastoreException("More than one markdown version found with the wiki page id: " + wikiId + ", and version number: " + markdownVersion);
		if(markdownResult.size() < 1) throw new NotFoundException("No markdown version found for version: " + markdownVersion + ", for the wiki page with id: " + wikiId);
		return markdownResult.get(0);
	}
	
	/**
	 * Process the file handle id to file name sequence into an accessible mapping
	 * @param attachmentIdList
	 * @return
	 */
	private Map<String, String> processMarkdownAttachmentIdList(String attachmentIdList) {
		Map<String, String> fileIdToNameMap = new HashMap<String, String>();
		String[] attachments = attachmentIdList.split(",");
		for(String attachment: attachments) {
			String idNamePair = attachment.substring(1, attachment.length() - 1);
			String[] idName = idNamePair.split(":");
			//should only be length 2
			fileIdToNameMap.put(idName[0], idName[1]);
		}
		return fileIdToNameMap;
	}
	
	/**
	 * Get the file handle ids for the attachments for a given wiki
	 * @param wikiId
	 * @param markdownVersion
	 * @return
	 * @throws NotFoundException 
	 */
	private List<String> getAttachmentFileHandleIds(Long wikiId, Long markdownVersion) throws NotFoundException {
		// make list of filehandleids
		List<String> fileHandleIds = new ArrayList<String>();
		String attachmentIdList = getMarkdownVersion(wikiId, markdownVersion).getAttachmentIdList();
		Map<String, String> fileIdToNameMap = processMarkdownAttachmentIdList(attachmentIdList);
		for(String fileHandleId: fileIdToNameMap.keySet()) {
			fileHandleIds.add(fileHandleId);
		}
		return fileHandleIds;
	}
	
	/**
	 * Get a list of attachment dbos for a given wiki
	 * @param wikiId
	 * @param markdownVersion
	 * @return
	 */
	private List<V2DBOWikiAttachmentReservation> getAttachmentDbos(Long wikiId, Long markdownVersion) throws NotFoundException{
		if(wikiId == null) throw new IllegalArgumentException("Th ID cannot be null");
		if(markdownVersion == null) throw new IllegalArgumentException("Markdown Version cannot be null");
		List<String> fileHandleIds = getAttachmentFileHandleIds(wikiId, markdownVersion);
		// make new result list of DBOWikiAttachmentReservation
		List<V2DBOWikiAttachmentReservation> results = new ArrayList<V2DBOWikiAttachmentReservation>();
		// for each filehandleid, query for that filehandleid with the given wiki id, add this to the result list
		for(String fileHandleId: fileHandleIds) {
			//query for each fileHandleId
			List<V2DBOWikiAttachmentReservation> attachmentList = simpleJdbcTemplate.query(SQL_GET_ALL_WIKI_ATTACHMENTS, ATTACHMENT_ROW_MAPPER, wikiId, fileHandleId);
			if(attachmentList.size() > 1) throw new DatastoreException("More than one attachment was found with the file handle id: " + fileHandleId + ", for the wiki page id: " + wikiId);
			if(attachmentList.size() < 1) throw new NotFoundException("No attachment was found for the file handle id: " + fileHandleId + ", for the wiki page id: " + wikiId);
			results.add(attachmentList.get(0));
		}
		return results;
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
	public List<WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType)
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
		return getAttachmentFileHandleIds(dbo.getId(), dbo.getMarkdownVersion());
	}

	@Override
	public String getWikiAttachmentFileHandleForFileName(WikiPageKey key,
			String fileName) throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		// Get the DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);
		String attachmentIdList = getMarkdownVersion(dbo.getId(), dbo.getMarkdownVersion()).getAttachmentIdList();
		Map<String, String> fileIdToNameMap = processMarkdownAttachmentIdList(attachmentIdList);
		for(String fileHandleId: fileIdToNameMap.keySet()) {
			if(fileIdToNameMap.get(fileHandleId).equals(fileName)) {
				return fileHandleId;
			}
		}
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

}
