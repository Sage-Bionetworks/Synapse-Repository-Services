package org.sagebionetworks.util.json.translator;

public class LongTranslator implements Translator<Long, Long> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return Long.class.equals(fieldType);
	}

	@Override
	public Long translateFromJSONToFieldValue(Class type, Long jsonValue) {
		return jsonValue;
	}

	@Override
	public Long translateFieldValueToJSON(Class type, Long fieldValue) {
		return fieldValue;
	}

	@Override
	public Class<? extends Long> getFieldClass() {
		return Long.class;
	}

	@Override
	public Class<? extends Long> getJSONClass() {
		return Long.class;
	}

}
