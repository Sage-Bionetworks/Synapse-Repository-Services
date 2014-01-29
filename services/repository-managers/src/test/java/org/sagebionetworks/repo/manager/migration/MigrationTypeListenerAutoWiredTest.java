package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList.OWNER_ID_FIELD_NAME;
import static org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList.OWNER_TYPE_FIELD_NAME;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationTypeListenerAutoWiredTest {
	
	@Autowired
	private UserManager userManager;	
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired 
	MigrationTypeListener aclMigrationTypeListener;
	
	@Autowired
	AccessControlListDAO aclDAO;
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;	


	
	private List<String> entityIdsToDelete;
	private UserInfo adminUser;
	private String creatorUserGroupId;
	
	@Before
	public void before() throws Exception {
		entityIdsToDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		creatorUserGroupId = adminUser.getId().toString();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after() throws Exception{
		// If we have deleted all data make sure the bootstrap process puts it back
		for (String entityId : entityIdsToDelete) {
			entityManager.deleteEntity(adminUser, entityId);
		}
	}

	private static final String OWNER_TYPE_UPDATE_SQL = "update ACL set OWNER_TYPE=:"+OWNER_TYPE_FIELD_NAME+" where OWNER_ID=:"+OWNER_ID_FIELD_NAME;
	
	@Test
	public void testACLMigrationListener() throws Exception {
		// create a node
		Project project = new Project();
		project.setName("MigrationTypeListenerAutoWiredTest");
		String id = entityManager.createEntity(adminUser, project, null);
		entityIdsToDelete.add(id);
		AccessControlList acl = aclDAO.get(id, ObjectType.ENTITY); 
		assertNotNull(acl);
		
		long lid = KeyFactory.stringToKey(id);
		DBOAccessControlList dbo = null;
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OWNER_ID_FIELD_NAME, lid);
			param.addValue(OWNER_TYPE_FIELD_NAME, ObjectType.ENTITY.name());
		
			dbo = dboBasicDao.getObjectByPrimaryKey(DBOAccessControlList.class, param);
		}
		assertEquals(ObjectType.ENTITY.name(), dbo.getOwnerType());
		
		// now set to 'unknown':
		dbo.setOwnerType(DBOAccessControlList.UNKNOWN_OWNER_TYPE);
		{
			MapSqlParameterSource updateParam = new MapSqlParameterSource();
			updateParam.addValue(OWNER_ID_FIELD_NAME, lid);
			updateParam.addValue(OWNER_TYPE_FIELD_NAME, DBOAccessControlList.UNKNOWN_OWNER_TYPE);
			simpleJdbcTemplate.update(OWNER_TYPE_UPDATE_SQL, updateParam);
		}
		{
			// the owner-type has been changed
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OWNER_ID_FIELD_NAME, lid);
			param.addValue(OWNER_TYPE_FIELD_NAME, DBOAccessControlList.UNKNOWN_OWNER_TYPE);
			dbo = dboBasicDao.getObjectByPrimaryKey(DBOAccessControlList.class, param);
			assertEquals(DBOAccessControlList.UNKNOWN_OWNER_TYPE, dbo.getOwnerType());
		}

		// create an ACL pointing to the node, but with with owner_type=UNKNOWN
		// now run the listener
		List<DBOAccessControlList> delta = new ArrayList<DBOAccessControlList>();
		delta.add(dbo);
		aclMigrationTypeListener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		// now show that the ownerType is changed to ENTITY
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OWNER_ID_FIELD_NAME, lid);
			param.addValue(OWNER_TYPE_FIELD_NAME, ObjectType.ENTITY.name());
		
			dbo = dboBasicDao.getObjectByPrimaryKey(DBOAccessControlList.class, param);
		}
		assertEquals(ObjectType.ENTITY.name(), dbo.getOwnerType());
		
	}
}
