package org.sagebionetworks;

public interface PropertyAccessor {
	String getValueAsString();

	long getValueAsLong();

	int getValueAsInteger();

	boolean getValueAsBoolean();

	double getValueAsDouble();
}
