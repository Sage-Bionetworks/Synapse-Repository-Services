package org.sagebionetworks.javadoc;

import javax.lang.model.element.Name;

public class NameImpl implements Name {

	private final String name;

	public NameImpl(String name) {
		super();
		this.name = name;
	}

	@Override
	public int length() {
		return this.name.length();
	}

	@Override
	public char charAt(int index) {
		return this.name.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return this.name.subSequence(end, end);
	}

	@Override
	public boolean contentEquals(CharSequence cs) {
		return this.name.contentEquals(cs);
	}

}
