package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_FILE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_ATTACHMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_PAGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiAttachment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiOwner;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiPage;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
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
 * The basic implementation of the WikiPageDao.
 * 
 * @author John
 *
 */
public class DBOWikiPageDaoImpl implements WikiPageDao {
	
	private static final String SQL_LOOKUP_WIKI_PAGE_KEY = "SELECT WO."+COL_WIKI_ONWERS_OWNER_ID+", WO."+COL_WIKI_ONWERS_OBJECT_TYPE+", WP."+COL_WIKI_ID+" FROM "+TABLE_WIKI_PAGE+" WP, "+TABLE_WIKI_OWNERS+" WO WHERE WP."+COL_WIKI_ROOT_ID+" = WO."+COL_WIKI_ONWERS_ROOT_WIKI_ID+" AND WP."+COL_WIKI_ID+" = ?";
	private static final String SQL_COUNT_ALL_WIKIPAGES = "SELECT COUNT(*) FROM "+TABLE_WIKI_PAGE;
	private static final String SQL_FIND_WIKI_ATTACHMENT_BY_NAME = "SELECT WA."+COL_WIKI_ATTACHMENT_FILE_HANDLE_ID+" FROM "+TABLE_WIKI_OWNERS+" WO, "+TABLE_WIKI_PAGE+" WP, "+TABLE_WIKI_ATTACHMENT+" WA WHERE WO."+COL_WIKI_ONWERS_OWNER_ID+" = ? AND WO."+COL_WIKI_ONWERS_OBJECT_TYPE+" = ? AND WO."+COL_WIKI_ONWERS_ROOT_WIKI_ID+" = WP."+COL_WIKI_ROOT_ID+" AND WP."+COL_WIKI_ID+" = ? AND WP."+COL_WIKI_ID+" = WA."+COL_WIKI_ATTACHMENT_ID+" AND WA."+COL_WIKI_ATTACHMENT_FILE_NAME+"= ?";
	private static final String SQL_SELECT_WIKI_ROOT_USING_OWNER_ID_AND_TYPE = "SELECT "+COL_WIKI_ONWERS_ROOT_WIKI_ID+" FROM "+TABLE_WIKI_OWNERS+" WHERE "+COL_WIKI_ONWERS_OWNER_ID+" = ? AND "+COL_WIKI_ONWERS_OBJECT_TYPE+" = ?";
	private static final String SQL_SELECT_ATTACHMENT_IDS_USING_ROOT_AND_ID = "SELECT ATT."+COL_WIKI_ATTACHMENT_FILE_HANDLE_ID+" FROM "+TABLE_WIKI_PAGE+" PAG, "+TABLE_WIKI_ATTACHMENT+" ATT WHERE PAG."+COL_WIKI_ID+" = ATT."+COL_WIKI_ATTACHMENT_ID+" AND PAG."+COL_WIKI_ROOT_ID+" = ? AND PAG."+COL_WIKI_ID+" = ? ORDER BY ATT."+COL_WIKI_ATTACHMENT_FILE_HANDLE_ID;
	private static final String SQL_LOCK_FOR_UPDATE = "SELECT "+COL_WIKI_ETAG+" FROM "+TABLE_WIKI_PAGE+" WHERE "+COL_WIKI_ID+" = ? FOR UPDATE";
	private static final String SQL_DELETE_USING_ID_AND_ROOT = "DELETE FROM "+TABLE_WIKI_PAGE+" WHERE "+COL_WIKI_ID+" = ? AND "+COL_WIKI_ROOT_ID+" = ?";
	private static final String SQL_SELECT_WIKI_USING_ID_AND_ROOT = "SELECT * FROM "+TABLE_WIKI_PAGE+" WHERE "+COL_WIKI_ID+" = ? AND "+COL_WIKI_ROOT_ID+" = ?";
	private static final String WIKI_HEADER_SELECT = COL_WIKI_ID+", "+COL_WIKI_TITLE+", "+COL_WIKI_PARENT_ID;
	private static final String SQL_SELECT_CHILDREN_HEADERS = "SELECT "+WIKI_HEADER_SELECT+" FROM "+TABLE_WIKI_PAGE+" WHERE "+COL_WIKI_ROOT_ID+" = ? ORDER BY "+COL_WIKI_PARENT_ID+", "+COL_WIKI_TITLE;
	private static final String SQL_DELETE_ATTACHMENT_BY_PRIMARY_KEY = "DELETE FROM "+TABLE_WIKI_ATTACHMENT+" WHERE "+COL_WIKI_ATTACHMENT_ID+" = ? AND "+COL_WIKI_ATTACHMENT_FILE_HANDLE_ID+" = ?";
	private static final String SQL_GET_ALL_WIKI_ATTACHMENTS = "SELECT * FROM "+TABLE_WIKI_ATTACHMENT+" WHERE "+COL_WIKI_ATTACHMENT_ID+" = ? ORDER BY "+COL_WIKI_ATTACHMENT_FILE_HANDLE_ID;
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
		
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	/**
	 * Used to detect if a wiki object already exists.
	 */
	private static final String SQL_DOES_EXIST = "SELECT "+COL_WIKI_ID+" FROM "+TABLE_WIKI_PAGE+" WHERE "+COL_WIKI_ID+" = ?";
	
