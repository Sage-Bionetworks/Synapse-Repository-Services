package org.sagebionetworks.util.json.translator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A translators between a byte[] and a base 64 String.
 */
public class ByteArrayTranslator implements Translator<byte[], String> {

	/**
	 * This can translate a byte[].
	 */
	@Override
	public boolean canTranslate(Class<?> fieldType) {
		return byte[].class.equals(fieldType);
	}

	@Override
	public byte[] translateFromJSONToJava(Class<? extends byte[]> type, String jsonValue) {
		return Base64.getDecoder().decode(jsonValue.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String translateFromJavaToJSON(byte[] fieldValue) {
		return new String(Base64.getEncoder().encode(fieldValue), StandardCharsets.UTF_8);
	}

	@Override
	public Class<? extends String> getJSONClass() {
		return String.class;
	}

}
