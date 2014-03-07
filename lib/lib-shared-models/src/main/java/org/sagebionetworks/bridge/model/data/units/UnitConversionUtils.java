package org.sagebionetworks.bridge.model.data.units;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataLabValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.repo.model.PaginatedResults;

public class UnitConversionUtils {

	public static ParticipantDataCurrentRow maybeNormalizeCurrentRow(boolean normalizeData, ParticipantDataCurrentRow row) {
		if (normalizeData) {
			normalizeRow(row.getPreviousData());
			normalizeRow(row.getCurrentData());
		}
		return row;
	}

	public static ParticipantDataRow maybeNormalizeRow(boolean normalizeData, ParticipantDataRow row) {
		if (normalizeData) {
			normalizeRow(row);
		}
		return row;
	}
	
	public static PaginatedResults<ParticipantDataRow> maybeNormalizePagedData(boolean normalizeData,
			PaginatedResults<ParticipantDataRow> results) {
		maybeNormalizeDataSet(normalizeData, results.getResults());	
		return results;
	}
	
	public static List<ParticipantDataRow> maybeNormalizeDataSet(boolean normalizeData, List<ParticipantDataRow> data) {
		if (normalizeData) {
			for (ParticipantDataRow row : data) {
				normalizeRow(row);
			}
		}
		return data;
	}

	private static void normalizeRow(ParticipantDataRow row) {
		for (Map.Entry<String, ParticipantDataValue> entry : row.getData().entrySet()) {
			
			ParticipantDataValue pdv = null;
			if (entry.getValue() instanceof ParticipantDataLabValue) {
				pdv = convertToNormalized((ParticipantDataLabValue)entry.getValue());
			} else {
				pdv = convertToNormalized(entry.getValue());
			}

			// Labs may not convert if they have no units, so they must be removed from the dataset.
			// However the UI should prevent a lab from being created with no units, so this does not happen.
			if (pdv != null) {
				row.getData().put(entry.getKey(), pdv);	
			} else {
				row.getData().remove(entry.getKey());
			}
		}
	}
	
	/**
	 * Currently only labs contain units and can be normalized. We can overload this method
	 * with other types if any convert. 
	 */
	public static ParticipantDataValue convertToNormalized(ParticipantDataValue value) {
		return value;
	}
	
	public static ParticipantDataLabValue convertToNormalized(ParticipantDataLabValue lab) {
		if (lab == null) {
			throw new IllegalArgumentException("Lab cannot be null");
		}
		// Must have a unit and a value for the lab to make sense. 
		Units unit = Units.unitFromString(lab.getUnits());
		if (unit == null || lab.getValue() == null) {
			return null;
		}
		ParticipantDataLabValue convertedLab = new ParticipantDataLabValue();
		
		convertedLab.setUnits(unit.getNormalizedUnit().getLabels().get(0));
		
		Measure normalizedValue = unit.convertToNormalized( lab.getValue() );
		convertedLab.setValue(normalizedValue.getAmount());
		
		if (lab.getMinNormal() != null) {
			Measure normalizedMin = unit.convertToNormalized( lab.getMinNormal() );
			convertedLab.setMinNormal(normalizedMin.getAmount());
		}
		if (lab.getMaxNormal() != null) {
			Measure normalizedMax = unit.convertToNormalized( lab.getMaxNormal() );
			convertedLab.setMaxNormal(normalizedMax.getAmount());
		}
		return convertedLab;
	}

}
