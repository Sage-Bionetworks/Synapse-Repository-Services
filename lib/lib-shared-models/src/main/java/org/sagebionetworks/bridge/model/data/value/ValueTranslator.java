package org.sagebionetworks.bridge.model.data.value;

import java.util.Map;
import java.util.SortedSet;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;

public class ValueTranslator {
	public static final String LABRESULT_VALUE = "-value";
	public static final String LABRESULT_MIN_NORMAL_VALUE = "-minNormal";
	public static final String LABRESULT_MAX_NORMAL_VALUE = "-maxNormal";
	public static final String LABRESULT_UNITS = "-units";

	public static final String EVENT_NAME = "-name";
	public static final String EVENT_GROUPING = "-grouping";
	public static final String EVENT_START = "-start";
	public static final String EVENT_END = "-end";

	public static ParticipantDataValue transformToValue(Map<String, String> row, ParticipantDataColumnDescriptor descriptor) {
		String columnName = descriptor.getName();
		ParticipantDataColumnType columnType = descriptor.getColumnType();
		return transformToValue(row, columnName, columnType);
	}

	public static ParticipantDataValue transformToValue(Map<String, String> row, String columnName, ParticipantDataColumnType columnType) {
		if (columnType == null) {
			columnType = ParticipantDataColumnType.STRING;
		}
		switch (columnType) {
		case STRING:
			String svalue = row.get(columnName);
			if (isEmpty(svalue)) {
				return null;
			}
			ParticipantDataStringValue sresult = new ParticipantDataStringValue();
			sresult.setValue(svalue);
			return sresult;
		case BOOLEAN:
			String bvalue = row.get(columnName);
			if (isEmpty(bvalue)) {
				return null;
			}
			ParticipantDataBooleanValue bresult = new ParticipantDataBooleanValue();
			bresult.setValue(parseBoolean(bvalue));
			return bresult;
		case DATETIME:
			String dtvalue = row.get(columnName);
			if (isEmpty(dtvalue)) {
				return null;
			}
			ParticipantDataDatetimeValue dtresult = new ParticipantDataDatetimeValue();
			dtresult.setValue(parseDate(dtvalue));
			return dtresult;
		case DOUBLE:
			String dvalue = row.get(columnName);
			if (isEmpty(dvalue)) {
				return null;
			}
			ParticipantDataDoubleValue dresult = new ParticipantDataDoubleValue();
			dresult.setValue(parseDouble(dvalue));
			return dresult;
		case LONG:
			String lvalue = row.get(columnName);
			if (isEmpty(lvalue)) {
				return null;
			}
			ParticipantDataLongValue lresult = new ParticipantDataLongValue();
			lresult.setValue(parseLong(lvalue));
			return lresult;
		case LAB:
			String enteredValue = row.get(columnName + LABRESULT_VALUE);
			if (isEmpty(enteredValue)) {
				return null;
			}
			ParticipantDataLabValue labresult = new ParticipantDataLabValue();
			labresult.setValue(parseDouble(enteredValue));
			labresult.setUnits(parseString(row.get(columnName + LABRESULT_UNITS)));
			labresult.setMinNormal(parseDouble(row.get(columnName + LABRESULT_MIN_NORMAL_VALUE)));
			labresult.setMaxNormal(parseDouble(row.get(columnName + LABRESULT_MAX_NORMAL_VALUE)));
			return labresult;
		case EVENT:
			String nameValue = row.get(columnName + EVENT_NAME);
			if (isEmpty(nameValue)) {
				return null;
			}
			ParticipantDataEventValue eventresult = new ParticipantDataEventValue();
			eventresult.setName(nameValue);
			eventresult.setStart(parseDate(row.get(columnName + EVENT_START)));
			eventresult.setEnd(parseDate(row.get(columnName + EVENT_END)));
			eventresult.setGrouping(parseString(row.get(columnName + EVENT_GROUPING)));
			return eventresult;
		}
		throw new IllegalArgumentException("Column type " + columnType + " not handled");
	}

