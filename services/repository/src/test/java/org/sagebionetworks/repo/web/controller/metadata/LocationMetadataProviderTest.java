package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.ObjectType;

public class LocationMetadataProviderTest {
	
	
	@Test
	public void testValidate() throws InvalidModelException{
		LocationMetadataProvider provider = new LocationMetadataProvider();
		Location mock = new Location();
		mock.setParentId("12");
		mock.setMd5sum("85e8c666f57573345d7b9fbe8d704f05");
		mock.setType(Location.LocationTypeNames.awss3.name());

		mock.setPath("foo.zip");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("application/zip", mock.getContentType());

		mock.setPath("foo.tar.gz");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("application/octet-stream", mock.getContentType());

		mock.setPath("foo.tgz");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("application/binary", mock.getContentType());

		mock.setPath("foo.jpg");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.jpeg");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.JPG");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.JPEG");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/jpeg", mock.getContentType());

		mock.setPath("foo.gif");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/gif", mock.getContentType());

		mock.setPath("foo.GIF");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/gif", mock.getContentType());

		mock.setPath("foo.png");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/png", mock.getContentType());

		mock.setPath("foo.PNG");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("image/png", mock.getContentType());

		mock.setPath("foo.pdf");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("application/pdf", mock.getContentType());

		mock.setPath("foo.PDF");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("application/pdf", mock.getContentType());
		
		mock.setPath("foo.txt");
		mock.setContentType(null);
		provider.validateEntity(mock, new EntityEvent(EventType.CREATE, null, null));
		assertEquals("text/plain", mock.getContentType());

		
	}
}
