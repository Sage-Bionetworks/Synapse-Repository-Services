package org.sagebionetworks.web.client.widget.sharing;

import java.util.List;

import org.sagebionetworks.web.shared.users.AclEntry;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.PermissionLevel;

import com.google.gwt.user.client.ui.IsWidget;

public interface AccessControlListEditorView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	/**
	 * Sets the details needed to display the form
	 * @param entries
	 * @param principals
	 */
	public void setAclDetails(List<AclEntry> entries, List<AclPrincipal> principals, boolean isEditable);
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);

	public void refresh(List<AclEntry> entries, List<AclPrincipal> principals, boolean isEditable);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
		void createAcl();
		
		void addAccess(AclPrincipal principal, PermissionLevel permissionLevel);
		
		void changeAccess(AclEntry aclEntry, PermissionLevel permissionLevel);
		
		void removeAccess(AclEntry aclEntry);
	}
}
