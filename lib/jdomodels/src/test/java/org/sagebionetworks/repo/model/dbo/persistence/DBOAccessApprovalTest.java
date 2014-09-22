package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PostMessageContentAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessApprovalTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String TEST_USER_NAME = "test-user@test.com";
	
	private Node node = null;
	private UserGroup individualGroup = null;
	private DBOAccessRequirement ar = null;
	private DBOAccessApproval accessApproval = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());

		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDao.createNew(node) );
		};
		deleteAccessApproval();
		deleteAccessRequirement();
		DBOAccessRequirement accessRequirement = DBOAccessRequirementTest.newAccessRequirement(individualGroup, node, "foo".getBytes(), idGenerator.generateNewId());
		ar = dboBasicDao.createNew(accessRequirement);
	}
	
	private void deleteAccessRequirement() throws DatastoreException {
		if(dboBasicDao != null && ar!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", ar.getId());
			dboBasicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, params);
			ar=null;
		}		
	}
		
	private void deleteAccessApproval() throws DatastoreException {
		if(dboBasicDao != null && accessApproval!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", accessApproval.getId());
			dboBasicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class, params);
			accessApproval=null;
		}		
	}
		
	
	@After
	public void tearDown() throws Exception {
		deleteAccessApproval();
		deleteAccessRequirement();
		if (node!=null && nodeDao!=null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (individualGroup != null) {
			// this will delete the user profile too
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	public static DBOAccessApproval newAccessApproval(UserGroup principal, DBOAccessRequirement ar, Long id) throws DatastoreException {
		DBOAccessApproval accessApproval = new DBOAccessApproval();
		accessApproval.setCreatedBy(Long.parseLong(principal.getId()));
		accessApproval.setCreatedOn(System.currentTimeMillis());
		accessApproval.setModifiedBy(Long.parseLong(principal.getId()));
		accessApproval.setModifiedOn(System.currentTimeMillis());
		accessApproval.seteTag("10");
		accessApproval.setAccessorId(Long.parseLong(principal.getId()));
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setSerializedEntity("my dog has fleas".getBytes());
		accessApproval.setId(id);
		return accessApproval;
	}
	
	@Test
	public void testCRUD() throws Exception{
		accessApproval = dboBasicDao.createNew(newAccessApproval(individualGroup, ar, idGenerator.generateNewId()));
		// Create a new object		
		
		// Create it
		assertNotNull(accessApproval);
		assertNotNull(accessApproval.getId());
		
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", accessApproval.getId());
		DBOAccessApproval clone = dboBasicDao.getObjectByPrimaryKey(DBOAccessApproval.class, params);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
		// Update it
		dboBasicDao.update(clone);
		clone = dboBasicDao.getObjectByPrimaryKey(DBOAccessApproval.class, params);
		assertNotNull(clone);
		
		// Delete it
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOAccessApproval.class,  params);
		assertTrue("Failed to delete the type created", result);
	}
	
	@Test
	public void testMigratableTableTranslationToU() throws Exception {
		DBOAccessApproval ar = new DBOAccessApproval();
		MigratableTableTranslation<DBOAccessApproval, DBOAccessApprovalBackup> translator = ar.getTranslator();
		DBOAccessApprovalBackup dbobackup = new DBOAccessApprovalBackup();
		dbobackup.setAccessorId(11111L);
		dbobackup.setRequirementId(22222L);
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("aa");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		TermsOfUseAccessApprovalLegacy touLegacy = new TermsOfUseAccessApprovalLegacy();
		touLegacy.setConcreteType(TermsOfUseAccessApproval.class.getName());
		touLegacy.setCreatedBy("101");
		touLegacy.setCreatedOn(new Date(now));
		touLegacy.setEntityType(TermsOfUseAccessApproval.class.getName());
		touLegacy.setEtag("abcd");
		touLegacy.setId(987L);
		touLegacy.setModifiedBy("202");
		touLegacy.setModifiedOn(new Date(now+10L));
		touLegacy.setUri("/my/uri");
		touLegacy.setAccessorId("11111");
		touLegacy.setRequirementId(22222L);
		
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(touLegacy, TermsOfUseAccessApproval.class.getName());
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessApproval dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessApprovalBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals(new Long(11111L), backup2.getAccessorId());
		assertEquals(new Long(22222L), backup2.getRequirementId());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		TermsOfUseAccessApproval touAA = (TermsOfUseAccessApproval)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(TermsOfUseAccessApproval.class.getName(), touAA.getConcreteType());
		assertEquals("101", touAA.getCreatedBy());
		assertEquals(new Date(now), touAA.getCreatedOn());
		assertEquals("abcd", touAA.getEtag());
		assertEquals(new Long(987L), touAA.getId());
		assertEquals("202", touAA.getModifiedBy());
		assertEquals(new Date(now+10), touAA.getModifiedOn());
		assertEquals("11111", touAA.getAccessorId());
		assertEquals(new Long(22222L), touAA.getRequirementId());
	}

	
	@Test
	public void testMigratableTableTranslationACT() throws Exception {
		DBOAccessApproval ar = new DBOAccessApproval();
		MigratableTableTranslation<DBOAccessApproval, DBOAccessApprovalBackup> translator = ar.getTranslator();
		DBOAccessApprovalBackup dbobackup = new DBOAccessApprovalBackup();
		dbobackup.setAccessorId(11111L);
		dbobackup.setRequirementId(22222L);
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("aa");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		ACTAccessApprovalLegacy touLegacy = new ACTAccessApprovalLegacy();
		touLegacy.setConcreteType(ACTAccessApproval.class.getName());
		touLegacy.setCreatedBy("101");
		touLegacy.setCreatedOn(new Date(now));
		touLegacy.setEntityType(ACTAccessApproval.class.getName());
		touLegacy.setEtag("abcd");
		touLegacy.setId(987L);
		touLegacy.setModifiedBy("202");
		touLegacy.setModifiedOn(new Date(now+10L));
		touLegacy.setUri("/my/uri");
		touLegacy.setAccessorId("11111");
		touLegacy.setRequirementId(22222L);
		
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(touLegacy, ACTAccessApproval.class.getName());
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessApproval dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessApprovalBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals(new Long(11111L), backup2.getAccessorId());
		assertEquals(new Long(22222L), backup2.getRequirementId());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		ACTAccessApproval touAA = (ACTAccessApproval)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(ACTAccessApproval.class.getName(), touAA.getConcreteType());
		assertEquals("101", touAA.getCreatedBy());
		assertEquals(new Date(now), touAA.getCreatedOn());
		assertEquals("abcd", touAA.getEtag());
		assertEquals(new Long(987L), touAA.getId());
		assertEquals("202", touAA.getModifiedBy());
		assertEquals(new Date(now+10), touAA.getModifiedOn());
		assertEquals("11111", touAA.getAccessorId());
		assertEquals(new Long(22222L), touAA.getRequirementId());
	}
	
	
	@Test
	public void testMigratableTableTranslationPMC() throws Exception {
		DBOAccessApproval ar = new DBOAccessApproval();
		MigratableTableTranslation<DBOAccessApproval, DBOAccessApprovalBackup> translator = ar.getTranslator();
		DBOAccessApprovalBackup dbobackup = new DBOAccessApprovalBackup();
		dbobackup.setAccessorId(11111L);
		dbobackup.setRequirementId(22222L);
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("aa");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		PostMessageContentAccessApproval pmcAA = new PostMessageContentAccessApproval();
		pmcAA.setConcreteType(PostMessageContentAccessApproval.class.getName());
		pmcAA.setCreatedBy("101");
		pmcAA.setCreatedOn(new Date(now));
		pmcAA.setEtag("abcd");
		pmcAA.setId(987L);
		pmcAA.setModifiedBy("202");
		pmcAA.setModifiedOn(new Date(now+10L));
		pmcAA.setAccessorId("11111");
		pmcAA.setRequirementId(22222L);
		
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(pmcAA, PostMessageContentAccessApproval.class.getName());
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessApproval dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessApprovalBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals(new Long(11111L), backup2.getAccessorId());
		assertEquals(new Long(22222L), backup2.getRequirementId());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		PostMessageContentAccessApproval pmcAA2 = (PostMessageContentAccessApproval)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(PostMessageContentAccessApproval.class.getName(), pmcAA2.getConcreteType());
		assertEquals("101", pmcAA2.getCreatedBy());
		assertEquals(new Date(now), pmcAA2.getCreatedOn());
		assertEquals("abcd", pmcAA2.getEtag());
		assertEquals(new Long(987L), pmcAA2.getId());
		assertEquals("202", pmcAA2.getModifiedBy());
		assertEquals(new Date(now+10), pmcAA2.getModifiedOn());
		assertEquals("11111", pmcAA2.getAccessorId());
		assertEquals(new Long(22222L), pmcAA2.getRequirementId());
	}
}
