package org.sagebionetworks.openapi.datamodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class SecuritySchemeTest {
	@Mock
	JSONObjectAdapter mockAdapter;

	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			new SecurityScheme().initializeFromJSONObject(mockAdapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		SecurityScheme securityScheme = new SecurityScheme().withType("http").withScheme("bearer");

		// call under test
		securityScheme.writeToJSONObject(mockAdapter);
		
		Mockito.verify(mockAdapter).put("type", "http");
		Mockito.verify(mockAdapter).put("scheme", "bearer");
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new SecurityScheme().writeToJSONObject(mockAdapter);
		});
		assertEquals("The 'type' is a required attribute of SecurityScheme.", exception.getMessage());
	}
}
