package org.sagebionetworks.util.json.translator;

/**
 * Translation from any Java enum to/from String.
 */
public class EnumTranslator implements Translator<Object, String> {

	@Override
	public boolean canTranslate(Class<?> fieldType) {
		return fieldType.isEnum();
	}

	@Override
	public Object translateFromJSONToJava(Class<? extends Object> type, String jsonValue) {
		for (Object e : type.getEnumConstants()) {
			if (e.toString().equals(jsonValue)) {
				return e;
			}
		}
		throw new IllegalArgumentException(
				String.format("The value: '%s' was not found in type: '%s'", jsonValue, type.getName()));
	}

	@Override
	public String translateFromJavaToJSON(Object fieldValue) {
		return fieldValue.toString();
	}

	@Override
	public Class<? extends String> getJSONClass() {
		return String.class;
	}

}
