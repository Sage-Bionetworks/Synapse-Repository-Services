package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.TableFileAssociationDao;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableFileAssociationDaoImplTest {

	@Autowired
	TableFileAssociationDao tableFileAssociationDao;
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	TableRowTruthDAO tableRowTruthDAO;
	
	Long adminUserId;
	String adminUserIdString;
	
	List<String> fileIds;
	
	String tableOneId;
	String tableTwoId;
	
	@Before
	public void before(){		
		// get user IDs
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		
		fileIds = new LinkedList<String>();
		
		for(int i=0; i<5; i++){
			S3FileHandle fh = new S3FileHandle();
			fh.setCreatedBy(adminUserIdString);
			fh.setCreatedOn(new Date());
			fh.setBucketName("bucket");
			fh.setKey("mainFileKey");
			fh.setEtag("etag");
			fh.setFileName("foo.bar");
			fh = fileHandleDao.createFile(fh, false);
			fileIds.add(fh.getId());
		}
		// Create a table
		tableRowTruthDAO.truncateAllRowData();
		tableOneId = "123";
		tableRowTruthDAO.reserveIdsInRange(tableOneId, 5);
		tableTwoId = "456";
		tableRowTruthDAO.reserveIdsInRange(tableTwoId, 5);
	}
	
	@After
	public void after(){
		if(fileIds != null){
			for(String id: fileIds){
				try {
					fileHandleDao.delete(id);
				} catch (Exception e) {}
			}
		}
		tableRowTruthDAO.truncateAllRowData();
	}
	
	@Test
	public void testRoundTrip(){
		Set<String> filesInTableOne = Sets.newHashSet(fileIds.get(0), fileIds.get(3), fileIds.get(4));
		Set<String> filesInTableTwo = Sets.newHashSet(fileIds.get(1), fileIds.get(2), fileIds.get(0));
		// bind to table one.
		tableFileAssociationDao.bindFileHandleIdsToTable(tableOneId, filesInTableOne);
		tableFileAssociationDao.bindFileHandleIdsToTable(tableTwoId, filesInTableTwo);
		// asking for all file ids should only return the associated.
		Set<String> results = tableFileAssociationDao.getFileHandleIdsAssociatedWithTable(fileIds, tableOneId);
		assertEquals(filesInTableOne, results);
		// same for results 2.
		results = tableFileAssociationDao.getFileHandleIdsAssociatedWithTable(fileIds, tableTwoId);
		assertEquals(filesInTableTwo, results);
	}
	
	/**
	 * Must be able to insert the same file multiple times.
	 */
	@Test
	public void testIdempotent(){
		Set<String> filesInTableOne = Sets.newHashSet(fileIds.get(0), fileIds.get(3), fileIds.get(4));
		Set<String> filesInTableTwo = Sets.newHashSet(fileIds.get(1), fileIds.get(2), fileIds.get(0));
		// bind to table one.
		tableFileAssociationDao.bindFileHandleIdsToTable(tableOneId, filesInTableOne);
		tableFileAssociationDao.bindFileHandleIdsToTable(tableOneId, filesInTableTwo);
		tableFileAssociationDao.bindFileHandleIdsToTable(tableOneId, filesInTableTwo);
		// asking for all file ids should only return the associated.
		Set<String> results = tableFileAssociationDao.getFileHandleIdsAssociatedWithTable(fileIds, tableOneId);
		Set<String> expected = Sets.union(filesInTableOne, filesInTableTwo);
		assertEquals(expected, results);
	}

}
