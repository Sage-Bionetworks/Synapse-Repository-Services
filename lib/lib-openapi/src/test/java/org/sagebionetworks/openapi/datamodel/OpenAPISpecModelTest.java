package org.sagebionetworks.openapi.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

public class OpenAPISpecModelTest {
	
	@Test
	public void testGeneratesCorrectJsonBasicController() throws Exception {
		InputStream is = OpenAPISpecModel.class.getClassLoader().getResourceAsStream("BasicExampleControllerOpenAPISpec.json");
		assertNotNull(is);
		String jsonTxt = IOUtils.toString(is, "UTF-8");
		JSONObject expectedJson = new JSONObject(jsonTxt);
		
		Gson gson = new Gson();
		OpenAPISpecModel swaggerSpecModel = gson.fromJson(expectedJson.toString(), OpenAPISpecModel.class);
		assertNotNull(swaggerSpecModel);
		
		JSONObject generatedJson = swaggerSpecModel.generateJSON();
		assertEquals(expectedJson.toString(), generatedJson.toString());
    }
}
