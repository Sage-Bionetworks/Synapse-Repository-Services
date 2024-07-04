package org.sagebionetworks.util.json.translator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ByteArrayTranslator implements Translator<byte[], String> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return byte[].class.equals(fieldType);
	}

	@Override
	public byte[] translateFromJSONToFieldValue(Class type, String jsonValue) {
		return Base64.getDecoder().decode(jsonValue.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String translateFieldValueToJSON(Class type, byte[] fieldValue) {
		return new String(Base64.getEncoder().encode(fieldValue), StandardCharsets.UTF_8);
	}

	@Override
	public Class<? extends byte[]> getFieldClass() {
		return byte[].class;
	}

	@Override
	public Class<? extends String> getJSONClass() {
		return String.class;
	}

}
