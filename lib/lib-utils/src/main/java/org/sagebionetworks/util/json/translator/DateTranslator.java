package org.sagebionetworks.util.json.translator;

import java.util.Date;

public class DateTranslator implements Translator<Date, Long> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return Date.class.equals(fieldType);
	}

	@Override
	public Date translateFromJSONToFieldValue(Class type, Long jsonValue) {
		return new Date(jsonValue);
	}

	@Override
	public Long translateFieldValueToJSON(Class type, Date fieldValue) {
		return fieldValue.getTime();
	}

	@Override
	public Class<? extends Date> getFieldClass() {
		return Date.class;
	}

	@Override
	public Class<? extends Long> getJSONClass() {
		return Long.class;
	}

}
