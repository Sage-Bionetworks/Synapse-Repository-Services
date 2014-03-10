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
		if (value == null) {
			return null;
		}
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

	public static ParticipantDataEventValue createEventValue(Date start, Date end, String name, String grouping) {
		return createEventValue(start.getTime(), end == null ? null : end.getTime(), name, grouping);
	}

	public static ParticipantDataEventValue createEventValue(Long start, Long end, String name, String grouping) {
		ParticipantDataEventValue eresult = new ParticipantDataEventValue();
		eresult.setStart(start);
		eresult.setEnd(end);
		eresult.setName(name);
		eresult.setGrouping(grouping);
		return eresult;
	}
	
	public static ParticipantDataLabValue createLabValue(double value, String unit, double minNormal, double maxNormal) {
		ParticipantDataLabValue lresult = new ParticipantDataLabValue();
		lresult.setValue(value);
		lresult.setUnits(unit);
		lresult.setMinNormal(minNormal);
		lresult.setMaxNormal(maxNormal);
		return lresult;
	}
}
