package org.sagebionetworks.bridge.model.data.units;

import static org.junit.Assert.*;

import org.junit.Test;

public class UnitsTest {

	@Test
	public void canRetrieveUnitFromString() {
		Units unit = Units.unitFromString("pL");
		assertEquals(Units.PICOLITER, unit);
		
		unit = Units.unitFromString("deciliter");
		assertEquals(Units.DECILITER, unit);
	}
	
	@Test
	public void valuesConvert() {
		Measure trillions = new Measure(3, Units.TRILLIONS_PER_LITER);
		
		Measure converted = trillions.convertToNormalized();
		assertEquals(3.0, converted.getAmount(), 0.0);
		assertEquals(Units.MILLIONS_PER_MICROLITER, converted.getUnit());
	}

	@Test
	public void badStringValuesReturnNull() {
		assertNull(Units.unitFromString("belgium"));
		assertNull(Measure.measureFromStrings(null, "g/L"));
		assertNull(Measure.measureFromStrings("1000", null));
		assertNull(Measure.measureFromStrings(null, null));
		assertNull(Measure.measureFromStrings("belgium", "pL"));
		assertNull(Measure.measureFromStrings("1000", "belgium"));
	}
	
	@Test
	public void canRetrieveMeasureFromStrings() {
		Measure measure = Measure.measureFromStrings("1000", "g/L");
		Measure target = new Measure(1000, Units.GRAMS_PER_LITER);
		
		assertEquals(1000, measure.getAmount(), 0.0);
		assertEquals(Units.GRAMS_PER_LITER, measure.getUnit());
		assertEquals(target, measure);
	}
	
	@Test
	public void enforcesMeasureConstructor() {
		try {
			new Measure(0, null);
			fail("Did not enforce unit declaration");
		} catch(IllegalArgumentException e) {
		}
	}
	
	@Test
	public void enforcesThatEqualAmountsWithDifferentUnitsAreNotEqual() {
		Measure m = new Measure(1000, Units.MILLILITER);
		
		Measure converted = Units.GRAM.convertFromNormalized(1000);
		assertFalse(m.equals(converted));
	}
	
	@Test
	public void convertToNormalizedFromMeasure() {
		Measure millis = new Measure(1000, Units.MILLILITER);
		Measure millisAsLiter = millis.convertToNormalized();

		Measure oneLiter = new Measure(1, Units.LITER);
		
		assertEquals(millis, millisAsLiter);
		assertEquals(millis, oneLiter);
		assertEquals(millis, Units.MILLILITER.convertFromNormalized(1));
		
		// Liters are the normalized value, this should still make sense.
		Measure liters = new Measure(3, Units.LITER);
		Measure normLiters = liters.convertToNormalized();
		
		assertEquals(liters, normLiters);
		assertEquals(liters, Units.LITER.convertFromNormalized(3));
	}
	
	@Test
	public void equalQuantitiesInDifferentUnitsAreEqual() {
		Measure deciliters = new Measure(7, Units.DECILITER);
		Measure liters = new Measure(.7, Units.LITER);
		
		// If you use doubles, BTW, this test fails, because the converted value has a rounding 
		// error and is 0.7000000000000001, not 0.7.
		assertEquals(deciliters, liters);
		
		Measure milligrams = new Measure(200, Units.MILLIGRAM);
		Measure grams = new Measure(.2, Units.GRAM);
		assertEquals(milligrams, grams);
		
		// And this will fail
		assertFalse(deciliters.equals(grams));
	}
	
	@Test
	public void convertToNormalizedFromUnit() {
		Measure gram = new Measure(1, Units.GRAM);
		Measure decigrams = Units.DECIGRAM.convertToNormalized(10);
		assertEquals(gram, decigrams);
		
		Measure mmol = new Measure(3.229, Units.MILLIMOLES_PER_LITER);
		Measure phosphorus = Units.PHOSPHORUS_MG_DL.convertToNormalized(10);
		assertEquals(mmol, phosphorus);
	}
	
	@Test
	public void conversionFromNormalizedUsingMoles() {
		// Phosphate	2.5-4.5	mg/dL	0.32	mmol/L
		// NOTE: This is messed up. Somehow the conversion factor is mixed up with the ratio.
		// Can mg/dL be varied and how would this work?
		Measure phosphorus = new Measure(1000, Units.PHOSPHORUS_MG_DL);
		Measure normPhosphorus = phosphorus.convertToNormalized();

		Measure mmolPhosphorus = new Measure(322.9, Units.MILLIMOLES_PER_LITER);
		
		assertEquals(normPhosphorus, mmolPhosphorus);
		assertEquals(phosphorus, Units.PHOSPHORUS_MG_DL.convertFromNormalized(322.9));
	}
	
	@Test
	public void roundTripConversionHasNoRoundingErrors() {
		Measure phosphorus = new Measure(1042, Units.PHOSPHORUS_MG_DL);
		Measure roundtripped = Units.PHOSPHORUS_MG_DL.convertFromNormalized(phosphorus.convertToNormalized().getAmount());
		assertEquals(phosphorus, roundtripped);
		// and it doesn't, so doubles seem to be fine for this purpose.
	}
}
