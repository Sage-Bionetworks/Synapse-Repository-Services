package org.sagebionetworks.docusign;

public record RoleLabelKey(String roleName, String tabLabel) {

	public RoleLabelKey {
		if (roleName == null) {
			throw new IllegalArgumentException("roleName is required.");
		}
		if (tabLabel == null) {
			throw new IllegalArgumentException("tabLabel is required.");
		}
	}
}
