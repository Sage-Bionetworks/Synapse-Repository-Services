package org.sagebionetworks.web.client.widget.sharing;

import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.user.client.ui.IsWidget;

public interface AccessMenuButtonView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * Sets the access level string
	 * @param level
	 */
	public void createAccessMenu(AccessLevel level, AccessControlListEditor accessControlListEditor);

	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}

}
