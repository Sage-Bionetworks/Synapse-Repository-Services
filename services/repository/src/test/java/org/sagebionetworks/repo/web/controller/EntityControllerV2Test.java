package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
class EntityControllerV2Test extends AbstractAutowiredControllerJunit5TestBase{

	@Autowired
	private UserManager userManager;

	@Autowired
	private NodeManager nodeManager;

	private List<String> toDelete;
	private Long adminUserId;
	private String adminUserIdString;

	@BeforeEach
	public void setUp(){
		assertNotNull(userManager);
		assertNotNull(nodeManager);

		adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();

		toDelete = new ArrayList<>();

	}
	@AfterEach
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(adminUserId);
		for(String id: toDelete){
			try {
				nodeManager.delete(adminUserInfo, id);
			} catch (Exception e) {
				// Try even if it fails.
			}
		}
	}

	@Test
	public void testAnnotationsCRUD() throws Exception {
		Project p = new Project();
		p.setName("AnnotCrud");
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		AnnotationsV2 annos = entityServletHelper.getEntityAnnotationsV2(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		AnnotationsV2TestUtils.putAnnotations(annos,"doubleAnno", "45.0001", AnnotationsV2ValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos,"string", "A string", AnnotationsV2ValueType.STRING);
		// Updte them
		AnnotationsV2 annosClone = entityServletHelper.updateAnnotationsV2(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = AnnotationsV2Utils.getSingleValue(annosClone, "string");
		assertEquals("A string", value);
		assertEquals("45.0001", AnnotationsV2Utils.getSingleValue(annosClone, "doubleAnno"));

	}

	@Test
	public void testNaNAnnotationsCRUD() throws Exception {
		Project p = new Project();
		p.setName("AnnotCrud");
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		AnnotationsV2 annos = entityServletHelper.getEntityAnnotationsV2(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		AnnotationsV2TestUtils.putAnnotations(annos,"doubleAnno", "nan", AnnotationsV2ValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos,"string", "A string", AnnotationsV2ValueType.STRING);
		// Update them
		AnnotationsV2 annosClone = entityServletHelper.updateAnnotationsV2(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = AnnotationsV2Utils.getSingleValue(annosClone, "string");
		assertEquals("A string", value);
		assertEquals("nan", AnnotationsV2Utils.getSingleValue(annosClone, "doubleAnno"));

	}
}
