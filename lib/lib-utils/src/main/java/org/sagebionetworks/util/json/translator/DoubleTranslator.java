package org.sagebionetworks.util.json.translator;

public class DoubleTranslator implements Translator<Double, Double> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return Double.class.equals(fieldType);
	}

	@Override
	public Double translateFromJSONToFieldValue(Class type, Double jsonValue) {
		return jsonValue;
	}

	@Override
	public Double translateFieldValueToJSON(Class type, Double fieldValue) {
		return fieldValue;
	}

	@Override
	public Class<? extends Double> getFieldClass() {
		return Double.class;
	}

	@Override
	public Class<? extends Double> getJSONClass() {
		return Double.class;
	}

}