	public static void transformToStrings(ParticipantDataValue input, Map<String, String> row, ParticipantDataColumnDescriptor descriptor,
			SortedSet<String> columns) {
		if (input == null) {
			return;
		}

		ParticipantDataColumnType columnType = descriptor.getColumnType();
		if (columnType == null) {
			columnType = ParticipantDataColumnType.STRING;
		}
		String columnName = descriptor.getName();
		switch (columnType) {
		case STRING:
			ParticipantDataStringValue sresult = (ParticipantDataStringValue) input;
			row.put(columnName, sresult.getValue());
			columns.add(columnName);
			return;
		case BOOLEAN:
			ParticipantDataBooleanValue bresult = (ParticipantDataBooleanValue) input;
			if (bresult.getValue() != null) {
				row.put(columnName, bresult.getValue().toString());
				columns.add(columnName);
			}
			return;
		case DATETIME:
			ParticipantDataDatetimeValue dtresult = (ParticipantDataDatetimeValue) input;
			if (dtresult.getValue() != null) {
				row.put(columnName, dtresult.getValue().toString());
				columns.add(columnName);
			}
			return;
		case DOUBLE:
			ParticipantDataDoubleValue dresult = (ParticipantDataDoubleValue) input;
			if (dresult.getValue() != null) {
				row.put(columnName, dresult.getValue().toString());
				columns.add(columnName);
			}
			return;
		case LONG:
			ParticipantDataLongValue lresult = (ParticipantDataLongValue) input;
			if (lresult.getValue() != null) {
				row.put(columnName, lresult.getValue().toString());
				columns.add(columnName);
			}
			return;
		case LAB:
			ParticipantDataLabValue labresult = (ParticipantDataLabValue) input;
			if (labresult.getValue() != null) {
				row.put(columnName + LABRESULT_VALUE, labresult.getValue().toString());
				columns.add(columnName + LABRESULT_VALUE);
				if (labresult.getUnits() != null) {
					row.put(columnName + LABRESULT_UNITS, labresult.getUnits().toString());
					columns.add(columnName + LABRESULT_UNITS);
				}
				if (labresult.getMaxNormal() != null) {
					row.put(columnName + LABRESULT_MAX_NORMAL_VALUE, labresult.getMaxNormal().toString());
					columns.add(columnName + LABRESULT_MAX_NORMAL_VALUE);
				}
				if (labresult.getMinNormal() != null) {
					row.put(columnName + LABRESULT_MIN_NORMAL_VALUE, labresult.getMinNormal().toString());
					columns.add(columnName + LABRESULT_MIN_NORMAL_VALUE);
				}
			}
			return;
		case EVENT:
			ParticipantDataEventValue eventresult = (ParticipantDataEventValue) input;
			if (eventresult.getName() != null) {
				row.put(columnName + EVENT_NAME, eventresult.getName());
				columns.add(columnName + EVENT_NAME);
				if (eventresult.getStart() != null) {
					row.put(columnName + EVENT_START, eventresult.getStart().toString());
					columns.add(columnName + EVENT_START);
				}
				if (eventresult.getEnd() != null) {
					row.put(columnName + EVENT_END, eventresult.getEnd().toString());
					columns.add(columnName + EVENT_END);
				}
				if (eventresult.getGrouping() != null) {
					row.put(columnName + EVENT_GROUPING, eventresult.getGrouping());
					columns.add(columnName + EVENT_GROUPING);
				}
			}
			return;
		}
		throw new IllegalArgumentException("Column type " + columnType + " not handled");
	}

