package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_TITLE;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.V2WikiPageMigrationDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Temporary DAO that interacts with the V1 WikiPage table for migration
 * @author hso
 *
 */
public class DBOWikiMigrationDAO {
	@Autowired
	private WikiPageDao wikiPageDao;
	@Autowired
	private V2WikiPageMigrationDao v2WikiPageMigrationDao;
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	static private Log log = LogFactory.getLog(DBOWikiMigrationDAO.class);
	private static final String SELECT_WIKI_PAGES = "SELECT * FROM " + TABLE_WIKI_PAGE + " LIMIT ?, ?";
	private static final String SELECT_WIKI_PAGE = "SELECT * FROM " + TABLE_WIKI_PAGE + " WHERE " + COL_WIKI_ID + " = ?";
	private static final String SELECT_COUNT_OF_WIKIS = "SELECT COUNT(" + COL_WIKI_ID + ") FROM " + TABLE_WIKI_PAGE;;
	
	private static final RowMapper<WikiPage> wikiPageRowMapper = new RowMapper<WikiPage>() {
		@Override
		public WikiPage mapRow(ResultSet rs, int rowNum) throws SQLException {
			WikiPage wiki = new WikiPage();
			wiki.setId(rs.getString(COL_WIKI_ID));
			wiki.setEtag(rs.getString(COL_WIKI_ETAG));
			wiki.setTitle(rs.getString(COL_WIKI_TITLE));
			wiki.setCreatedBy(rs.getString(COL_WIKI_CREATED_BY));
			wiki.setCreatedOn(new Date(rs.getLong(COL_WIKI_CREATED_ON)));
			wiki.setModifiedBy(rs.getString(COL_WIKI_MODIFIED_BY));
			wiki.setModifiedOn(new Date(rs.getLong(COL_WIKI_MODIFIED_ON)));
			wiki.setParentWikiId(rs.getString(COL_WIKI_PARENT_ID));
			if(rs.wasNull()){
				wiki.setParentWikiId(null);
			}
			java.sql.Blob blob = rs.getBlob(COL_WIKI_MARKDOWN);
			
			if(blob != null){
				byte[] markdownInBytes = blob.getBytes(1, (int) blob.length());
				String markdownAsString;
				try {
					markdownAsString = new String(markdownInBytes, "UTF-8");	
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				wiki.setMarkdown(markdownAsString);
			}
			return wiki;
		}	
	};
	
	/**
	 * Retrieves requested WikiPages from the WikiPage table 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 */
	public List<WikiPage> getWikiPages(long limit, long offset) throws NotFoundException {
		List<WikiPage> results = simpleJdbcTemplate.query(SELECT_WIKI_PAGES, wikiPageRowMapper, offset, limit);
		List<WikiPage> completeWikiPages = new ArrayList<WikiPage>();
		for(WikiPage page: results) {
			WikiPageKey key = wikiPageDao.lookupWikiKey(page.getId());
			List<String> ids = wikiPageDao.getWikiFileHandleIds(key);
			page.setAttachmentFileHandleIds(ids);
			completeWikiPages.add(page);
		}
		return completeWikiPages;
	}
	
	/**
	 * Returns the wiki page with the given id from the V1 DB
	 * @param wikiId
	 * @return
	 * @throws NotFoundException
	 */
	public WikiPage getWikiPage(String wikiId) throws NotFoundException {
		List<WikiPage> list = simpleJdbcTemplate.query(SELECT_WIKI_PAGE, wikiPageRowMapper, wikiId);
		if(list.size() > 1) throw new DatastoreException("More than one Wiki page found with the id: " + wikiId);
		if(list.size() < 1) throw new NotFoundException("No wiki page found with id: " + wikiId);
		WikiPage page = list.get(0);
		WikiPageKey key = wikiPageDao.lookupWikiKey(page.getId());
		List<String> ids = wikiPageDao.getWikiFileHandleIds(key);
		page.setAttachmentFileHandleIds(ids);
		return page;
	}
	
	/**
	 * Returns the total number of wikis in the WikiPage table
	 * @return
	 */
	public Long getTotalCount() {
		return simpleJdbcTemplate.queryForLong(SELECT_COUNT_OF_WIKIS);
	}
	
	/**
	 * Stores the V2 WikiPage in the V2 database
	 * @param toMigrate
	 * @return
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public V2WikiPage migrateWiki(V2WikiPage toMigrate) {
		V2WikiPage result;
		try {
			Map<String, FileHandle> fileNameToFileHandleMap = buildFileNameMap(toMigrate);
			List<String> newFileHandlesToInsert = new ArrayList<String>();
			for(FileHandle handle: fileNameToFileHandleMap.values()){
				newFileHandlesToInsert.add(handle.getId());
			}
			WikiPageKey key = wikiPageDao.lookupWikiKey(toMigrate.getId());
			// Store in the V2 WikiPage DB
			result = v2WikiPageMigrationDao.create(toMigrate, fileNameToFileHandleMap, key.getOwnerObjectId(), key.getOwnerObjectType(), newFileHandlesToInsert);
		} catch(Exception e) {
			// To roll back all exceptions
			throw new RuntimeException(e);
		}
		return result;
	}
	
	/**
	 * Returns whether or not the parent wiki has migrated successfully to V2
	 * @param parentWikiId
	 * @return
	 */
	public boolean hasParentMigrated(String parentWikiId) {
		return v2WikiPageMigrationDao.doesParentExist(parentWikiId);
	}
	
	/**
	 * Build up the Map of FileHandle.fileNames to FileHandles  If there are duplicate names in the input list,
	 * then then the handle with the most recent creation date will be used.
	 * (From org.sagebionetworks.repo.manager.wiki.V2WikiManagerImpl)
	 * @param page
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	Map<String, FileHandle> buildFileNameMap(V2WikiPage page) throws DatastoreException, NotFoundException{
		Map<String, FileHandle> results = new HashMap<String, FileHandle>();
		// First lookup each FileHandle
		List<FileHandle> handles = new LinkedList<FileHandle>();
		if(page.getAttachmentFileHandleIds() != null){
			for(String id: page.getAttachmentFileHandleIds()){
				FileHandle handle = fileMetadataDao.get(id);
				handles.add(handle);
			}
		}
		// Now sort the list by createdOn
		Collections.sort(handles,  new Comparator<FileHandle>(){
			@Override
			public int compare(FileHandle one, FileHandle two) {
				return one.getCreatedOn().compareTo(two.getCreatedOn());
			}});
		// Now process the results
		for(FileHandle handle: handles){
			FileHandle old = results.put(handle.getFileName(), handle);
			if(old != null){
				// Log the duplicates
				log.info("Duplicate attachment file name found for Wiki. The older FileHandle will be replaced with the newer FileHandle.  Old FileHandle: "+old );
			}
		}
		return results;
	}
}
