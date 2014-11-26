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
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
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
public class DBOAccessRequirementTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired 
	UserGroupDAO userGroupDAO;
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	NodeDAO nodeDao;
	
	private static final String TEST_USER_NAME = "test-user@test.com";
	
	private Node node = null;
	private UserGroup individualGroup = null;
	private DBOAccessRequirement ar = null;
	
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
		deleteAccessRequirement();
	}
	
	private void deleteAccessRequirement() throws DatastoreException {
		if(dboBasicDao != null && ar!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", ar.getId());
			dboBasicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class, params);
			ar=null;
		}		
	}
		
	
	@After
	public void tearDown() throws Exception {
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
	
	public static DBOAccessRequirement newAccessRequirement(UserGroup principal, Node node, byte[] serializedEntity, Long id) throws DatastoreException {
		DBOAccessRequirement accessRequirement = new DBOAccessRequirement();
		accessRequirement.setCreatedBy(Long.parseLong(principal.getId()));
		accessRequirement.setCreatedOn(System.currentTimeMillis());
		accessRequirement.setModifiedBy(Long.parseLong(principal.getId()));
		accessRequirement.setModifiedOn(System.currentTimeMillis());
		accessRequirement.seteTag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD.toString());
		accessRequirement.setSerializedEntity(serializedEntity);
		accessRequirement.setId(id);
		return accessRequirement;
	}
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new object
		DBOAccessRequirement accessRequirement = newAccessRequirement(individualGroup, node, "foo".getBytes(), idGenerator.generateNewId());
		
		// Create it
		DBOAccessRequirement clone = dboBasicDao.createNew(accessRequirement);
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", accessRequirement.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOAccessRequirement.class, params);
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// Update it
		dboBasicDao.update(clone);
		clone = dboBasicDao.getObjectByPrimaryKey(DBOAccessRequirement.class, params);
		assertNotNull(clone);
		
		// Delete it
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOAccessRequirement.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}
	
	@Test
	public void testMigratableTableTranslationToU() throws Exception {
		DBOAccessRequirement ar = new DBOAccessRequirement();
		MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirementBackup> translator = ar.getTranslator();
		DBOAccessRequirementBackup dbobackup = new DBOAccessRequirementBackup();
		dbobackup.setAccessType("DOWNLOAD");
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("ar");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		TermsOfUseAccessRequirementLegacy touLegacy = new TermsOfUseAccessRequirementLegacy();
		touLegacy.setAccessType(ACCESS_TYPE.DOWNLOAD);
		touLegacy.setConcreteType(TermsOfUseAccessRequirement.class.getName());
		touLegacy.setCreatedBy("101");
		touLegacy.setCreatedOn(new Date(now));
		touLegacy.setEntityType(TermsOfUseAccessRequirement.class.getName());
		touLegacy.setEtag("abcd");
		touLegacy.setId(987L);
		touLegacy.setLocationData(new LocationData());
		touLegacy.setModifiedBy("202");
		touLegacy.setModifiedOn(new Date(now+10L));
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("111");
		rod.setType(RestrictableObjectType.ENTITY);
		List<RestrictableObjectDescriptor> rods = Collections.singletonList(rod);
		touLegacy.setSubjectIds(rods);
		touLegacy.setTermsOfUse("foo");
		touLegacy.setUri("/my/uri");
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(touLegacy, TermsOfUseAccessRequirement.class.getName());
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessRequirement dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessRequirementBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals("DOWNLOAD", backup2.getAccessType());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		TermsOfUseAccessRequirement touAr = (TermsOfUseAccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(ACCESS_TYPE.DOWNLOAD, touAr.getAccessType());
		assertEquals(TermsOfUseAccessRequirement.class.getName(), touAr.getConcreteType());
		assertEquals("101", touAr.getCreatedBy());
		assertEquals(new Date(now), touAr.getCreatedOn());
		assertEquals("abcd", touAr.getEtag());
		assertEquals(new Long(987L), touAr.getId());
		assertEquals("202", touAr.getModifiedBy());
		assertEquals(new Date(now+10), touAr.getModifiedOn());
		assertEquals(rods, touAr.getSubjectIds());
		assertEquals("foo", touAr.getTermsOfUse());
	}
	
	@Test
	public void testMigratableTableTranslationACT() throws Exception {
		DBOAccessRequirement ar = new DBOAccessRequirement();
		MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirementBackup> translator = ar.getTranslator();
		DBOAccessRequirementBackup dbobackup = new DBOAccessRequirementBackup();
		dbobackup.setAccessType("DOWNLOAD");
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("ar");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		ACTAccessRequirement act = new ACTAccessRequirement();
		act.setAccessType(ACCESS_TYPE.DOWNLOAD);
		act.setConcreteType(ACTAccessRequirement.class.getName());
		act.setCreatedBy("101");
		act.setCreatedOn(new Date(now));
		act.setEtag("abcd");
		act.setId(987L);
		act.setActContactInfo("foo");
		act.setModifiedBy("202");
		act.setModifiedOn(new Date(now+10L));
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("111");
		rod.setType(RestrictableObjectType.ENTITY);
		List<RestrictableObjectDescriptor> rods = Collections.singletonList(rod);
		act.setSubjectIds(rods);
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(act, ACTAccessRequirement.class.getName());
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessRequirement dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessRequirementBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals("DOWNLOAD", backup2.getAccessType());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		ACTAccessRequirement actAr = (ACTAccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(ACCESS_TYPE.DOWNLOAD, actAr.getAccessType());
		assertEquals(ACTAccessRequirement.class.getName(), actAr.getConcreteType());
		assertEquals("101", actAr.getCreatedBy());
		assertEquals(new Date(now), actAr.getCreatedOn());
		assertEquals("abcd", actAr.getEtag());
		assertEquals(new Long(987L), actAr.getId());
		assertEquals("202", actAr.getModifiedBy());
		assertEquals(new Date(now+10), actAr.getModifiedOn());
		assertEquals(rods, actAr.getSubjectIds());
		assertEquals("foo", actAr.getActContactInfo());
		assertTrue(actAr.getOpenJiraIssue());
	}
	
	
	@Test
	public void testMigratableTableTranslationACTLegacy() throws Exception {
		DBOAccessRequirement ar = new DBOAccessRequirement();
		MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirementBackup> translator = ar.getTranslator();
		DBOAccessRequirementBackup dbobackup = new DBOAccessRequirementBackup();
		dbobackup.setAccessType("DOWNLOAD");
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("ar");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		ACTAccessRequirementLegacy actLegacy = new ACTAccessRequirementLegacy();
		actLegacy.setAccessType(ACCESS_TYPE.DOWNLOAD);
		actLegacy.setConcreteType(ACTAccessRequirement.class.getName());
		actLegacy.setCreatedBy("101");
		actLegacy.setCreatedOn(new Date(now));
		actLegacy.setEntityType(ACTAccessRequirement.class.getName());
		actLegacy.setEtag("abcd");
		actLegacy.setId(987L);
		actLegacy.setActContactInfo("foo");
		actLegacy.setModifiedBy("202");
		actLegacy.setModifiedOn(new Date(now+10L));
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("111");
		rod.setType(RestrictableObjectType.ENTITY);
		List<RestrictableObjectDescriptor> rods = Collections.singletonList(rod);
		actLegacy.setSubjectIds(rods);
		actLegacy.setUri("/my/uri");
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(actLegacy, ACTAccessRequirement.class.getName());
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessRequirement dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessRequirementBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals("DOWNLOAD", backup2.getAccessType());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		ACTAccessRequirement actAr = (ACTAccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(ACCESS_TYPE.DOWNLOAD, actAr.getAccessType());
		assertEquals(ACTAccessRequirement.class.getName(), actAr.getConcreteType());
		assertEquals("101", actAr.getCreatedBy());
		assertEquals(new Date(now), actAr.getCreatedOn());
		assertEquals("abcd", actAr.getEtag());
		assertEquals(new Long(987L), actAr.getId());
		assertEquals("202", actAr.getModifiedBy());
		assertEquals(new Date(now+10), actAr.getModifiedOn());
		assertEquals(rods, actAr.getSubjectIds());
		assertEquals("foo", actAr.getActContactInfo());
		assertTrue(actAr.getOpenJiraIssue());
	}
	
	
	@Test
	public void testMigratableTableTranslationPostMessageContent() throws Exception {
		DBOAccessRequirement ar = new DBOAccessRequirement();
		MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirementBackup> translator = ar.getTranslator();
		DBOAccessRequirementBackup dbobackup = new DBOAccessRequirementBackup();
		dbobackup.setAccessType("DOWNLOAD");
		dbobackup.setCreatedBy(101L);
		long now = (new Date()).getTime();
		dbobackup.setCreatedOn(now);
		dbobackup.setEntityType("ar");
		dbobackup.seteTag("abcd");
		dbobackup.setId(987L);
		dbobackup.setModifiedBy(202L);
		dbobackup.setModifiedOn(now+10);
		PostMessageContentAccessRequirement pmcar = new PostMessageContentAccessRequirement();
		pmcar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		pmcar.setConcreteType(PostMessageContentAccessRequirement.class.getName());
		pmcar.setCreatedBy("101");
		pmcar.setCreatedOn(new Date(now));
		pmcar.setEtag("abcd");
		pmcar.setId(987L);
		pmcar.setModifiedBy("202");
		pmcar.setModifiedOn(new Date(now+10L));
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("111");
		rod.setType(RestrictableObjectType.ENTITY);
		List<RestrictableObjectDescriptor> rods = Collections.singletonList(rod);
		pmcar.setSubjectIds(rods);
		pmcar.setUrl("http://foo.bar.com");
		byte[] ser = JDOSecondaryPropertyUtils.compressObject(pmcar);
		dbobackup.setSerializedEntity(ser);
		
		DBOAccessRequirement dbo = translator.createDatabaseObjectFromBackup(dbobackup);
		DBOAccessRequirementBackup backup2 = translator.createBackupFromDatabaseObject(dbo);
		
		assertEquals("DOWNLOAD", backup2.getAccessType());
		assertEquals(new Long(101L), backup2.getCreatedBy());
		assertEquals(now, backup2.getCreatedOn());
		assertEquals(null, backup2.getEntityType());
		assertEquals("abcd", backup2.geteTag());
		assertEquals(new Long(987L), backup2.getId());
		assertEquals(new Long(202L), backup2.getModifiedBy());
		assertEquals(now+10, backup2.getModifiedOn());
		byte[] ser2 = backup2.getSerializedEntity();
		PostMessageContentAccessRequirement pmcAr = (PostMessageContentAccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(ser2);
		assertEquals(ACCESS_TYPE.DOWNLOAD, pmcAr.getAccessType());
		assertEquals(PostMessageContentAccessRequirement.class.getName(), pmcAr.getConcreteType());
		assertEquals("101", pmcAr.getCreatedBy());
		assertEquals(new Date(now), pmcAr.getCreatedOn());
		assertEquals("abcd", pmcAr.getEtag());
		assertEquals(new Long(987L), pmcAr.getId());
		assertEquals("202", pmcAr.getModifiedBy());
		assertEquals(new Date(now+10), pmcAr.getModifiedOn());
		assertEquals(rods, pmcAr.getSubjectIds());
		assertEquals("http://foo.bar.com", pmcAr.getUrl());
	}

}
