package org.sagebionetworks.swagger.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.swagger.datamodel.SwaggerSpecModel;

import com.google.gson.Gson;

public class SwaggerSpecJsonGeneratorTest {
	
	@Test
	public void testGeneratesCorrectJsonBasicController() throws Exception {
		InputStream is = SwaggerSpecJsonGenerator.class.getClassLoader().getResourceAsStream("BasicExampleControllerSwaggerSpec.json");
		assertNotNull(is);
		String jsonTxt = IOUtils.toString(is, "UTF-8");
		JSONObject expectedJson = new JSONObject(jsonTxt);
		
		Gson gson = new Gson();
		SwaggerSpecModel swaggerSpecModel = gson.fromJson(expectedJson.toString(), SwaggerSpecModel.class);
		assertNotNull(swaggerSpecModel);
		
		JSONObject generatedJson = SwaggerSpecJsonGenerator.generateJson(swaggerSpecModel);
		assertEquals(expectedJson.toString(), generatedJson.toString());
    }
}
