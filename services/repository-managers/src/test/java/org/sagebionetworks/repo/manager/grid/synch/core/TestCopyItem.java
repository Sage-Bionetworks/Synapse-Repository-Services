package org.sagebionetworks.repo.manager.grid.synch.core;

import java.util.Objects;

public class TestCopyItem implements CopyItem {

	private String id;
	private String value;
	private boolean wasChangedByUser;

	@Override
	public boolean wasChangedByUser() {
		return wasChangedByUser;
	}

	public String getValue() {
		return value;
	}

	public TestCopyItem setValue(String value) {
		this.value = value;
		return this;
	}

	public TestCopyItem setWasChangedByUser(boolean wasChangedByUser) {
		this.wasChangedByUser = wasChangedByUser;
		return this;
	}

	public String getId() {
		return id;
	}

	public TestCopyItem setId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, value, wasChangedByUser);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestCopyItem other = (TestCopyItem) obj;
		return Objects.equals(id, other.id) && Objects.equals(value, other.value)
				&& wasChangedByUser == other.wasChangedByUser;
	}

	@Override
	public String toString() {
		return "TestCopyItem [id=" + id + ", value=" + value + ", wasChangedByUser=" + wasChangedByUser + "]";
	}

}
