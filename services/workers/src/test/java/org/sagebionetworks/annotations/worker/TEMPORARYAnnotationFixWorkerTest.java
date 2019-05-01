package org.sagebionetworks.annotations.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
class TEMPORARYAnnotationF43ixWorkerIntegrationTest {

	@Autowired
	NodeManager nodeManager;

	@Autowired
	UserManager userManager;

	@Autowired
	EntityManager entityManager;

	Node node1;
	Node node2;

	NamedAnnotations annotations1;
	NamedAnnotations annotations2;

	List<String> nodeIdToDelete;

	UserInfo adminUserInfo;

	@BeforeEach
	void setUp() {
		adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		node1 = new Node();
		node1.setNodeType(EntityType.file);
		node2 = new Node();
		node2.setNodeType(EntityType.file);


		annotations1 = new NamedAnnotations();
		annotations2 = new NamedAnnotations();
		addAnnotations(annotations1);
		addAnnotations(annotations2);

		node1 = nodeManager.createNewNode(node1, annotations1, adminUserInfo);
		node2 = nodeManager.createNewNode(node2, annotations2, adminUserInfo);


		nodeIdToDelete = Arrays.asList(node1.getId(),node2.getId());
	}

	@AfterEach
	void tearDown(){
		for(String nodeId : nodeIdToDelete){
			nodeManager.delete(adminUserInfo, nodeId);
		}

	}

	@Test
	public void testWorker() throws InterruptedException {
		//check annotations where written for the nodes
		assertFalse(nodeManager.getAnnotations(adminUserInfo, node1.getId()).isEmpty());
		assertFalse(nodeManager.getAnnotations(adminUserInfo, node2.getId()).isEmpty());

		//make the listener write changes to sqs queue
		DBORevision dborevisoin1 = new DBORevision();
		dborevisoin1.setOwner(KeyFactory.stringToKey(node1.getId()));
		dborevisoin1.setRevisionNumber(node1.getVersionNumber());
		DBORevision dborevisoin2 = new DBORevision();
		dborevisoin2.setOwner(KeyFactory.stringToKey(node2.getId()));
		dborevisoin2.setRevisionNumber(node2.getVersionNumber());

		entityManager.TEMPORARYcleanupAnnotations(adminUserInfo, KeyFactory.stringToKey(node1.getId()), 1000);
		System.out.println(node1.getId() + "," + node2.getId());

		long maxWaitMillis = 5 * 1000;
		long startTime = System.currentTimeMillis();

		//keep checking until the annotations no longer contain "concreteType"
		do{
			annotations1 = nodeManager.getAnnotations(adminUserInfo, node1.getId());
			annotations2 = nodeManager.getAnnotations(adminUserInfo, node2.getId());
			if(System.currentTimeMillis() - startTime >= maxWaitMillis){
				fail("worker did not remove \"concreteType\" anotations");
			}
			Thread.sleep(1000);
		}
		while (
				annotations1.getAdditionalAnnotations().getStringAnnotations().containsKey("concreteType")
						&& annotations1.getPrimaryAnnotations().getStringAnnotations().containsKey("concreteType")

						&& annotations2.getAdditionalAnnotations().getStringAnnotations().containsKey("concreteType")
						&& annotations2.getPrimaryAnnotations().getStringAnnotations().containsKey("concreteType")
		);

		//check that other annotations were not deleted
		assertTrue(annotations1.getAdditionalAnnotations().getStringAnnotations().containsKey("otherAdditionalAnnotation"));
		assertTrue(annotations1.getPrimaryAnnotations().getStringAnnotations().containsKey("otherPrimaryAnnotation"));
		assertTrue(annotations2.getAdditionalAnnotations().getStringAnnotations().containsKey("otherAdditionalAnnotation"));
		assertTrue(annotations2.getPrimaryAnnotations().getStringAnnotations().containsKey("otherPrimaryAnnotation"));

		//check that etags did not change
		Node currNode1 = nodeManager.getNodeForVersionNumber(adminUserInfo, node1.getId(), node1.getVersionNumber());
		Node currNode2 = nodeManager.getNodeForVersionNumber(adminUserInfo, node2.getId(), node2.getVersionNumber());

		assertEquals(node1.getETag(), currNode1.getETag());
		assertEquals(node2.getETag(), currNode2.getETag());
	}


	void addAnnotations(NamedAnnotations namedAnnotations){
		namedAnnotations.getPrimaryAnnotations().addAnnotation("concreteType", "org.sagebionetworks.repo.model.FileEntity");
		namedAnnotations.getPrimaryAnnotations().addAnnotation("otherPrimaryAnnotation", "do not delete");
		namedAnnotations.getAdditionalAnnotations().addAnnotation("concreteType", "org.sagebionetworks.repo.model.FileEntity");
		namedAnnotations.getAdditionalAnnotations().addAnnotation("otherAdditionalAnnotation", "also do not delete");
	}
}