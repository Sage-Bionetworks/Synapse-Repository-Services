package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.amazonaws.util.StringInputStream;


public class JSONEntityHttpMessageConverterTest {

	
	Project project;
	
	HttpOutputMessage mockOutMessage;
	HttpInputMessage mockInMessage;
	ByteArrayOutputStream outStream;
	HttpHeaders mockHeaders;

	
	@Before
	public void before() throws IOException{	
		project = new Project();
		project.setName("foo-bar");
		
		// Create the mocks
		outStream = new ByteArrayOutputStream();
		mockOutMessage = Mockito.mock(HttpOutputMessage.class);
		Mockito.when(mockOutMessage.getBody()).thenReturn(outStream);
		mockInMessage = Mockito.mock(HttpInputMessage.class);
		mockHeaders = Mockito.mock(HttpHeaders.class);
		
		Mockito.when(mockInMessage.getHeaders()).thenReturn(mockHeaders);
		Mockito.when(mockOutMessage.getHeaders()).thenReturn(mockHeaders);
		Mockito.when(mockHeaders.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
		
	}

	@Test
	public void testCanRead(){
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		assertTrue(converter.canRead(Project.class, MediaType.APPLICATION_JSON));
		assertFalse(converter.canRead(Object.class, MediaType.APPLICATION_JSON));
		assertTrue(converter.canRead(Project.class, new MediaType("application","json", Charset.forName("ISO-8859-1"))));
	}
	
	@Test
	public void testCanWrite(){
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		assertTrue(converter.canWrite(Project.class, MediaType.APPLICATION_JSON));
		assertFalse(converter.canWrite(Object.class, MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void testRoundTrip() throws HttpMessageNotWritableException, IOException{
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		// Write it out.
		converter.write(project, MediaType.APPLICATION_JSON, mockOutMessage);
		
		ByteArrayInputStream in  = new ByteArrayInputStream(outStream.toByteArray());
		Mockito.when(mockInMessage.getBody()).thenReturn(in);
		// Make sure we can read it back
		JSONEntity results = converter.read(Project.class, mockInMessage);
		assertEquals(project, results);
	}
	
	@Test
	public void testRoundTripWithPlainTextMediaType() throws HttpMessageNotWritableException, IOException{
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		// Write it out.
		converter.write(project, MediaType.TEXT_PLAIN, mockOutMessage);
		
		ByteArrayInputStream in  = new ByteArrayInputStream(outStream.toByteArray());
		Mockito.when(mockInMessage.getBody()).thenReturn(in);
		// Make sure we can read it back
		JSONEntity results = converter.read(Project.class, mockInMessage);
		assertEquals(project, results);
	}
	
	@Test
	public void testErrorResponseRoundTripWithPlainTextMediaType() throws HttpMessageNotWritableException, IOException{
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		ErrorResponse error = new ErrorResponse();
		error.setReason("foo");
		// Write it out.
		converter.write(error, MediaType.TEXT_PLAIN, mockOutMessage);
		
		ByteArrayInputStream in  = new ByteArrayInputStream(outStream.toByteArray());
		assertEquals("foo", IOUtils.toString(in));
	}
	
	@Test
	public void testConvertEntityToPlainText() throws Exception {
		ErrorResponse error = new ErrorResponse();
		error.setReason("foo");
		assertEquals("foo", JSONEntityHttpMessageConverter.convertEntityToPlainText(error));
	}

	@Test 
	public void testReadToString() throws IOException{
		String value = "This string should make a round trip!";
		StringReader reader = new StringReader(value);
		String clone = JSONEntityHttpMessageConverter.readToString(reader);
		assertEquals(value, clone);
	}
	
	@Test
	public void testReadEntity() throws JSONObjectAdapterException, IOException{
		ExampleEntity entity = new ExampleEntity();
		entity.setName("name");
		// this version requires a class name fo the entity type.
		entity.setDoubleList(new ArrayList<Double>());
		entity.getDoubleList().add(123.45);
		entity.getDoubleList().add(4.56);
		// To string
		String jsonString =EntityFactory.createJSONStringForEntity(entity);
		StringReader reader = new StringReader(jsonString);
		ExampleEntity clone = (ExampleEntity) JSONEntityHttpMessageConverter.readEntity(reader);
		assertEquals(entity, clone);
	}
	
	@Test (expected=JSONObjectAdapterException.class)
	public void testReadEntityNullType() throws JSONObjectAdapterException, IOException{
		ExampleEntity entity = new ExampleEntity();
		entity.setName("name");
		// this version requires a class name fo the entity type.
		entity.setConcreteType(null);
		entity.setDoubleList(new ArrayList<Double>());
		entity.getDoubleList().add(123.45);
		entity.getDoubleList().add(4.56);
		// To string
		String jsonString =EntityFactory.createJSONStringForEntity(entity);
		StringReader reader = new StringReader(jsonString);
		ExampleEntity clone = (ExampleEntity) JSONEntityHttpMessageConverter.readEntity(reader);
	}
	
	/**
	 * This test was added for PLFM-1280.
	 * @throws JSONObjectAdapterException
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testCreateEntityFromAdapterClassNotFound() throws JSONObjectAdapterException{
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.FakeClass");
		JSONEntityHttpMessageConverter.createEntityFromeAdapter(adapter);
	}

	/**
	 * This test was added for PLFM-1280.
	 * @throws JSONObjectAdapterException
	 */
	@Test (expected=JSONObjectAdapterException.class)
	public void testCreateEntityFromAdapterBadJSON() throws JSONObjectAdapterException{
		// Test a valid entity type with a field that does not exist on that type.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("entityType", ExampleEntity.class.getName());
		adapter.put("notAField", "shoudld not exist");
		JSONEntityHttpMessageConverter.createEntityFromeAdapter(adapter);
	}
	
	@Test
	public void testPLFM_2079() throws Exception{
		// In the past we used the "entityType" field to determine which implementation Entity to create when a caller passed an JSON string.
		// This was specific to Entity so when the JSON schema project tackled the same problem "concreteType" was used instead of entityType.
		// We then switch Entities to use concreteType but we did not want this to be a breaking API change.
		// So when a old client uses "entityType" it should not break.
		
		// Create some JSON using a project entity.
		Project project = new Project();
		project.setName("someProject");
		project.setParentId("syn123");
		project.setId("syn456");
		JSONObject jsonObject = new JSONObject();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
		project.writeToJSONObject(adapter);
		// Swap the concreteType field with entityType
		String type = jsonObject.getString("concreteType");
		jsonObject.remove("concreteType");
		// replace it with entity type
		jsonObject.put("entityType", type);
		String json = adapter.toJSONString();
		assertTrue(json.indexOf("entityType") > 0);
		assertFalse(json.indexOf("concreteType") > 0);
		// Now make sure we can parse the json
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		Mockito.when(mockInMessage.getBody()).thenReturn(new StringInputStream(json));
		try{
			Project clone = (Project) converter.read(Entity.class, mockInMessage);
			assertNotNull(clone);
			// It should match the original
			assertEquals(project, clone);
		}catch(Exception e){
			throw new RuntimeException(json,e);
		}
	}
	
}