	@SuppressWarnings("rawtypes")
	public static Comparable getComparable(ParticipantDataValue input) {
		if (input == null) {
			return null;
		}

		if (input instanceof ParticipantDataStringValue) {
			return ((ParticipantDataStringValue) input).getValue();
		}
		if (input instanceof ParticipantDataBooleanValue) {
			return ((ParticipantDataBooleanValue) input).getValue();
		}
		if (input instanceof ParticipantDataDatetimeValue) {
			return ((ParticipantDataDatetimeValue) input).getValue();
		}
		if (input instanceof ParticipantDataDoubleValue) {
			return ((ParticipantDataDoubleValue) input).getValue();
		}
		if (input instanceof ParticipantDataLongValue) {
			return ((ParticipantDataLongValue) input).getValue();
		}
		if (input instanceof ParticipantDataLabValue) {
			return ((ParticipantDataLabValue) input).getValue();
		}
		if (input instanceof ParticipantDataEventValue) {
			return ((ParticipantDataEventValue) input).getStart();
		}
		throw new IllegalArgumentException("Data value type " + input.getClass().getName() + " not handled in getValue");
	}

	public static String toString(ParticipantDataValue input) {
		if (input == null) {
			return null;
		}

		if (input instanceof ParticipantDataStringValue) {
			return ((ParticipantDataStringValue) input).getValue();
		}
		if (input instanceof ParticipantDataBooleanValue) {
			return ((ParticipantDataBooleanValue) input).getValue().toString();
		}
		if (input instanceof ParticipantDataDatetimeValue) {
			return ((ParticipantDataDatetimeValue) input).getValue().toString();
		}
		if (input instanceof ParticipantDataDoubleValue) {
			return ((ParticipantDataDoubleValue) input).getValue().toString();
		}
		if (input instanceof ParticipantDataLongValue) {
			return ((ParticipantDataLongValue) input).getValue().toString();
		}
		if (input instanceof ParticipantDataLabValue) {
			return ((ParticipantDataLabValue) input).getValue() + " " + ((ParticipantDataLabValue) input).getUnits();
		}
		if (input instanceof ParticipantDataEventValue) {
			return ((ParticipantDataEventValue) input).getName();
		}
		throw new IllegalArgumentException("Data value type " + input.getClass().getName() + " not handled in toString");
	}

	public static Long toLong(ParticipantDataValue input) {
		if (input == null) {
			return null;
		}

		if (input instanceof ParticipantDataLongValue) {
			return ((ParticipantDataLongValue) input).getValue();
		}
		if (input instanceof ParticipantDataDatetimeValue) {
			return ((ParticipantDataDatetimeValue) input).getValue();
		}
		throw new IllegalArgumentException("Data value type " + input.getClass().getName() + " not handled in toDouble");
	}

	public static Double toDouble(ParticipantDataValue input) {
		if (input == null) {
			return null;
		}

		if (input instanceof ParticipantDataDoubleValue) {
			return ((ParticipantDataDoubleValue) input).getValue();
		}
		if (input instanceof ParticipantDataLongValue) {
			return ((ParticipantDataLongValue) input).getValue().doubleValue();
		}
		if (input instanceof ParticipantDataLabValue) {
			return ((ParticipantDataLabValue) input).getValue();
		}
		throw new IllegalArgumentException("Data value type " + input.getClass().getName() + " not handled in toDouble");
	}

	public static boolean canBeDouble(ParticipantDataColumnType columnType) {
		switch (columnType) {
		case DOUBLE:
		case LONG:
		case LAB:
			return true;
		default:
			return false;
		}
	}

	private static String parseString(String value) {
		if (isEmpty(value)) {
			return null;
		} else {
			return value;
		}
	}

	private static Double parseDouble(String value) {
		if (isEmpty(value)) {
			return null;
		} else {
			return Double.parseDouble(value);
		}
	}

	private static Long parseLong(String value) {
		if (isEmpty(value)) {
			return null;
		} else {
			return Long.parseLong(value);
		}
	}

	private static Boolean parseBoolean(String value) {
		if (isEmpty(value)) {
			return null;
		} else {
			return Boolean.parseBoolean(value);
		}
	}

	private static Long parseDate(String value) {
		if (isEmpty(value)) {
			return null;
		} else {
		// could be a long or an ISO date
			try {
				return parseLong(value);
			} catch (NumberFormatException e) {
				// not a long, try date
				return javax.xml.bind.DatatypeConverter.parseDateTime(value).getTimeInMillis();
			}
		}
	}

	private static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
}
