package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.V2WikiPageMigrationDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.V2WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiOwner;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiPage;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Temporary DAO that interacts with the V2 WikiPage table for migration
 * @author hso
 *
 */
public class V2DBOWikiPageMigrationDAOImpl implements V2WikiPageMigrationDao {
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	private static final String SQL_SELECT_WIKI_ROOT_USING_OWNER_ID_AND_TYPE = "SELECT "+V2_COL_WIKI_ONWERS_ROOT_WIKI_ID+" FROM "+V2_TABLE_WIKI_OWNERS+" WHERE "+V2_COL_WIKI_ONWERS_OWNER_ID+" = ? AND "+V2_COL_WIKI_ONWERS_OBJECT_TYPE+" = ?";
	private static final String SQL_SELECT_WIKI_USING_ID_AND_ROOT = "SELECT * FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ? AND "+V2_COL_WIKI_ROOT_ID+" = ?";
	private static final String SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION = "SELECT * FROM "+V2_TABLE_WIKI_MARKDOWN+" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	private static final String SQL_DOES_EXIST = "SELECT "+V2_COL_WIKI_ID+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ?";
	private static final String SQL_SELECT_WIKI_ETAG = "SELECT "+V2_COL_WIKI_ETAG+" FROM "+V2_TABLE_WIKI_PAGE+" WHERE "+V2_COL_WIKI_ID+" = ?";
	
	private static final TableMapping<V2DBOWikiPage> WIKI_PAGE_ROW_MAPPER = new V2DBOWikiPage().getTableMapping();	
	private static final TableMapping<V2DBOWikiMarkdown> WIKI_MARKDOWN_ROW_MAPPER = new V2DBOWikiMarkdown().getTableMapping();
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public V2WikiPage create(V2WikiPage wikiPage, Map<String, FileHandle> fileNameToFileHandleMap, String ownerId, ObjectType ownerType, List<String> newFileHandleIds) throws NotFoundException {
		if(wikiPage == null) throw new IllegalArgumentException("wikiPage cannot be null");
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
		if(ownerType == null) throw new IllegalArgumentException("ownerType cannot be null");
		
		// Convert to a DBO
		V2DBOWikiPage dbo = V2WikiTranslationUtils.createDBOFromDTO(wikiPage);
		// Start markdown version at 0
		dbo.setMarkdownVersion(new Long(0));
		
		// Check for a cycle in parent/child relationship
		if(dbo.getParentId() != null && dbo.getParentId().equals(dbo.getId())) {
			throw new IllegalArgumentException("A wiki page cannot be its own parent.");
		}
		
		Long ownerIdLong = KeyFactory.stringToKey(ownerId);
		dbo = create(ownerType, dbo, ownerIdLong);
		
		long currentTime = System.currentTimeMillis();
		// Create the attachments with the current timestamp
		long timeStamp = (currentTime/1000)*1000;
		List<V2DBOWikiAttachmentReservation> attachments = V2WikiTranslationUtils.createDBOAttachmentReservationFromDTO(newFileHandleIds, dbo.getId(), timeStamp);
		// Save them to the attachments archive, update if there are duplicate file handle ids for this wiki
		if(attachments.size() > 0){
			String attachmentsInsertSql = DMLUtils.getBatchInsertOrUdpate(new V2DBOWikiAttachmentReservation().getTableMapping());
			createOrUpdateOnDuplicateBatch(attachments, attachmentsInsertSql);
		}
		
		// Create the markdown snapshot
		Long markdownFileHandleId = Long.parseLong(wikiPage.getMarkdownFileHandleId());
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameToFileHandleMap, dbo.getId(), markdownFileHandleId, wikiPage.getTitle());
		markdownDbo.setMarkdownVersion(new Long(0));
		markdownDbo.setModifiedOn(currentTime);
		markdownDbo.setModifiedBy(dbo.getModifiedBy());
		// Save this new version to the markdown DB, updates if there's already a markdown version 0 for this wiki
		String markdownInsertSql = DMLUtils.getBatchInsertOrUdpate(new V2DBOWikiMarkdown().getTableMapping());
		createOrUpdateOnDuplicate(markdownDbo, markdownInsertSql);
		
