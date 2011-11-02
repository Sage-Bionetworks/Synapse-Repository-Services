package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.MetadataProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LocationMetadataProviderTest {

	@Autowired
	MetadataProviderFactory metadataProviderFactory;

	TypeSpecificMetadataProvider<Entity> locationMetadataProvider;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		locationMetadataProvider = metadataProviderFactory
				.getMetadataProvider(EntityType.location);
	}


	@Test(expected=InvalidModelException.class)
	public void testMd5InvalidLengthTooShort() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Location mock = new Location();
		mock.setParentId("12");
		mock.setMd5sum("85e8c666f57573345d7b9fbe8d704f0");
		mock.setType(LocationTypeNames.awss3);
		mock.setPath("foo.zip");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
	}
	
	@Test(expected=InvalidModelException.class)
	public void testMd5InvalidLengthTooLong() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Location mock = new Location();
		mock.setParentId("12");
		mock.setMd5sum("85e8c666f57573345d7b9fbe8d704f055");
		mock.setType(LocationTypeNames.awss3);
		mock.setPath("foo.zip");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
	}

	@Test(expected=InvalidModelException.class)
	public void testMd5InvalidCharacter() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Location mock = new Location();
		mock.setParentId("12");
		// Letter O instead of zero
		mock.setMd5sum("85e8c666f57573345d7b9fbe8d704fO5");
		mock.setType(LocationTypeNames.awss3);
		mock.setPath("foo.zip");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
	}
	
	@Test
	public void testValidateContentType() throws InvalidModelException,
			NotFoundException, DatastoreException, UnauthorizedException {
		Location mock = new Location();
		mock.setParentId("12");
		mock.setMd5sum("85e8c666f57573345d7b9fbe8d704f05");
		mock.setType(LocationTypeNames.awss3);

		mock.setPath("foo.zip");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("application/zip", mock.getContentType());

		mock.setPath("foo.tar.gz");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("application/octet-stream", mock.getContentType());

		mock.setPath("foo.tgz");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("application/binary", mock.getContentType());

		mock.setPath("foo.jpg");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.jpeg");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.JPG");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.JPEG");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.gif");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/gif", mock.getContentType());

		mock.setPath("foo.GIF");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/gif", mock.getContentType());

		mock.setPath("foo.png");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/png", mock.getContentType());

		mock.setPath("foo.PNG");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("image/png", mock.getContentType());

		mock.setPath("foo.pdf");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("application/pdf", mock.getContentType());

		mock.setPath("foo.PDF");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("application/pdf", mock.getContentType());

		mock.setPath("foo.txt");
		mock.setContentType(null);
		locationMetadataProvider.validateEntity(mock, new EntityEvent(
				EventType.CREATE, null, null));
		assertEquals("text/plain", mock.getContentType());

	}
}
