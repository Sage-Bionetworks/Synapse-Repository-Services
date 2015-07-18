package org.sagebionetworks.object.snapshot.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.QueueCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class NodeObjectSnapshotWorkerIntegrationTest {

	private static final String QUEUE_NAME = "OBJECT";

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private QueueCleaner queueCleaner;

	private List<String> toDelete = new ArrayList<String>();
	private Long creatorUserGroupId;
	private Long altUserGroupId;
	private String type;

	@Before
	public void before() {
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		altUserGroupId = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();

		assertNotNull(nodeDao);
		assertNotNull(objectRecordDAO);
		assertNotNull(queueCleaner);
		toDelete = new ArrayList<String>();

		type = Node.class.getSimpleName().toLowerCase();
		queueCleaner.purgeQueue(StackConfiguration.singleton().getAsyncQueueName(QUEUE_NAME));
	}
	
	@After
	public void after() {
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
			}
		}
	}

	@Test
	public void test() throws Exception {
		Set<String> keys = ObjectSnapshotWorkerIntegrationTestUtils.listAllKeys(objectRecordDAO, type);

		Node toCreate = createNew("node name", creatorUserGroupId, altUserGroupId);
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// This node should exist
		assertTrue(nodeDao.doesNodeExist(KeyFactory.stringToKey(id)));

		// fetch it
		Node node = nodeDao.getNode(id);
		ObjectRecord expectedRecord = new ObjectRecord();
		expectedRecord.setJsonClassName(node.getClass().getSimpleName().toLowerCase());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(node));

		assertTrue(ObjectSnapshotWorkerIntegrationTestUtils.waitForObjects(keys, Arrays.asList(expectedRecord), objectRecordDAO, type));
	}

	private static Node createNew(String name, Long creatorUserGroupId, Long modifierGroupId){
		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(modifierGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project);
		return node;
	}

}
