package org.sagebionetworks.swagger.generator;

import org.json.JSONObject;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.swagger.datamodel.SwaggerSpecModel;

import com.google.gson.Gson;

/**
 * This generator generates a JSONObject based on a SwaggerSpecModel that conforms to the OpenAPI
 * specification standards.
 * 
 * @author lli
 *
 */
public class SwaggerSpecJsonGenerator {
	
		public static JSONObject generateJson(SwaggerSpecModel swaggerSpecModel) throws JSONObjectAdapterException {
			Gson gson = new Gson();
			String json = gson.toJson(swaggerSpecModel);
			return new JSONObject(json);
		}
}