	private static final TableMapping<DBOWikiAttachment> ATTACHMENT_ROW_MAPPER = new DBOWikiAttachment().getTableMapping();
	private static final TableMapping<DBOWikiPage> WIKI_PAGE_ROW_MAPPER = new DBOWikiPage().getTableMapping();
	/**
	 * Maps to a simple wiki header.
	 */
	private static final RowMapper<WikiHeader> WIKI_HEADER_ROW_MAPPER = new RowMapper<WikiHeader>() {
		@Override
		public WikiHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
			WikiHeader header = new WikiHeader();
			header.setId(""+rs.getLong(COL_WIKI_ID));
			header.setTitle(rs.getString(COL_WIKI_TITLE));
			header.setParentId(rs.getString(COL_WIKI_PARENT_ID));
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
		DBOWikiPage dbo = WikiTranslationUtils.createDBOFromDTO(wikiPage);
		dbo.setCreatedOn(System.currentTimeMillis());
		dbo.setModifiedOn(dbo.getCreatedOn());
		
		if(wikiPage.getId() == null){
			dbo.setId(idGenerator.generateNewId(TYPE.WIKI_ID));
		}else{
			// If an id was provided then it must not exist
			if(doesExist(wikiPage.getId())) throw new IllegalArgumentException("A wiki page already exists with ID: "+wikiPage.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(new Long(wikiPage.getId()), TYPE.WIKI_ID);
		}
		// When we migrate we keep the original etag.  When it is null we set it.
		if(dbo.getEtag() == null){
			dbo.setEtag(UUID.randomUUID().toString());
		}
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		dbo = create(ownerType, dbo, ownerIdLong);
		// Create the attachments
		List<DBOWikiAttachment> attachments = WikiTranslationUtils.createDBOAttachmentsFromDTO(fileNameToFileHandleMap, dbo.getId());
		// Save them to the DB
		if(attachments.size() >0){
			basicDao.createBatch(attachments);
		}
		// Send the create message
		transactionalMessenger.sendMessageAfterCommit(dbo, ChangeType.CREATE);
		try {
			return get(new WikiPageKey(ownerId, ownerType, dbo.getId().toString()));
		} catch (NotFoundException e) {
			// This should not occur.
			throw new RuntimeException(e);
		}
	}

	private DBOWikiPage create(ObjectType ownerType, DBOWikiPage dbo,
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
	private void setRoot(Long ownerId, ObjectType ownerType, DBOWikiPage dbo) throws NotFoundException {
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
	 * Get the root wiki page ID.
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws NotFoundException
	 */
	public Long getRootWiki(String ownerId, ObjectType ownerType) throws NotFoundException {
		return getRootWiki(KeyFactory.stringToKey(ownerId), ownerType);
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
		DBOWikiOwner ownerEntry = new DBOWikiOwner();
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, boolean keepEtag) throws NotFoundException {
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileHandleMap cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		if(wikiPage.getId() == null) throw new IllegalArgumentException("wikiPage.getID() cannot be null");
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		// does this page exist?
		if(!doesExist(wikiPage.getId())) throw new NotFoundException("No WikiPage exists with id: "+wikiPage.getId());
		Long wikiId = new Long(wikiPage.getId());
		// First we need to delete the attachments.
		DBOWikiPage newDBO = WikiTranslationUtils.createDBOFromDTO(wikiPage);
		List<DBOWikiAttachment> newAttachments = WikiTranslationUtils.createDBOAttachmentsFromDTO(fileNameToFileHandleMap, wikiId);
		replaceAttachments(wikiId, newAttachments);
		// Now update the wiki
		if(!keepEtag){
			// We keep the etag for migration scenarios.
			newDBO.setEtag(UUID.randomUUID().toString());
		}
		// Set the modified on to current.
		newDBO.setModifiedOn(System.currentTimeMillis());
		update(ownerType, ownerIdLong, newDBO);
		
		// Send the change message
		transactionalMessenger.sendMessageAfterCommit(newDBO, ChangeType.UPDATE);
		
		// Return the results.
		return get(new WikiPageKey(ownerId, ownerType, wikiPage.getId().toString()));
	}

	private void update(ObjectType ownerType, Long ownerIdLong,
			DBOWikiPage newDBO) throws NotFoundException {
		// Set the root
		setRoot(ownerIdLong, ownerType, newDBO);
		// Update
		basicDao.update(newDBO);
	}

	@Override
	public WikiPage get(WikiPageKey key) throws NotFoundException{
		// Get the DBO.
		DBOWikiPage dbo = getWikiPageDBO(key);
		// Now get the attachments
		List<DBOWikiAttachment> attachments = getAttachments(dbo.getId());
		return WikiTranslationUtils.createDTOfromDBO(dbo, attachments);
	}

	/**
	 * Get the DBOWikiPage using its key.
	 * @param key
	 * @return
	 * @throws NotFoundException
	 */
	private DBOWikiPage getWikiPageDBO(WikiPageKey key) throws NotFoundException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// In order to access a wiki you must know its owner.
		// If the root does not exist then the wiki does not exist.
		Long root = getRootWiki(key.getOwnerObjectId(), key.getOwnerObjectType());
		// We use the root in addition to the primary key (id) to enforce they are not out of sych.
		List<DBOWikiPage> list = simpleJdbcTemplate.query(SQL_SELECT_WIKI_USING_ID_AND_ROOT, WIKI_PAGE_ROW_MAPPER, new Long(key.getWikiPageId()), root);
		if(list.size() > 1) throw new DatastoreException("More than one Wiki page found with the id: "+key.getWikiPageId());
		if(list.size() < 1) throw new NotFoundException("No wiki page found with id: "+key.getWikiPageId());
		return list.get(0);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(WikiPageKey key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// In order to access a wiki you must know its owner.
		// If the root does not exist then the wiki does not exist.
		try{
			Long root = getRootWiki(key.getOwnerObjectId(), key.getOwnerObjectType());
			// Delete the wiki using both the root and the id 
			simpleJdbcTemplate.update(SQL_DELETE_USING_ID_AND_ROOT, new Long(key.getWikiPageId()), root);
		}catch(NotFoundException e){
			// Nothing to do if the wiki does not exist.
		}
	}
	
	/**
	 * Get the attachments for a given wiki
	 * @param id
	 * @return
	 */
	private List<DBOWikiAttachment> getAttachments(Long id){
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		return simpleJdbcTemplate.query(SQL_GET_ALL_WIKI_ATTACHMENTS, ATTACHMENT_ROW_MAPPER, id);
	}
	
	/**
	 * Replace all of the attachments.
	 */
	private void replaceAttachments(Long wikiId, List<DBOWikiAttachment> newAttachments){
		// First we get the existing attachments, and delete each one.
		// Note: Deleting the attachments using 'DELETE FROM WIKI_ATTACHMENTS WHERE WIKI_ID = ?'
		// will result in MySql gap locks which cause needless deadlock.  To avoid this we delete each
		// attachment by its primary key;
		List<DBOWikiAttachment> current = getAttachments(wikiId);
		for(DBOWikiAttachment oldAtt: current){
			simpleJdbcTemplate.update(SQL_DELETE_ATTACHMENT_BY_PRIMARY_KEY, oldAtt.getWikiId(), oldAtt.getFileHandleId());
		}
		// Add back the new attachments
		if(newAttachments.size() > 0){
			basicDao.createBatch(newAttachments);
		}
	}
	
	@Override
	public List<WikiHeader> getHeaderTree(String ownerId, ObjectType ownerType) throws DatastoreException, NotFoundException {
		// First look up the root for this owner
		Long root = getRootWiki(ownerId, ownerType);
		// Now use the root to the the full tree
		return simpleJdbcTemplate.query(SQL_SELECT_CHILDREN_HEADERS, WIKI_HEADER_ROW_MAPPER, root);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String lockForUpdate(String wikiId) {
		// Lock the wiki row and return current Etag.
		return simpleJdbcTemplate.queryForObject(SQL_LOCK_FOR_UPDATE, String.class, new Long(wikiId));
	}

	@Override
	public List<String> getWikiFileHandleIds(WikiPageKey key) throws NotFoundException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// First filter by root to ensure only handles belonging to the owner are returned.
		Long rootId = getRootWiki(key.getOwnerObjectId(), key.getOwnerObjectType());
		return simpleJdbcTemplate.query(SQL_SELECT_ATTACHMENT_IDS_USING_ROOT_AND_ID, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(1);
			}
		}, rootId, key.getWikiPageId());
	}

	@Override
	public String getWikiAttachmentFileHandleForFileName(WikiPageKey key, String fileName) throws NotFoundException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(fileName == null) throw new IllegalArgumentException("fileName cannot be null");
		Long ownerId = KeyFactory.stringToKey(key.getOwnerObjectId());
		try{
			return ""+simpleJdbcTemplate.queryForLong(SQL_FIND_WIKI_ATTACHMENT_BY_NAME, ownerId, key.getOwnerObjectType().name(), key.getWikiPageId(), fileName);
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("Cannot find a wiki attachment for OwnerID: "+key.getOwnerObjectId()+", ObjectType: "+key.getOwnerObjectType()+", WikiPageId: "+key.getWikiPageId()+", fileName: "+fileName);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SQL_COUNT_ALL_WIKIPAGES);
	}

	@Override
	public WikiPageKey lookupWikiKey(String wikiId) throws NotFoundException {
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null");
		long id = Long.parseLong(wikiId);
		try{
			return this.simpleJdbcTemplate.queryForObject(SQL_LOOKUP_WIKI_PAGE_KEY, new RowMapper<WikiPageKey>() {
				@Override
				public WikiPageKey mapRow(ResultSet rs, int rowNum) throws SQLException {
					String owner = rs.getString(COL_WIKI_ONWERS_OWNER_ID);
					ObjectType type = ObjectType.valueOf(rs.getString(COL_WIKI_ONWERS_OBJECT_TYPE));
					if(ObjectType.ENTITY == type){
						owner = KeyFactory.keyToString(Long.parseLong(owner));
					}
					String wikiId = rs.getString(COL_WIKI_ID);
					return new WikiPageKey(owner, type, wikiId);
				}
			}, id);
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException("Cannot find a wiki page with id: "+wikiId);
		}
	}

}
