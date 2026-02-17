package org.sagebionetworks.repo.manager.grid.internal.replica.change;

public enum IntendedChangeType {

	update_row_metadata(0),
	insert_row(1),
	update_row(2),
	delete_row(3);

	private final int code;

	private IntendedChangeType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static IntendedChangeType fromCode(int code) {
		for (IntendedChangeType c : IntendedChangeType.values()) {
			if (c.code == code) {
				return c;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + code);
	}

}
