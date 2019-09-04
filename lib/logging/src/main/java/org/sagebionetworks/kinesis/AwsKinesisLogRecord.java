package org.sagebionetworks.kinesis;

public interface AwsKinesisLogRecord {

	/**
	 * @return The stack to which this record belongs
	 */
	public String getStack();

	/**
	 * Set the stack to which this record belongs
	 * @param stack The stack to which this record belongs
	 * @return reference to this object so that value setting operations can be chained
	 */
	public AwsKinesisLogRecord withStack(String stack);

	/**
	 * @return The instance to which this record belongs
	 */
	public String getInstance();

	/**
	 * Set the instance to which this record belongs
	 * @param instance The instance to which this record belongs
	 * @return reference to this object so that value setting operations can be chained
	 */
	public AwsKinesisLogRecord withInstance(String instance);
}
