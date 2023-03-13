package org.sagebionetworks.swagger.generator;

import org.json.JSONObject;
import org.sagebionetworks.openapi.datamodel.OpenAPISpecModel;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.gson.Gson;

/**
 * This generator generates a JSONObject based on a OpenAPISpecModel that conforms to the OpenAPI
 * specification standards.
 * 
 * @author lli
 *
 */
public class SwaggerSpecJsonGenerator {
	
		public static JSONObject generateJson(OpenAPISpecModel swaggerSpecModel) throws JSONObjectAdapterException {
			Gson gson = new Gson();
			String json = gson.toJson(swaggerSpecModel);
			return new JSONObject(json);
		}
}
