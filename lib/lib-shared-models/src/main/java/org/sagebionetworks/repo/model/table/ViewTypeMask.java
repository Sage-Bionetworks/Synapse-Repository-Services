package org.sagebionetworks.repo.model.table;

public enum ViewTypeMask {

	File(0x01), Project(0x02), Table(0x04), Folder(0x08), View(0x10), Docker(0x20);

	long bitMask;

	ViewTypeMask(int mask) {
		this.bitMask = mask;
	}

	/**
	 * Create the mask for the deprecated view type.
	 * 
	 * @param oldType
	 * @return
	 */
	public static long getMaskForDepricatedType(ViewType oldType) {
		switch (oldType) {
		case file:
			return File.bitMask;
		case project:
			return Project.bitMask;
		case file_and_table:
			return File.bitMask | Table.bitMask;
		default:
			throw new IllegalArgumentException("Unknown type: " + oldType);
		}
	}
	
	/**
	 * The bit mask for this type.
	 * 
	 * @return
	 */
	public long getMask() {
		return this.bitMask;
	}

}