		// Send the create message
		transactionalMessenger.sendMessageAfterCommit(dbo, ChangeType.CREATE);
		
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
		// Save it to the DB, update if a wiki with this id exists already
		String insertSql = DMLUtils.getBatchInsertOrUdpate(new V2DBOWikiPage().getTableMapping());
		dbo = createOrUpdateOnDuplicate(dbo, insertSql);
		// If the parentID is null then this must be a root.
		if(dbo.getParentId() == null){
			// Set the root entry.
			createRootOwnerEntry(ownerIdLong, ownerType, dbo.getId());
		}
		return dbo;
	}
	
	/**
	 * Creates a database object; updates on duplicate key
	 * 
	 * With ON DUPLICATE KEY UPDATE, the affected-rows value per row is 1 if the row is inserted as new, and 2 if an existing row is updated.
	 * @param <T>
	 * @param toCreate
	 * @param insertSql
	 * @return
	 * @throws DatastoreException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	private <T extends DatabaseObject<T>> T createOrUpdateOnDuplicate(T toCreate, String insertSql) throws DatastoreException {
		if(toCreate == null) throw new IllegalArgumentException("The object cannot be null");
		SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(toCreate);
		try{
			int updatedCount = simpleJdbcTemplate.update(insertSql, namedParameters);
			if(updatedCount != 1 && updatedCount != 2) throw new DatastoreException("Failed to insert without error");
			return toCreate;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Creates a batch of database objects; updates on duplicate key
	 * With ON DUPLICATE KEY UPDATE, the affected-rows value per row is 1 if the row is inserted as new, and 2 if an existing row is updated.
	 * @param <T>
	 * @param batch
	 * @param insertSql
	 * @return
	 * @throws DatastoreException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	private <T extends DatabaseObject<T>> List<T> createOrUpdateOnDuplicateBatch(List<T> batch, String insertSql) throws DatastoreException {
		if(batch == null) throw new IllegalArgumentException("The batch cannot be null");
		if(batch.size() < 1) throw new IllegalArgumentException("There must be at least one item in the batch");

		SqlParameterSource[] namedParameters = new BeanPropertySqlParameterSource[batch.size()];
		for(int i=0; i<batch.size(); i++){
			namedParameters[i] = new BeanPropertySqlParameterSource(batch.get(i));
		}
		try{
			int[] updatedCountArray = simpleJdbcTemplate.batchUpdate(insertSql, namedParameters);
			for(int count: updatedCountArray){
				if(count != 1 && count != 2) throw new DatastoreException("Failed to insert without error");
			}
			return batch;
		}catch(DataIntegrityViolationException e){
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public boolean doesWikiExist(String wikiId) {
		if(wikiId == null) throw new IllegalArgumentException("Id cannot be null");
		try{
			// Is this in the database?
			simpleJdbcTemplate.queryForLong(SQL_DOES_EXIST, wikiId);
			return true;
		}catch(EmptyResultDataAccessException e){
			return false;
		}
	}
	
	@Override
	public String getWikiEtag(String wikiId) {
		if(wikiId == null) throw new IllegalArgumentException("Id cannot be null");
		try {
			String etag = simpleJdbcTemplate.queryForObject(SQL_SELECT_WIKI_ETAG, String.class, wikiId);
			return etag;
		} catch(EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	/*
	 * OTHER PRIVATE HELPER METHODS 
	 * 
	 */
	
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
		// Insert into owners table, updates if there's already an entry for this owner id and type
		String insertSql = DMLUtils.getBatchInsertOrUdpate(new V2DBOWikiOwner().getTableMapping());
		createOrUpdateOnDuplicate(ownerEntry, insertSql);
	}
	
	/**
	 * Gets a V2 WikiPage by its key
	 * @param key
	 * @return
	 * @throws NotFoundException
	 */
	private V2WikiPage get(WikiPageKey key) throws NotFoundException {
		// Get the Wikipage DBO.
		V2DBOWikiPage dbo = getWikiPageDBO(key);	
		// Now get the markdown
		V2DBOWikiMarkdown markdownDbo = getWikiMarkdownDBO(dbo.getId(), dbo.getMarkdownVersion()); 
		String listToString = V2WikiTranslationUtils.getStringFromByteArray(markdownDbo.getAttachmentIdList());
		List<String> fileHandleIds = createFileHandleIdsList(listToString);
		return V2WikiTranslationUtils.createDTOfromDBO(dbo, fileHandleIds, markdownDbo.getFileHandleId());
	}
	
	/**
	 * Parses the attachment list and returns a list of the file handle ids
	 * @param attachmentsList
	 * @return
	 */
	private List<String> createFileHandleIdsList(String attachmentsList) {
		List<String> fileHandleIds = new ArrayList<String>();
		if(attachmentsList != null) {
			// Process the list of attachments into a map for easy searching
			Map<String, String> fileNameToIdMap = V2WikiTranslationUtils.getFileNameAndHandleIdPairs(attachmentsList);
			for(String fileName: fileNameToIdMap.keySet()) {
				fileHandleIds.add(fileNameToIdMap.get(fileName));
			}
		}
		return fileHandleIds;
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
	
	public Long getRootWiki(String ownerId, ObjectType ownerType) throws NotFoundException {
		return getRootWiki(KeyFactory.stringToKey(ownerId), ownerType);
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
	
}
