package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class LocationableMetadataProviderTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	MetadataProviderFactory metadataProviderFactory;

	public void testMultipleS3Locations() throws InvalidModelException,
			NotFoundException, DatastoreException, UnauthorizedException {

		LocationData location1 = new LocationData();
		location1.setType(LocationTypeNames.awss3);
		location1.setPath("/13/998/foo.zip");
		LocationData location2 = new LocationData();
		location2.setType(LocationTypeNames.awss3);
		location2.setPath("/13/999/bar.zip");		
		
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(location1);
		locations.add(location1);

		Data mock = new Data();
		mock.setId("13");
		mock.setParentId("12");
		mock.setMd5("85e8c666f57573345d7b9fbe8d704f05");
		mock.setType(LayerTypeNames.G);
		mock.setContentType(null);
		mock.setLocations(locations);

		try {
			validateEntityHelper(mock);
			fail("expected exception not thrown");
		}
		catch(InvalidModelException ex) {
			assertEquals("Only one AWS S3 location is allowed per entity", ex.getCause().getMessage());
		}
	}
	
	@Test(expected = InvalidModelException.class)
	public void testMd5InvalidLengthTooShort() throws InvalidModelException,
			NotFoundException, DatastoreException, UnauthorizedException {

		LocationData location = new LocationData();
		location.setType(LocationTypeNames.awss3);
		location.setPath("foo.zip");
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(location);

		Code mock = new Code();
		mock.setParentId("12");
		mock.setMd5("85e8c666f57573345d7b9fbe8d704f0");
		mock.setContentType(null);
		mock.setLocations(locations);

		validateEntityHelper(mock);
	}

	@Test(expected = InvalidModelException.class)
	public void testMd5InvalidLengthTooLong() throws InvalidModelException,
			NotFoundException, DatastoreException, UnauthorizedException {

		LocationData location = new LocationData();
		location.setType(LocationTypeNames.awss3);
		location.setPath("foo.zip");
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(location);

		Data mock = new Data();
		mock.setParentId("12");
		mock.setMd5("85e8c666f57573345d7b9fbe8d704f055");
		mock.setType(LayerTypeNames.G);
		mock.setContentType(null);
		mock.setLocations(locations);

		validateEntityHelper(mock);
	}

	@Test(expected = InvalidModelException.class)
	public void testMd5InvalidCharacter() throws InvalidModelException,
			NotFoundException, DatastoreException, UnauthorizedException {

		LocationData location = new LocationData();
		location.setType(LocationTypeNames.awss3);
		location.setPath("foo.zip");
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(location);

		Study mock = new Study();
		mock.setParentId("12");
		// Letter O instead of zero
		mock.setMd5("85e8c666f57573345d7b9fbe8d704fO5");
		mock.setContentType(null);
		mock.setLocations(locations);

		validateEntityHelper(mock);
	}

	@Test
	public void testValidateContentType() throws InvalidModelException,
			NotFoundException, DatastoreException, UnauthorizedException {

		LocationData location = new LocationData();
		location.setType(LocationTypeNames.external);
		List<LocationData> locations = new LinkedList<LocationData>();
		locations.add(location);

		Data mock = new Data();
		mock.setParentId("12");
		mock.setMd5("85e8c666f57573345d7b9fbe8d704f05");
		mock.setType(LayerTypeNames.E);
		mock.setLocations(locations);

		location.setPath("foo.zip");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("application/zip", mock.getContentType());

		location.setPath("foo.tar.gz");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("application/octet-stream", mock.getContentType());

		location.setPath("foo.tgz");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("application/binary", mock.getContentType());

		location.setPath("foo.jpg");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/jpeg", mock.getContentType());

		location.setPath("foo.jpeg");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/jpeg", mock.getContentType());

		location.setPath("foo.JPG");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/jpeg", mock.getContentType());

		location.setPath("foo.JPEG");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/jpeg", mock.getContentType());

		location.setPath("foo.gif");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/gif", mock.getContentType());

		location.setPath("foo.GIF");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/gif", mock.getContentType());

		location.setPath("foo.png");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/png", mock.getContentType());

		location.setPath("foo.PNG");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("image/png", mock.getContentType());

		location.setPath("foo.pdf");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("application/pdf", mock.getContentType());

		location.setPath("foo.PDF");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("application/pdf", mock.getContentType());

		location.setPath("foo.txt");
		mock.setContentType(null);
		validateEntityHelper(mock);
		assertEquals("text/plain", mock.getContentType());

	}

	private void validateEntityHelper(Entity entity)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		List<TypeSpecificMetadataProvider<Entity>> providers = metadataProviderFactory
				.getMetadataProvider(EntityType.getNodeTypeForClass(entity
						.getClass()));

		for (TypeSpecificMetadataProvider<Entity> provider : providers) {
			provider.validateEntity(entity, new EntityEvent(EventType.CREATE,
					null, null));
		}
	}
}
