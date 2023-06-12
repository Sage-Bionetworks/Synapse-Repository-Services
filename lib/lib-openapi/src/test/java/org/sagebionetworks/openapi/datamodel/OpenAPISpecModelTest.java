package org.sagebionetworks.openapi.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.translator.ControllerModelDoclet;

import com.google.gson.Gson;

public class OpenAPISpecModelTest {
	
	@Test
	public void testGeneratesCorrectJsonBasicController() throws Exception {
		try (InputStream is = OpenAPISpecModel.class.getClassLoader().getResourceAsStream("BasicExampleControllerOpenAPISpec.json")) {
			assertNotNull(is);
			String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
			JSONObject expectedJson = new JSONObject(jsonTxt);
			
			Gson gson = new Gson();
			OpenAPISpecModel swaggerSpecModel = gson.fromJson(expectedJson.toString(), OpenAPISpecModel.class);
			assertNotNull(swaggerSpecModel);
			
			JSONObject generatedJson = swaggerSpecModel.generateJSON();
			assertEquals(expectedJson.toString(), generatedJson.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
}
