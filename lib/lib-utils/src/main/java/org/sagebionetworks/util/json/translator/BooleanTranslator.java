package org.sagebionetworks.util.json.translator;

public class BooleanTranslator implements Translator<Boolean, Boolean> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return Boolean.class.equals(fieldType);
	}

	@Override
	public Boolean translateFromJSONToFieldValue(Class type, Boolean jsonValue) {
		return jsonValue;
	}

	@Override
	public Boolean translateFieldValueToJSON(Class type, Boolean fieldValue) {
		return fieldValue;
	}

	@Override
	public Class<? extends Boolean> getFieldClass() {
		return Boolean.class;
	}

	@Override
	public Class<? extends Boolean> getJSONClass() {
		return Boolean.class;
	}

}
