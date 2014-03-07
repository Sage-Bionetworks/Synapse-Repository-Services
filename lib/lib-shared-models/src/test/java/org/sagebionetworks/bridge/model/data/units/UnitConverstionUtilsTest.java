package org.sagebionetworks.bridge.model.data.units;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataLabValue;

public class UnitConverstionUtilsTest {

	private ParticipantDataLabValue createLab(Double value, Units units, Double min, Double max) {
		ParticipantDataLabValue pdv = new ParticipantDataLabValue();
		pdv.setValue(value);
		if (units != null) {
			pdv.setUnits(units.getLabels().get(0));	
		}
		pdv.setMinNormal(min);
		pdv.setMaxNormal(max);
		return pdv;
	}
	
	@Test
	public void convertsLabCorrectly() {
		ParticipantDataLabValue pdv = createLab(1000d, Units.DECILITER, 500d, 1500d);
		
		// Converted to liters.
		ParticipantDataLabValue converted = UnitConversionUtils.convertToNormalized(pdv);
		assertEquals(100, converted.getValue(), 0.0);
		assertEquals("L", converted.getUnits());
		assertEquals(50, converted.getMinNormal(), 0.0);
		assertEquals(150, converted.getMaxNormal(), 0.0);
	}
	
	@Test
	public void incompleteLabReturnsNull() {
		ParticipantDataLabValue pdv = createLab(1000d, null, 500d, 1500d);
		
		ParticipantDataLabValue converted = UnitConversionUtils.convertToNormalized(pdv);
		assertNull(converted);
		
		pdv = createLab(null, Units.DECILITER, 500d, 1500d);
		assertNull(converted);
		
		pdv = createLab(null, null, 500d, 1500d);
		assertNull(converted);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullThrowsException() {
		ParticipantDataLabValue converted = UnitConversionUtils.convertToNormalized(null);
		assertNull(converted);
	}

}
