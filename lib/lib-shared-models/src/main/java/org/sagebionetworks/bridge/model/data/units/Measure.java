package org.sagebionetworks.bridge.model.data.units;

import java.math.BigDecimal;

public final class Measure {
	
	private final BigDecimal amount;
	private final Units unit;

	public static Measure measureFromStrings(String amount, String unit) {
		if (amount == null || unit == null) {
			return null;
		}
		Units units = Units.unitFromString(unit);
		if (units == null) {
			return null;
		}
		try {
			double value = Double.parseDouble(amount);
			return new Measure(value, units);
		} catch(NumberFormatException e) {
			return null;
		}
	};
	
	public Measure(BigDecimal amount, Units unit) {
		if (unit == null) {
			throw new IllegalArgumentException("Measure must be constructed with units");
		}
		this.amount = amount;
		this.unit = unit;
	}
	
	public Measure(double amount, Units unit) {
		if (unit == null) {
			throw new IllegalArgumentException("Measure must be constructed with units");
		}
		this.amount = BigDecimal.valueOf(amount);
		this.unit = unit;
	}
	
	public double getAmount() {
		return amount.doubleValue();
	}
	
	public Units getUnit() {
		return unit;
	}
	
	public Measure convertToNormalized() {
		if (unit.getNormalizedUnit() == null) {
			return this;
		}
		return new Measure(amount.multiply(unit.getBigDecimalFactor()), unit.getNormalizedUnit());
	}

	@Override
	public String toString() {
		return "Measure [amount=" + amount + ", unit=" + unit.getLabels().get(0) + "]";
	}

	@Override
	public int hashCode() {
		double converted = convertToNormalized().getAmount();
		
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(converted);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Measure other = (Measure) obj;
		Measure thisOne = this.convertToNormalized();
		Measure converted = other.convertToNormalized();
		
		if (thisOne.amount.compareTo(converted.amount) != 0)
			return false;
		if (thisOne.unit != converted.unit)
			return false;
		return true;
	}
}
