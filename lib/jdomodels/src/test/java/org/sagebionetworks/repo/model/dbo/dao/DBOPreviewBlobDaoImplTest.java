package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.image.ImagePreviewUtilsTest;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOPreviewBlobDaoImplTest {

	@Autowired
	DBOPreviewBlobDao previewBlobDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	NodeDAO nodeDao;
	
	Node project;
	
	@Before
	public void before() throws NotFoundException, DatastoreException{
		project = NodeTestUtils.createNew(idGenerator.generateNewId().toString());
		project.setParentId(nodeDao.getNodeIdForPath("root"));
		String id = nodeDao.createNew(project);
		assertNotNull(id);
		project.setId(id);
	}
	
	@After
	public void after() throws NotFoundException, DatastoreException{
		if(project != null){
			nodeDao.delete(project.getId());
		}
	}
	
	@Test
	public void testCreatePreview() throws IOException, DatastoreException, NotFoundException{
		// Make sure we can create a preview for a node.
		Long tokenId = idGenerator.generateNewId();
		String fileName = "images/tallSkinny.jpg";
		InputStream in = ImagePreviewUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", in);
		Long ownerId = KeyFactory.stringToKey(project.getId());
		previewBlobDao.createNewPreview(in, ownerId, tokenId);
		// Now make sure we can get it back
		byte[] imageBytes = previewBlobDao.getPreview(ownerId, tokenId);
		assertNotNull(imageBytes);
		assertTrue(imageBytes.length > 0);
		assertTrue("A preview image should always be under 100K",imageBytes.length < 100*1000);
		
	}
}
