package org.sagebionetworks.openapi.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

@ExtendWith(MockitoExtension.class)
public class ComponentsTest {
	@Mock
	JSONObjectAdapter mockAdapter;
	@Mock
	JSONObjectAdapter mockSchemasAdapter;
	@Mock
	JSONObjectAdapter mockSecuritySchemesAdapter;

	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			new Components().initializeFromJSONObject(mockAdapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		Map<String, OpenApiJsonSchema> schemaMap = new HashMap<>();
		OpenApiJsonSchema schema = new OpenApiJsonSchema();
		schema.setType(Type.integer);
		schemaMap.put("test.class", schema);
		Map<String, SecurityScheme> securitySchemes = new HashMap<>();
		securitySchemes.put("bearerAuth", new SecurityScheme().withType("http").withScheme("bearer"));
		Components components = new Components().withSchemas(schemaMap).withSecuritySchemes(securitySchemes);

		JSONObjectAdapterImpl schemaAdapterImpl1 = new JSONObjectAdapterImpl();

		JSONObjectAdapterImpl securitySchemesAdapterImpl1 = new JSONObjectAdapterImpl();

		doReturn(mockSchemasAdapter, schemaAdapterImpl1, mockSecuritySchemesAdapter, securitySchemesAdapterImpl1).when(mockAdapter).createNew();

		// Call under test
		components.writeToJSONObject(mockAdapter);

		verify(mockAdapter).put("schemas", mockSchemasAdapter);
		verify(mockSchemasAdapter).put("test.class", schemaAdapterImpl1);
		verify(mockAdapter).put("securitySchemes", mockSecuritySchemesAdapter);
		verify(mockSecuritySchemesAdapter).put("bearerAuth", securitySchemesAdapterImpl1);
	}
}
