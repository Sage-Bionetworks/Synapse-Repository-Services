package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.dao.OldACTAccessRequirement;
import org.sagebionetworks.repo.model.dbo.dao.SchemaSerializationUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

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
	NodeDAO nodeDAO;
	
	private static final String TEST_USER_NAME = "test-user";
	
	private Node node = null;
	private UserGroup individualGroup = null;
	private DBOAccessRequirement ar = null;
	
	@Before
	public void setUp() throws Exception {
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup == null) {
			individualGroup = new UserGroup();
			individualGroup.setName(TEST_USER_NAME);
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup));
		}
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDAO.createNew(node) );
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
		if (node!=null && nodeDAO!=null) {
			nodeDAO.delete(node.getId());
			node = null;
		}
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
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
		accessRequirement.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
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
	public void testTranslator() throws Exception {
		DBOAccessRequirement backup = new DBOAccessRequirement();
		backup.setAccessType("DOWNLOAD");
		backup.setCreatedBy(1234L);
		long createdOn = System.currentTimeMillis() - 10000L;
		long modifiedOn = System.currentTimeMillis();
		backup.setCreatedOn(createdOn);
		backup.setEntityType("ACTAccessRequirement");
		backup.seteTag("abc");
		backup.setId(101L);
		backup.setModifiedBy(9876L);
		backup.setModifiedOn(modifiedOn);
		OldACTAccessRequirement old = new OldACTAccessRequirement();
		old.setId(backup.getId());
		List<String> entityIds = Arrays.asList(new String[]{"1", "2", "3", "4"});
		old.setEntityIds(entityIds);
		old.setAccessType(ACCESS_TYPE.valueOf(backup.getAccessType()));
		old.setActContactInfo("act@sagebase.org");
		old.setCreatedBy(backup.getCreatedBy().toString());
		old.setCreatedOn(new Date(backup.getCreatedOn()));
		old.setEntityType(backup.getEntityType());
		old.setEtag(backup.geteTag());
		old.setModifiedBy(backup.getModifiedBy().toString());
		old.setModifiedOn(new Date(backup.getModifiedOn()));
		old.setUri("/foo");
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream buff = new BufferedOutputStream(out);
		GZIPOutputStream zipper = new GZIPOutputStream(buff);
		try{
			XStream xstream = new XStream();
			xstream.alias(ACTAccessRequirement.class.getName(), OldACTAccessRequirement.class);
			xstream.toXML(old, zipper);
			zipper.flush();
			zipper.close();
		}finally{
			zipper.flush();
			zipper.close();
		}
		backup.setSerializedEntity(out.toByteArray());
		MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirement> translator = 
			backup.getTranslator();
		DBOAccessRequirement dbo = translator.createDatabaseObjectFromBackup(backup);
		
		// now check that it's right
		assertEquals(backup.getAccessType(), dbo.getAccessType());
		assertEquals(backup.getCreatedBy(), dbo.getCreatedBy());
		assertEquals(backup.getCreatedOn(), dbo.getCreatedOn());
		assertEquals(backup.getEntityType(), dbo.getEntityType());
		assertEquals(backup.geteTag(), dbo.geteTag());
		assertEquals(backup.getId(), dbo.getId());
		assertEquals(backup.getModifiedBy(), dbo.getModifiedBy());
		assertEquals(backup.getModifiedOn(), dbo.getModifiedOn());
		AccessRequirement deser = (AccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		assertTrue(deser instanceof ACTAccessRequirement);
		ACTAccessRequirement act = (ACTAccessRequirement)deser;
		assertEquals(backup.getId(), act.getId());
		Set<String> actIds = new HashSet<String>();
		for (RestrictableObjectDescriptor subjectId : act.getSubjectIds()) {
			assertEquals(RestrictableObjectType.ENTITY, subjectId.getType());
			actIds.add(subjectId.getId());
		}
		assertEquals(new HashSet<String>(entityIds), actIds);
		assertEquals(old.getAccessType(), act.getAccessType());
		assertEquals(old.getActContactInfo(), act.getActContactInfo());
		assertEquals(old.getCreatedBy(), act.getCreatedBy());
		assertEquals(old.getCreatedOn(), act.getCreatedOn());
		assertEquals(old.getEntityType(), act.getEntityType());
		assertEquals(old.getEtag(), act.getEtag());
		assertEquals(old.getModifiedBy(), act.getModifiedBy());
		assertEquals(old.getModifiedOn(), act.getModifiedOn());
		assertEquals(old.getUri(), act.getUri());
	}

}
