package org.sagebionetworks.util.json.translator;

import java.sql.Timestamp;

public class TimestampTranslator implements Translator<Timestamp, Long> {

	@Override
	public boolean canTranslate(Class<?> fieldType) {
		return Timestamp.class.equals(fieldType);
	}

	@Override
	public Timestamp translateFromJSONToJava(Class<? extends Timestamp> type, Long jsonValue) {
		return new Timestamp(jsonValue);
	}

	@Override
	public Long translateFromJavaToJSON(Timestamp fieldValue) {
		return fieldValue.getTime();
	}

	@Override
	public Class<? extends Long> getJSONClass() {
		return Long.class;
	}

}
