package org.sagebionetworks.bridge.model.data.value;

import java.util.Date;

public class ValueFactory {
	public static ParticipantDataStringValue createStringValue(String value) {
		if (value == null) {
			return null;
		}
		ParticipantDataStringValue sresult = new ParticipantDataStringValue();
		sresult.setValue(value);
		return sresult;
	}

	public static ParticipantDataBooleanValue createBooleanValue(Boolean value) {
		if (value == null) {
			return null;
		}
		ParticipantDataBooleanValue bresult = new ParticipantDataBooleanValue();
		bresult.setValue(value);
		return bresult;
	}

	public static ParticipantDataDatetimeValue createDatetimeValue(Date value) {
		return createDatetimeValue(value.getTime());
	}

	public static ParticipantDataDatetimeValue createDatetimeValue(Long value) {
		if (value == null) {
			return null;
		}
		ParticipantDataDatetimeValue dtresult = new ParticipantDataDatetimeValue();
		dtresult.setValue(value);
		return dtresult;
	}

	public static ParticipantDataDoubleValue createDoubleValue(Double value) {
		if (value == null) {
			return null;
		}
		ParticipantDataDoubleValue dresult = new ParticipantDataDoubleValue();
		dresult.setValue(value);
		return dresult;
	}

	public static ParticipantDataLongValue createLongValue(Long value) {
		if (value == null) {
			return null;
		}
		ParticipantDataLongValue lresult = new ParticipantDataLongValue();
		lresult.setValue(value);
		return lresult;
	}
}
