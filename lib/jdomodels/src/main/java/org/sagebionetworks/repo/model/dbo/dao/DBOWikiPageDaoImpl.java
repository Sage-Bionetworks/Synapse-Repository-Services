package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
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
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiAttachment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiOwner;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiPage;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOWikiPageDaoImpl implements WikiPageDao {
	
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
	private TagMessenger tagMessenger;
		
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
	public WikiPage create(WikiPage toCreate, String ownerId, ObjectType ownerType) throws NotFoundException {
		if(toCreate == null) throw new IllegalArgumentException("FileMetadata cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("OnwerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("OwnerType cannot be null");
		
		// Convert to a DBO
		DBOWikiPage dbo = WikiTranslationUtils.createDBOFromDTO(toCreate);
		dbo.setCreatedOn(System.currentTimeMillis());
		dbo.setModifiedOn(dbo.getCreatedOn());
		
		if(toCreate.getId() == null){
			dbo.setId(idGenerator.generateNewId(TYPE.WIKI_ID));
		}else{
			// If an id was provided then it must not exist
			if(doesExist(toCreate.getId())) throw new IllegalArgumentException("A wiki page already exists with ID: "+toCreate.getId());
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(new Long(toCreate.getId()), TYPE.WIKI_ID);
		}
		// When we migrate we keep the original etag.  When it is null we set it.
		if(dbo.getEtag() == null){
			dbo.setEtag(UUID.randomUUID().toString());
		}
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		// If the parentID is null then this is a root wiki
		setRoot(ownerIdLong, ownerType, dbo);
		// Save it to the DB
		dbo = basicDao.createNew(dbo);
		// If the parentID is null then this must be a root.
		if(dbo.getParentId() == null){
			// Set the root entry.
			createRootOwnerEntry(ownerIdLong, ownerType, dbo.getId());
		}
		// Create the attachments
		List<DBOWikiAttachment> attachments = WikiTranslationUtils.createDBOAttachmentsFromDTO(toCreate, dbo.getId());
		// Save them to the DB
		if(attachments.size() >0){
			basicDao.createBatch(attachments);
		}
		// Send the create message
		tagMessenger.sendMessage(dbo.getId().toString(), dbo.getEtag(), ObjectType.WIKI, ChangeType.CREATE);
		try {
			return get(new WikiPageKey(ownerId, ownerType, dbo.getId().toString()));
		} catch (NotFoundException e) {
			// This should not occur.
			throw new RuntimeException(e);
		}
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
	
	private Long getRootWiki(String ownerId, ObjectType ownerType) throws NotFoundException {
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
		}catch(DatastoreException e){
			throw new IllegalArgumentException("A root wiki already exists for ownerId: "+ownerId+" and ownerType: "+ownerType);
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
	public WikiPage updateWikiPage(WikiPage toUpdate, String ownerId, ObjectType ownerType, boolean keepEtag) throws NotFoundException {
		if(toUpdate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(toUpdate.getId() == null) throw new IllegalArgumentException("WikiPage.getID() cannot be null");
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		// does this page exist?
		if(!doesExist(toUpdate.getId())) throw new NotFoundException("No WikiPage exists with id: "+toUpdate.getId());
		Long wikiId = new Long(toUpdate.getId());
		// First we need to delete the attachments.
		DBOWikiPage newDBO = WikiTranslationUtils.createDBOFromDTO(toUpdate);
		List<DBOWikiAttachment> newAttachments = WikiTranslationUtils.createDBOAttachmentsFromDTO(toUpdate, wikiId);
		replaceAttachments(wikiId, newAttachments);
		// Now update the wiki
		if(!keepEtag){
			// We keep the etag for migration scenarios.
			newDBO.setEtag(UUID.randomUUID().toString());
		}
		// Set the modified on to current.
		newDBO.setModifiedOn(System.currentTimeMillis());
		// Set the root
		setRoot(ownerIdLong, ownerType, newDBO);
		// Update
		basicDao.update(newDBO);
		// Send the change message
		tagMessenger.sendMessage(newDBO.getId().toString(), newDBO.getEtag(), ObjectType.WIKI, ChangeType.UPDATE);
		// Return the results.
		return get(new WikiPageKey(ownerId, ownerType, toUpdate.getId().toString()));
	}

	@Override
	public WikiPage get(WikiPageKey key) throws NotFoundException{
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// In order to access a wiki you must know its owner.
		// If the root does not exist then the wiki does not exist.
		Long root = getRootWiki(key.getOwnerObjectId(), key.getOwnerObjectType());
		// We use the root in addition to the primary key (id) to enforce they are not out of sych.
		List<DBOWikiPage> list = simpleJdbcTemplate.query(SQL_SELECT_WIKI_USING_ID_AND_ROOT, WIKI_PAGE_ROW_MAPPER, new Long(key.getWikiPageId()), root);
		if(list.size() > 1) throw new DatastoreException("More than one Wiki page found with the id: "+key.getWikiPageId());
		if(list.size() < 1) throw new NotFoundException("No wiki page found with id: "+key.getWikiPageId());
		DBOWikiPage dbo = list.get(0);
		// Now get the attachments
		List<DBOWikiAttachment> attachments = getAttachments(dbo.getId());
		return WikiTranslationUtils.createDTOfromDBO(dbo, attachments);
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
}
