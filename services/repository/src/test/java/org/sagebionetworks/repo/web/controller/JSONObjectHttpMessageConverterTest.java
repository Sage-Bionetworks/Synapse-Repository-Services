package org.sagebionetworks.repo.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpInputMessage;

@ExtendWith(MockitoExtension.class)
class JSONObjectHttpMessageConverterTest {
	
	@Mock
	private HttpInputMessage mockMessage;
	
	private void mockMessageBody(String body) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(body.getBytes());
		when(mockMessage.getBody()).thenReturn(is);
	}

	@Test
	void testReadObjectHappyCase() throws Exception {
		
		mockMessageBody("{\"foo\":\"bar\"}");
		
		JSONObjectHttpMessageConverter converter = new JSONObjectHttpMessageConverter();
		
		// method under test
		JSONObject converted = converter.read(null, mockMessage);
		
		assertEquals("bar", converted.get("foo"));
	}

	@Test
	void testReadObjectInvalidJSON() throws Exception {
		
		mockMessageBody("not-valid-json");
		
		JSONObjectHttpMessageConverter converter = new JSONObjectHttpMessageConverter();		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			converter.read(null, mockMessage);
		});
		
		assertEquals("Request body is not valid JSON.", e.getMessage());
	}

}
