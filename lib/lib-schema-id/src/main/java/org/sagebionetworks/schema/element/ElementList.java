package org.sagebionetworks.schema.element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class ElementList<T extends Element> extends Element {
	
	private final String delimiter;
	private final ArrayList<T> elements;
	
	public ElementList(String delimiter) {
		this.delimiter = delimiter;
		if(delimiter == null) {
			throw new IllegalArgumentException("Delimiter cannot be null");
		}
		this.elements = new ArrayList<T>();
	}
	
	public void add(T toAdd) {
		if(toAdd == null) {
			throw new IllegalArgumentException("Cannot add a null element");
		}
		this.elements.add(toAdd);
	}
	
	/**
	 * Iterator for the elements in this list.
	 * @return
	 */
	public Iterator<T> iterator(){
		return elements.iterator();
	}

	@Override
	public void toString(StringBuilder builder) {
		for(int i=0; i<elements.size(); i++) {
			if(i > 0) {
				builder.append(delimiter);
			}
			elements.get(i).toString(builder);
		}
	}

	@Override
	public final int hashCode() {
		return Objects.hash(delimiter, elements);
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ElementList)) {
			return false;
		}
		ElementList other = (ElementList) obj;
		return Objects.equals(delimiter, other.delimiter) && Objects.equals(elements, other.elements);
	}

}
