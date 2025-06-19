package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class FormEncodedMessageConverterTest {
	private static final Charset CHARSET = Charset.forName("ISO-8859-1");
	
	@Mock
	HttpInputMessage mockInMessage;
	
	@Mock
	HttpHeaders mockHeaders;
	
	FormEncodedMessageConverter converter;
	
	@BeforeEach
	void setUp() throws Exception {

		converter = new FormEncodedMessageConverter();
	}

	@Test
	public void testConvertFormEncodedDataToJSONString_HappyCase() throws JSONException, UnsupportedEncodingException {
		assertEquals("{\"foo/\":\"bar\",\"bar\":\"baz/\"}", 
				FormEncodedMessageConverter.convertFormEncodedDataToJSONString("foo%2F=bar&bar=baz%2F", CHARSET));
	}
	
	@Test
	public void testConvertFormEncodedDataToJSONString_EmptyString() throws JSONException, UnsupportedEncodingException {
		assertEquals("{}", FormEncodedMessageConverter.convertFormEncodedDataToJSONString("", CHARSET));
	}
	
	@Test
	public void testConvertFormEncodedDataToJSONString_InvalidInput() {
		assertThrows(IllegalArgumentException.class, () -> {
			FormEncodedMessageConverter.convertFormEncodedDataToJSONString("garbage", CHARSET);
		});
	}
	
	@Test
	public void testRoundTripWithFormEncodedMediaType() throws IOException  {
		// note we encode / as %2F
		String keyValueParams = "name=foo%2Fbar&concreteType=org.sagebionetworks.repo.model.Project";
		ByteArrayInputStream in  = new ByteArrayInputStream(keyValueParams.getBytes(CHARSET));
		Mockito.when(mockInMessage.getBody()).thenReturn(in);
		Mockito.when(mockInMessage.getHeaders()).thenReturn(mockHeaders);
		Mockito.when(mockHeaders.getContentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED);

		// method under test
		JSONEntity results = converter.read(Project.class, mockInMessage);
		
		Project expectedEntity = new Project();
		expectedEntity.setName("foo/bar");
		
		assertEquals(expectedEntity, results);
	}
	
	@Test
	public void testRoundTripWithFormEncodedMediaTypeAndSpecifiedCharset() throws IOException  {
		// note we encode / as %2F
		String keyValueParams = "name=foo%2Fbar&concreteType=org.sagebionetworks.repo.model.Project";
		Charset charsetUtf8=Charset.forName("UTF-8");
		MediaType mediaType = new MediaType("application", "x-www-form-urlencoded", charsetUtf8);
		ByteArrayInputStream in = new ByteArrayInputStream(keyValueParams.getBytes(charsetUtf8));
		Mockito.when(mockInMessage.getBody()).thenReturn(in);
		Mockito.when(mockInMessage.getHeaders()).thenReturn(mockHeaders);
		Mockito.when(mockHeaders.getContentType()).thenReturn(mediaType);

		// method under test
		JSONEntity results = converter.read(Project.class, mockInMessage);
		
		Project expectedEntity = new Project();
		expectedEntity.setName("foo/bar");
		
		assertEquals(expectedEntity, results);
	}
	
	@Test
	public void testCanRead() {
		assertTrue(converter.canRead(Project.class,  MediaType.APPLICATION_FORM_URLENCODED));
		assertFalse(converter.canRead(String.class, MediaType.APPLICATION_FORM_URLENCODED));
		assertFalse(converter.canRead(Project.class, MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void testCanWrite() {
		assertFalse(converter.canWrite(Project.class, MediaType.APPLICATION_FORM_URLENCODED));
	}
	
	@Test
	public void tesWrite() {
		assertThrows(IllegalStateException.class, ()->{
			converter.write(new Project(), MediaType.APPLICATION_FORM_URLENCODED, null);});
	}
	
}
