package org.sagebionetworks.schema.element;

public abstract class Element {

	abstract public void toString(StringBuilder builder);
	
	final public String toString() {
		StringBuilder builder = new StringBuilder();
		toString(builder);
		return builder.toString();
	}
}
