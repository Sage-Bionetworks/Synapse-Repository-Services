package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.sample.Example;
import org.sagebionetworks.sample.ExampleContainer;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JSONEntityHttpMessageConverterTest {
	
	ExampleContainer container;
	
	HttpOutputMessage mockOutMessage;
	HttpInputMessage mockInMessage;
	ByteArrayOutputStream outStream;
	HttpHeaders mockHeaders;

	
	@Before
	public void before() throws IOException{
		// This is the entity to write
		container = new ExampleContainer();
		container.setExampleList(new ArrayList<Example>());
		container.setExampleSet(new HashSet<Example>());
		
		for(int i=0; i<5; i++){
			Example example = new Example();
			example.setName("name:"+i);
			example.setQuantifier("quntifier:"+i);
			example.setType("type:"+i);
			if(i %2 == 0){
				container.getExampleList().add(example);
			}else{
				container.getExampleSet().add(example);
			}
		}
		
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
		assertTrue(converter.canRead(ExampleContainer.class, MediaType.APPLICATION_JSON));
		assertFalse(converter.canRead(Object.class, MediaType.APPLICATION_JSON));
		assertTrue(converter.canRead(ExampleContainer.class, new MediaType("application","json", Charset.forName("ISO-8859-1"))));
	}
	
	@Test
	public void testCanWrite(){
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		assertTrue(converter.canWrite(ExampleContainer.class, MediaType.APPLICATION_JSON));
		assertFalse(converter.canWrite(Object.class, MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void testRoundTrip() throws HttpMessageNotWritableException, IOException{
		JSONEntityHttpMessageConverter converter = new JSONEntityHttpMessageConverter();
		// Write it out.
		converter.write(container, MediaType.APPLICATION_JSON, mockOutMessage);
		
		ByteArrayInputStream in  = new ByteArrayInputStream(outStream.toByteArray());
		Mockito.when(mockInMessage.getBody()).thenReturn(in);
		// Make sure we can read it back
		JSONEntity results = converter.read(ExampleContainer.class, mockInMessage);
		assertEquals(container, results);
	}

}
