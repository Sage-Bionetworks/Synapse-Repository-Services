package org.sagebionetworks.openapi.model;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class OpenApiJsonSchema extends JsonSchema {
	
	private static final String P_DISCRIMINATOR = "discriminator";
	
	private Discriminator discriminator;

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		JSONObjectAdapter result = super.initializeFromJSONObject(adapter);
		if (!result.isNull(P_DISCRIMINATOR)) {
			discriminator = new Discriminator(result.getJSONObject(P_DISCRIMINATOR));
		} else {
			discriminator = null;
		}
		return result;
	}
	
	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		JSONObjectAdapter result = super.writeToJSONObject(adapter);
		
		if (discriminator != null) {
			result.put(P_DISCRIMINATOR, discriminator.writeToJSONObject(result.createNew()));
		}
		
		return result;
	}
	
	public Discriminator getDiscriminator() {
		return discriminator;
	}
	
	public OpenApiJsonSchema setDiscriminator(Discriminator discriminator) {
		this.discriminator = discriminator;
		return this;
	}
	
	
}
