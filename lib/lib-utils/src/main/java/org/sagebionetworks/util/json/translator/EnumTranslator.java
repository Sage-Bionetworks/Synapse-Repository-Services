package org.sagebionetworks.util.json.translator;

public class EnumTranslator implements Translator<Object, String> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return fieldType.isEnum();
	}

	@Override
	public Object translateFromJSONToFieldValue(Class type, String jsonValue) {
		for (Object e : type.getEnumConstants()) {
			if (e.toString().equals(jsonValue)) {
				return e;
			}
		}
		throw new IllegalArgumentException(
				String.format("The value: '%ds' was not found in type: '%s'", jsonValue, type.getName()));
	}

	@Override
	public String translateFieldValueToJSON(Class type, Object fieldValue) {
		return fieldValue.toString();
	}

	@Override
	public Class<? extends Object> getFieldClass() {
		return Object.class;
	}

	@Override
	public Class<? extends String> getJSONClass() {
		return String.class;
	}

}
