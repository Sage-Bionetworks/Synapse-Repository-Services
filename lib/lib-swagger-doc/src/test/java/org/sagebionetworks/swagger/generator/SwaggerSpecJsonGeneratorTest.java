package org.sagebionetworks.swagger.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.swagger.generator.SwaggerSpecJsonGenerator;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.swagger.datamodel.SwaggerSpecModel;
import org.sagebionetworks.swagger.datamodel.pathinfo.PathInfo;

public class SwaggerSpecJsonGeneratorTest {
	
//	public static void main(String[] args) throws Exception {
//		InputStream is = SwaggerSpecJsonGenerator.class.getClassLoader().getResourceAsStream("BasicExampleControllerPath.json");
//		assertNotNull(is);
//		String jsonTxt = IOUtils.toString(is, "UTF-8");
//		JSONObject pathsJson = new JSONObject(jsonTxt);
//		System.out.println(pathsJson.toString(5));
//		SwaggerSpecModel swaggerSpecModel = EntityFactory.createEntityFromJSONObject(pathsJson, SwaggerSpecModel.class);
//		System.out.println(swaggerSpecModel.getPaths());
//	}
	
//	@Test
//	public void testPathsCorrectJson() throws Exception {
//		InputStream is = SwaggerSpecJsonGenerator.class.getClassLoader().getResourceAsStream("BasicExampleControllerPath.json");
//		assertNotNull(is);
//		String jsonTxt = IOUtils.toString(is, "UTF-8");
//		JSONObject pathsJson = new JSONObject(jsonTxt);
//		PathInfo paths = EntityFactory.createEntityFromJSONObject(pathsJson, PathInfo.class);
//		System.out.println(paths.getOperationToEndpointInfo());
//	}
	
	@Test
	public void testCorrectJson() throws Exception {
		InputStream is = SwaggerSpecJsonGeneratorTest.class.getClassLoader().getResourceAsStream("BasicExampleControllerSwaggerSpec.json");
        assertNotNull(is);
        String jsonTxt = IOUtils.toString(is, "UTF-8");
        JSONObject controllerJson = new JSONObject(jsonTxt);
		SwaggerSpecModel swaggerSpecModel = EntityFactory.createEntityFromJSONObject(controllerJson, SwaggerSpecModel.class);
		assertNotNull(swaggerSpecModel);
		
		JSONObject resultingJson = SwaggerSpecJsonGenerator.generateJson(swaggerSpecModel);
		assertNotNull(resultingJson);
		assertEquals(controllerJson.toString(), resultingJson.toString());
    }
}
