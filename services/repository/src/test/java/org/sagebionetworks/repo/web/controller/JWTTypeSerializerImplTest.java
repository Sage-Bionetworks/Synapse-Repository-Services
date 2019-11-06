package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.oauth.JWTWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

@RunWith(MockitoJUnitRunner.class)
public class JWTTypeSerializerImplTest {

	JWTTypeSerializerImpl jwtTypeSerializerImpl = new JWTTypeSerializerImpl();

	private static final MediaType APPLICATON_JWT = new MediaType("application", "jwt");
	private static final MediaType APPLICATON_JSON = new MediaType("application", "json");

	@Mock
	HttpInputMessage mockHttpInputMessage;
	
	@Mock
	HttpOutputMessage mockHttpOutputMessage;

	@Test
	public void testCanRead() {
		// method under test
		assertFalse(jwtTypeSerializerImpl.canRead(JWTWrapper.class, APPLICATON_JWT));
	}

	@Test
	public void testCanWrite() {
		// method under test
		assertTrue(jwtTypeSerializerImpl.canWrite(JWTWrapper.class, APPLICATON_JWT));
					
		// method under test
		assertTrue(jwtTypeSerializerImpl.canWrite(JWTWrapper.class, APPLICATON_JSON));

		// method under test
		assertFalse(jwtTypeSerializerImpl.canWrite(String.class, APPLICATON_JWT));
		
		// method under test
		assertFalse(jwtTypeSerializerImpl.canWrite(String.class, APPLICATON_JSON));
	}

	@Test
	public void testGetSupportedMediaTypes() {
		// method under test
		assertEquals(Collections.singletonList(MediaType.ALL), jwtTypeSerializerImpl.getSupportedMediaTypes());
	}

	@Test()
	public void testRead() throws Exception {
		try {
			// method under test
			jwtTypeSerializerImpl.read(JWTWrapper.class, mockHttpInputMessage);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testWrite() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		when(mockHttpOutputMessage.getBody()).thenReturn(baos);
		
		HttpHeaders headers = new HttpHeaders();
		when(mockHttpOutputMessage.getHeaders()).thenReturn(headers);
		
		String content = "some content";
		
		// method under test
		jwtTypeSerializerImpl.write(new JWTWrapper(content), APPLICATON_JWT, mockHttpOutputMessage);
		
		assertEquals(content, baos.toString());
		
		// check content type and content length headers
		assertEquals((long)content.length(), headers.getContentLength());
		assertTrue(APPLICATON_JWT.isCompatibleWith(headers.getContentType()));
	}

	@Test
	public void testDeserialize() {
		try {
			// method under test
			jwtTypeSerializerImpl.deserialize(new ByteArrayInputStream(new byte[] {}), new HttpHeaders(), APPLICATON_JWT);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testSerializer() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		String content = "some content";
		
		HttpHeaders headers = new HttpHeaders();
		
		// method under test
		jwtTypeSerializerImpl.serializer(baos, headers, new JWTWrapper(content), APPLICATON_JWT);
		assertEquals(content, baos.toString());
		
		// check content type and content length headers
		assertEquals((long)content.length(), headers.getContentLength());
		assertTrue(APPLICATON_JWT.isCompatibleWith(headers.getContentType()));
	}

}
