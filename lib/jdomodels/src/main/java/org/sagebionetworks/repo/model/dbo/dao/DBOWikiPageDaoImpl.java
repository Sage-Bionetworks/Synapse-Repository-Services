package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_ATTACHMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_PAGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiAttachment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiPage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOWikiPageDaoImpl implements WikiPageDao {
	
	private static final String WIKI_HEADER_SELECT = COL_WIKI_ID+", "+COL_WIKI_TITLE;
	private static final String SQL_SELECT_CHILDREN_HEADERS = "SELECT "+WIKI_HEADER_SELECT+" FROM "+TABLE_WIKI_PAGE+" WHERE "+COL_WIKI_PARENT_ID+" = ? ORDER BY "+COL_WIKI_TITLE;
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
	
	/**
	 * Maps to a simple wiki header.
	 */
	private static final RowMapper<WikiHeader> WIKI_HEADER_ROW_MAPPER = new RowMapper<WikiHeader>() {
		@Override
		public WikiHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
			WikiHeader header = new WikiHeader();
			header.setId(""+rs.getLong(COL_WIKI_ID));
			header.setTitle(rs.getString(COL_WIKI_TITLE));
			return header;
		}
	};

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage create(WikiPage toCreate) {
		if(toCreate == null) throw new IllegalArgumentException("FileMetadata cannot be null");
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
		// Save it to the DB
		dbo = basicDao.createNew(dbo);
		// Create the attachments
		List<DBOWikiAttachment> attachments = WikiTranslationUtils.createDBOAttachmentsFromDTO(toCreate, dbo.getId());
		// Save them to the DB
		if(attachments.size() >0){
			basicDao.createBatch(attachments);
		}
		// Send the create message
		tagMessenger.sendMessage(dbo.getId().toString(), dbo.getEtag(), ObjectType.WIKI, ChangeType.CREATE);
		try {
			return get(dbo.getId().toString());
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(WikiPage toUpdate, boolean keepEtag) throws NotFoundException {
		if(toUpdate == null) throw new IllegalArgumentException("WikiPage cannot be null");
		if(toUpdate.getId() == null) throw new IllegalArgumentException("WikiPage.getID() cannot be null");
		// does this page exist?
		if(!doesExist(toUpdate.getId())) throw new NotFoundException("No WikiPage exists with id: "+toUpdate.getId());
		Long wikiId = new Long(toUpdate.getId());
		// First we need to delete the attachments.
		DBOWikiPage newDBO = WikiTranslationUtils.createDBOFromDTO(toUpdate);
		List<DBOWikiAttachment> newAttachments = WikiTranslationUtils.createDBOAttachmentsFromDTO(toUpdate, wikiId);
		replaceAttachmetns(wikiId, newAttachments);
		// Now update the wiki
		if(!keepEtag){
			// We keep the etag for migration scenarios.
			newDBO.setEtag(UUID.randomUUID().toString());
		}
		// Set the modified on to current.
		newDBO.setModifiedOn(System.currentTimeMillis());
		// Update
		basicDao.update(newDBO);
		// Send the change message
		tagMessenger.sendMessage(newDBO.getId().toString(), newDBO.getEtag(), ObjectType.WIKI, ChangeType.UPDATE);
		// Return the results.
		return get(toUpdate.getId());
	}

	@Override
	public WikiPage get(String id) throws NotFoundException{
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		DBOWikiPage dbo = basicDao.getObjectById(DBOWikiPage.class, param);
		// Now get the attachments
		List<DBOWikiAttachment> attachments = getAttachments(dbo.getId());
		return WikiTranslationUtils.createDTOfromDBO(dbo, attachments);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_FILES_ID.toLowerCase(), id);
		// Delete the object
		basicDao.deleteObjectById(DBOWikiPage.class, param);
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
	private void replaceAttachmetns(Long wikiId, List<DBOWikiAttachment> newAttachments){
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
	public List<WikiHeader> getChildrenHeaders(String parentId) {
		if(parentId == null) throw new IllegalArgumentException("ID cannot be null");
		return simpleJdbcTemplate.query(SQL_SELECT_CHILDREN_HEADERS, WIKI_HEADER_ROW_MAPPER, parentId);
	}
}
