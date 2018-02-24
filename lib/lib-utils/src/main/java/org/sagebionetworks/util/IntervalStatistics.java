package org.sagebionetworks.util;

/**
 * The statistics gathered for a particular metric over a fixed interval of time
 * includes:
 * <ul>
 * <li>Minimum Value - The minimum value observed over the interval.</li>
 * <li>Maximum Value - The maximum value observed over the interval.</li>
 * <li>Value sum - The sum of all values observed over the interval.</li>
 * <li>Value count - The total number of values observed over the interval.</li>
 * </ul>
 * Other statistics such as average (valueSum/valueCount) are derived from these core statistics.
 */
public class IntervalStatistics {

	private double minimumValue;
	private double maximumValue;
	private double valueSum;
	private long valueCount;


	public IntervalStatistics(){
		minimumValue = Double.MAX_VALUE; //insures that min/max will be overridden the moment a value is added
		maximumValue = Double.MIN_VALUE;
		valueSum = 0;
		valueCount = 0L;
	}

	/**
	 * Create a new IntervalStatistics with a starting value.
	 * @param startValue
	 */
	public IntervalStatistics(double startValue){
		minimumValue = startValue;
		maximumValue = startValue;
		valueSum = startValue;
		valueCount = 1L;
	}
	
	/**
	 * Add a new value to this interval.
	 * @param newValue
	 */
	public void addValue(double newValue){
		minimumValue  = Math.min(minimumValue, newValue);
		maximumValue = Math.max(maximumValue, newValue);
		valueSum += newValue;
		valueCount++;
	}

	/**
	 * The minimum value observed over the interval.
	 * @return
	 */
	public double getMinimumValue() {
		return minimumValue;
	}

	/**
	 * The maximum value observed over the interval.
	 * @return
	 */
	public double getMaximumValue() {
		return maximumValue;
	}

	/**
	 * The sum of all values observed over the interval.
	 * @return
	 */
	public double getValueSum() {
		return valueSum;
	}

	/**
	 * The total number of values observed over the interval.
	 * @return
	 */
	public long getValueCount() {
		return valueCount;
	}
	
}
