package org.sagebionetworks.util.json.translator;

public class StringTranslator implements Translator<String, String> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return String.class.equals(fieldType);
	}

	@Override
	public String translateFromJSONToFieldValue(Class type, String jsonValue) {
		return jsonValue;
	}

	@Override
	public String translateFieldValueToJSON(Class type, String fieldValue) {
		return fieldValue;
	}

	@Override
	public Class<? extends String> getFieldClass() {
		return String.class;
	}

	@Override
	public Class<? extends String> getJSONClass() {
		return String.class;
	}

}
