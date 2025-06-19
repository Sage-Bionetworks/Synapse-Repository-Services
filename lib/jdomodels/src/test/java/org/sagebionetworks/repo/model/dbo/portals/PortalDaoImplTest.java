package org.sagebionetworks.repo.model.dbo.portals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.portals.Portal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PortalDaoImplTest {

	@Autowired
	private PortalDao portalDao;
	
	private Long userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	
	@BeforeEach
	public void before() {
		portalDao.truncateAll();
	}

	@AfterEach
	public void after() {
		portalDao.truncateAll();
	}
	
	@Test
	public void testCreatePortal() {
		Portal expected = new Portal()
			.setName("portalOne")
			.setUrl("https://portalone.synapse.org")
			.setCreatedBy(userId.toString())
			.setModifiedBy(userId.toString());
		
		Portal portalOne = portalDao.createPortal(userId, expected.getName(), expected.getUrl());
		
		expected
			.setId(portalOne.getId())
			.setEtag(portalOne.getEtag())
			.setCreatedOn(portalOne.getCreatedOn())
			.setModifiedOn(portalOne.getModifiedOn());
		
		assertEquals(expected, portalOne);
		
		assertEquals("A portal with the given name and/or URL already exists.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test, same name (different case)
			portalDao.createPortal(userId, "portalone", "https://portaltwo.synapse.org");	
		}).getMessage());
		
		assertEquals("A portal with the given name and/or URL already exists.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test, same url (different case)
			portalDao.createPortal(userId, "portalTwo", "https://portalOne.synapse.org");	
		}).getMessage());
		
		// Call under test
		Portal portalTwo = portalDao.createPortal(userId, "portalTwo", "https://portaltwo.synapse.org");
		
		expected
			.setId(portalTwo.getId())
			.setEtag(portalTwo.getEtag())
			.setCreatedOn(portalTwo.getCreatedOn())
			.setModifiedOn(portalTwo.getModifiedOn());
		
	}
	
	@Test
	public void testUpdatePortal() throws InterruptedException {
		Portal portalOne = portalDao.createPortal(userId, "portalOne", "https://portalone.synapse.org");
		Portal portalTwo = portalDao.createPortal(userId, "portalTwo", "https://portaltwo.synapse.org");
		
		assertEquals("A portal with the given name and/or URL already exists.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			portalDao.updatePortal(userId, portalOne.getId(), portalTwo.getName(), portalOne.getUrl());
		}).getMessage());
		
		assertEquals("A portal with the given name and/or URL already exists.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			portalDao.updatePortal(userId, portalOne.getId(), portalOne.getName(), portalTwo.getUrl());
		}).getMessage());
		
		Thread.sleep(50L);
		
		// Call under test
		Portal portalOneUpdated = portalDao.updatePortal(userId, portalOne.getId(), portalOne.getName() + " updated", portalOne.getUrl());
		
		assertNotEquals(portalOneUpdated.getEtag(), portalOne.getEtag());
		assertNotEquals(portalOneUpdated.getModifiedOn(), portalOne.getModifiedOn());
		assertNotEquals(portalOneUpdated.getName(), portalOne.getName());
		
		assertEquals(portalOne
			.setEtag(portalOneUpdated.getEtag())
			.setModifiedOn(portalOneUpdated.getModifiedOn())
			.setName(portalOneUpdated.getName()),
			portalOneUpdated
		);
		
	}
	
	@Test
	public void testGetPortal() {
		assertTrue(portalDao.getPortal("123").isEmpty());
		
		Portal portalOne = portalDao.createPortal(userId, "portalOne", "https://portalone.synapse.org");
	
		// Call under test
		assertEquals(portalDao.getPortal(portalOne.getId()).orElseThrow(), portalOne);
	}
	
	@Test
	public void testDeletePortal() {
		// Call under test
		portalDao.deletePortal("123");
		
		Portal portalOne = portalDao.createPortal(userId, "portalOne", "https://portalone.synapse.org");
		
		// Call under test
		portalDao.deletePortal(portalOne.getId());
		
		assertTrue(portalDao.getPortal(portalOne.getId()).isEmpty());
	}
	
	@Test
	public void testGetPortalPage() {
		long limit = 10;
		long offset = 0;
		
		assertTrue(portalDao.getPortalPage(limit, offset).isEmpty());
		
		Portal portalOne = portalDao.createPortal(userId, "portalOne", "https://portalone.synapse.org");
		Portal portalTwo = portalDao.createPortal(userId, "portalTwo", "https://portaltwo.synapse.org");
		
		assertEquals(List.of(portalOne, portalTwo), portalDao.getPortalPage(limit, offset));
		
		assertEquals(List.of(portalOne), portalDao.getPortalPage(1, 0));
		assertEquals(List.of(portalTwo), portalDao.getPortalPage(1, 1));
		
	}
	
}
