package org.sagebionetworks.util.json.translator;

import java.util.Date;

/**
 * Translator between a Java Date and a Long.
 */
public class DateTranslator implements Translator<Date, Long> {

	@Override
	public boolean canTranslate(Class<?> fieldType) {
		return Date.class.equals(fieldType);
	}

	@Override
	public Date translateFromJSONToJava(Class<? extends Date> type, Long jsonValue) {
		return new Date(jsonValue);
	}

	@Override
	public Long translateFromJavaToJSON(Date fieldValue) {
		return fieldValue.getTime();
	}

	@Override
	public Class<? extends Long> getJSONClass() {
		return Long.class;
	}

}
