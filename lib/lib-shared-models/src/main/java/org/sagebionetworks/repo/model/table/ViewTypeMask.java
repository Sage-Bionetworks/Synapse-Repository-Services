package org.sagebionetworks.repo.model.table;

public enum ViewTypeMask {

	File(0x1),
	Project(0x02),
	Table(0x04),
	Folder(0x08),
	View(0x10),
	Docker(0x20);
	
	int mask;
	
	ViewTypeMask(int mask){
		this.mask = mask;
	}
	
	public static int getMaskForDepricatedType(ViewType oldType) {
		switch(oldType) {
		case file:
			return File.mask;
		case project:
			return Project.mask;
		case file_and_table:
			return File.mask | Table.mask;
			default:
				throw new IllegalArgumentException("");
		}
	}
	
}
