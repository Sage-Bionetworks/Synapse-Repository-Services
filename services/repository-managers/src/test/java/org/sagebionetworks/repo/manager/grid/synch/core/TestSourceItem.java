package org.sagebionetworks.repo.manager.grid.synch.core;

import java.util.Objects;

public class TestSourceItem implements SourceItem {

	private String value;
	private String key;

	public TestSourceItem setKey(String key) {
		this.key = key;
		return this;
	}

	@Override
	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public TestSourceItem setValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestSourceItem other = (TestSourceItem) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "TestSourceItem [value=" + value + ", key=" + key + "]";
	}

}
