package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ObjectTypeSerializerTest {
	
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
		project.setCreationDate(new Date(System.currentTimeMillis()));
		project.setUri("/project/123");
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
		project.setCreationDate(new Date(System.currentTimeMillis()));
		project.setUri("/project/123");
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

}
