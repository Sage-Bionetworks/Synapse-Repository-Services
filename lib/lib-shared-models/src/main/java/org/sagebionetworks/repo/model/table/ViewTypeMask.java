package org.sagebionetworks.repo.model.table;

import org.sagebionetworks.repo.model.EntityType;

public enum ViewTypeMask {

	File(0x01, EntityType.file),
	Project(0x02, EntityType.project),
	Table(0x04, EntityType.table),
	Folder(0x08, EntityType.folder),
	View(0x10, EntityType.entityview),
	Docker(0x20, EntityType.dockerrepo),
	SubmissionView(0x40, EntityType.submissionview);

	long bitMask;
	EntityType entityType;

	ViewTypeMask(int mask, EntityType entityType) {
		this.bitMask = mask;
		this.entityType = entityType;
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

	public EntityType getEntityType() {
		return this.entityType;
	}

	/**
	 * Determine what the ViewType mask should be given either the old ViewType or a
	 * view type mask.
	 * 
	 * @param viewType
	 * @param viewTypeMask
	 * @return
	 * @throws IllegalArgumentException
	 *             If both 'viewType' and 'viewTypeMask' are set. One or the other
	 *             should be set.
	 */
	public static long getViewTypeMask(ViewType viewType, Long viewTypeMask) {
		if(viewType != null && viewTypeMask != null) {
			// Per PLFM-5126 ignore the type and use the mask for this case.
			viewType = null;
		}
		if(viewType != null) {
			return getMaskForDepricatedType(viewType);
		}else {
			if(viewTypeMask == null) {
				throw new IllegalArgumentException("Either 'viewType' or 'viewTypeMask' must be provided");
			}
			return viewTypeMask;
		}
	}

	/**
	 * Determine what the ViewType mask should be given either the old ViewType or a
	 * view type mask.
	 * See {@link #getViewTypeMask(ViewType, Long)}
	 * @param scope
	 * @return
	 */
	public static long getViewTypeMask(ViewScope scope) {
		if(scope == null) {
			throw new IllegalArgumentException("ViewScope cannot be null");
		}
		return getViewTypeMask(scope.getViewType(), scope.getViewTypeMask());
	}

}
