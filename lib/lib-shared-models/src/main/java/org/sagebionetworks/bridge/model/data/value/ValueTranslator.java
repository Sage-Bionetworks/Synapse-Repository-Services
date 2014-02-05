package org.sagebionetworks.bridge.model.data.value;

import java.util.Map;
import java.util.SortedSet;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;

public class ValueTranslator {
	private static final String LABRESULT_NORMALIZED_VALUE = "-normalizedValue";
	private static final String LABRESULT_NORMALIZED_MIN = "-normalizedMin";
	private static final String LABRESULT_NORMALIZED_MAX = "-normalizedMax";
	private static final String LABRESULT_UNITS = "-units";
	private static final String LABRESULT_ENTERED = "-entered";

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
			dtresult.setValue(parseLong(dtvalue));
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
			String enteredValue = row.get(columnName + LABRESULT_ENTERED);
			if (isEmpty(enteredValue)) {
				return null;
			}
			ParticipantDataLabValue labresult = new ParticipantDataLabValue();
			labresult.setEnteredValue(enteredValue);
			labresult.setUnits(parseString(row.get(columnName + LABRESULT_UNITS)));
			labresult.setNormalizedMax(parseDouble(row.get(columnName + LABRESULT_NORMALIZED_MAX)));
			labresult.setNormalizedMin(parseDouble(row.get(columnName + LABRESULT_NORMALIZED_MIN)));
			labresult.setNormalizedValue(parseDouble(row.get(columnName + LABRESULT_NORMALIZED_VALUE)));
			return labresult;
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
			break;
		case BOOLEAN:
			ParticipantDataBooleanValue bresult = (ParticipantDataBooleanValue) input;
			if (bresult.getValue() != null) {
				row.put(columnName, bresult.getValue().toString());
				columns.add(columnName);
			}
			break;
		case DATETIME:
			ParticipantDataDatetimeValue dtresult = (ParticipantDataDatetimeValue) input;
			if (dtresult.getValue() != null) {
				row.put(columnName, dtresult.getValue().toString());
				columns.add(columnName);
			}
			break;
		case DOUBLE:
			ParticipantDataDoubleValue dresult = (ParticipantDataDoubleValue) input;
			if (dresult.getValue() != null) {
				row.put(columnName, dresult.getValue().toString());
				columns.add(columnName);
			}
			break;
		case LONG:
			ParticipantDataLongValue lresult = (ParticipantDataLongValue) input;
			if (lresult.getValue() != null) {
				row.put(columnName, lresult.getValue().toString());
				columns.add(columnName);
			}
			break;
		case LAB:
			ParticipantDataLabValue labresult = (ParticipantDataLabValue) input;
			if (labresult.getEnteredValue() != null) {
				row.put(columnName + LABRESULT_ENTERED, labresult.getEnteredValue().toString());
				columns.add(columnName + LABRESULT_ENTERED);
				if (labresult.getUnits() != null) {
					row.put(columnName + LABRESULT_UNITS, labresult.getUnits().toString());
					columns.add(columnName + LABRESULT_UNITS);
				}
				if (labresult.getNormalizedMax() != null) {
					row.put(columnName + LABRESULT_NORMALIZED_MAX, labresult.getNormalizedMax().toString());
					columns.add(columnName + LABRESULT_NORMALIZED_MAX);
				}
				if (labresult.getNormalizedMin() != null) {
					row.put(columnName + LABRESULT_NORMALIZED_MIN, labresult.getNormalizedMin().toString());
					columns.add(columnName + LABRESULT_NORMALIZED_MIN);
				}
				if (labresult.getNormalizedValue() != null) {
					row.put(columnName + LABRESULT_NORMALIZED_VALUE, labresult.getNormalizedValue().toString());
					columns.add(columnName + LABRESULT_NORMALIZED_VALUE);
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Column type " + columnType + " not handled");
		}
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
			return ((ParticipantDataLabValue) input).getEnteredValue() + " " + ((ParticipantDataLabValue) input).getUnits();
		}
		throw new IllegalArgumentException("Data value type " + input.getClass().getName() + " not handled in toString");
	}

	public static Double toDouble(ParticipantDataValue input) {
		if (input instanceof ParticipantDataDoubleValue) {
			return ((ParticipantDataDoubleValue) input).getValue();
		}
		if (input instanceof ParticipantDataLongValue) {
			return ((ParticipantDataLongValue) input).getValue().doubleValue();
		}
		if (input instanceof ParticipantDataLabValue) {
			return ((ParticipantDataLabValue) input).getNormalizedValue();
		}
		throw new IllegalArgumentException("Data value type " + input.getClass().getName() + " not handled in toDouble");
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

	private static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
}
