package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class ObjectTypeSerializerTest extends AbstractAutowiredControllerTestBase {
	
	MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;
	
	@Before
	public void before(){
		assertNotNull(objectTypeSerializer);
	}
	
	@Test
	public void testJson(){
		// Try a few objects
		Project project = new Project();
		project.setName("name");
		project.setParentId("123");
		project.setEtag("202");
		project.setCreatedOn(new Date(System.currentTimeMillis()));
		// Now write it to JSON
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Now write the object to the stream
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=UTF-8");
		objectTypeSerializer.serializer(out, headers, project, MediaType.APPLICATION_JSON);
		
		// Now reverse the process
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Project clone = objectTypeSerializer.deserialize(in, headers, Project.class, MediaType.APPLICATION_JSON);
		assertNotNull(clone);
		assertEquals(project, clone);
	}
	@Ignore
	@Test
	public void testXML(){
		// Try a few objects
		Project project = new Project();
		project.setName("name");
		project.setParentId("123");
		project.setEtag("202");
		project.setCreatedOn(new Date(System.currentTimeMillis()));
		// Now write it to JSON
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Now write the object to the stream
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/xml; charset=UTF-8");
		objectTypeSerializer.serializer(out, headers, project, MediaType.APPLICATION_XML);
		
		// Now reverse the process
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Project clone = objectTypeSerializer.deserialize(in, headers, Project.class, MediaType.APPLICATION_XML);
		assertNotNull(clone);
		assertEquals(project, clone);
	}
	
	/**
	 * Test for PLFM-834
	 * @throws IOException 
	 * @throws HttpMessageNotWritableException 
	 * @throws JSONObjectAdapterException 
	 */
	@Test
	public void testPLFM_834Simple() throws HttpMessageNotWritableException, IOException, JSONObjectAdapterException{
		// Make sure we can write an entity to json
		Project project = new Project();
		project.setName("name");
		project.setParentId("123");
		project.setEtag("202");
		project.setCreatedOn(new Date(System.currentTimeMillis()));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpOutputMessage message = new HttpOutputMessage() {
			@Override
			public HttpHeaders getHeaders() {
				return new HttpHeaders();
			}
			@Override
			public OutputStream getBody() throws IOException {
				return out;
			}
		};
		// Write this to the stream
		objectTypeSerializer.write(project, mediaType, message);
		String outString = new String(out.toByteArray(), Charset.forName("UTF-8"));
		System.out.println(outString);
		assertFalse("The resulting JSON should not contain the schema",outString.indexOf("jsonschema") > -1);
		// Make sure we can create a new entity with the path.
		Project clone = EntityFactory.createEntityFromJSONString(outString, Project.class);
		assertEquals(project, clone);
	}
	
	@Test
	public void testPLFM_834EntityPath() throws HttpMessageNotWritableException, IOException, JSONObjectAdapterException{
		// Make sure we can write an entity to json
		EntityPath path = new EntityPath();
		path.setPath(new ArrayList<EntityHeader>());
		EntityHeader header = new EntityHeader();
		header.setId("123");
		header.setName("This is my name!");
		header.setType("Some type");
		path.getPath().add(new EntityHeader());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpOutputMessage message = new HttpOutputMessage() {
			@Override
			public HttpHeaders getHeaders() {
				return new HttpHeaders();
			}
			@Override
			public OutputStream getBody() throws IOException {
				return out;
			}
		};
		// Write this to the stream
		objectTypeSerializer.write(path, mediaType, message);
		String outString = new String(out.toByteArray(), Charset.forName("UTF-8"));
		System.out.println(outString);
		assertFalse("The resulting JSON should not contain the schema",outString.indexOf("jsonschema") > -1);
		// Make sure we can create a new entity with the path.
		EntityPath clone = EntityFactory.createEntityFromJSONString(outString, EntityPath.class);
		assertEquals(path, clone);
	}
	
	@Test
	public void testPLFM_834Paginated() throws HttpMessageNotWritableException, IOException, JSONObjectAdapterException, JSONException{
		// Make sure we can write an entity to json
		List<EntityHeader> results = new ArrayList<EntityHeader>(); 
		EntityHeader h1 = new EntityHeader();
		h1.setId("123");
		h1.setName("Joe");
		h1.setType("type");
		results.add(h1);
		PaginatedResults<EntityHeader> paged = PaginatedResults.createWithLimitAndOffset(results, 101L, 0L);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		HttpOutputMessage message = new HttpOutputMessage() {
			@Override
			public HttpHeaders getHeaders() {
				return new HttpHeaders();
			}
			@Override
			public OutputStream getBody() throws IOException {
				return out;
			}
		};
		// Write this to the stream
		objectTypeSerializer.write(paged, mediaType, message);
		String outString = new String(out.toByteArray(), Charset.forName("UTF-8"));
		System.out.println(outString);
		assertFalse("The resulting JSON should not contain the schema",outString.indexOf("jsonschema") > -1);
		// Make sure we can create a new entity with the path.
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(outString);
		PaginatedResults<EntityHeader> clone = PaginatedResults.createFromJSONObjectAdapter(adapter, EntityHeader.class);
		assertEquals(paged, clone);
	}

}
