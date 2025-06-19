package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
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
	JSONEntityHttpMessageConverter converter;

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
		
		Set<Class <? extends JSONEntity>> set = new HashSet<>();
		set.add(CreateSchemaRequest.class);
		converter = new JSONEntityHttpMessageConverter(set);
		
	}

	@Test
	public void testCanRead(){
		assertTrue(converter.canRead(Project.class, MediaType.APPLICATION_JSON));
		assertFalse(converter.canRead(Object.class, MediaType.APPLICATION_JSON));
		assertTrue(converter.canRead(Project.class, new MediaType("application","json", Charset.forName("ISO-8859-1"))));
	}
	
	@Test
	public void testCanWrite(){
		assertTrue(converter.canWrite(Project.class, MediaType.APPLICATION_JSON));
		assertFalse(converter.canWrite(Object.class, MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void testRoundTrip() throws HttpMessageNotWritableException, IOException{
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

}
