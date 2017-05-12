package org.sagebionetworks.repo.model.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EntityBootstrapperAutowireTest {
	
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	EntityBootstrapper entityBootstrapper;
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	
	@Test
	public void testBootstrap() throws DatastoreException, NotFoundException{
		assertNotNull(nodeDao);
		assertNotNull(entityBootstrapper);
		assertNotNull(entityBootstrapper.getBootstrapEntities());
		// Make sure we can find each entity
		List<EntityBootstrapData> list = entityBootstrapper.getBootstrapEntities();
		for(EntityBootstrapData entityBoot: list){
			// Look up the entity
			String id = nodeDao.getNodeIdForPath(entityBoot.getEntityPath());
			assertNotNull(id);
			Node node = nodeDao.getNode(id);
			assertNotNull(node);
			String benenefactorId = nodeDao.getBenefactor(id);
			// This node should inherit from itself
			assertEquals("A bootstrapped node should be its own benefactor",id, benenefactorId);
			
			// root nodes don't have ACLs
			try {
				AccessControlList acl = accessControlListDAO.get(id, ObjectType.ENTITY);
				assertNotNull(acl);
			} catch (NotFoundException nfe) {
				throw new NotFoundException("id="+id);
			}
		}
	}
	
	@Test
	public void testRerunBootstrap() throws Exception{
		assertNotNull(nodeDao);
		assertNotNull(entityBootstrapper);
		assertNotNull(entityBootstrapper.getBootstrapEntities());
		// Make sure that we can rerun the bootstrapper
		entityBootstrapper.bootstrapAll();
		// Make sure we can find each entity
		List<EntityBootstrapData> list = entityBootstrapper.getBootstrapEntities();
		for(EntityBootstrapData entityBoot: list){
			// Look up the entity
			String id = nodeDao.getNodeIdForPath(entityBoot.getEntityPath());
			assertNotNull(id);
		}
	}

}
