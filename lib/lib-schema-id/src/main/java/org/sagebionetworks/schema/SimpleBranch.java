package org.sagebionetworks.schema;

/**
 * An abstract element the represents a simple branch between one or more elements
 *
 */
public class SimpleBranch extends Element {
	
	private Element child;
	
	public SimpleBranch(Element child) {
		super();
		if(child == null) {
			if(child == null) {
				throw new IllegalArgumentException("Child cannot be null");
			}
		}
		this.child = child;
	}

	@Override
	public void toString(StringBuilder builder) {
		child.toString(builder);
	}
	
}
