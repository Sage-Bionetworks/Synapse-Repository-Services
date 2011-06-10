package org.sagebionetworks.web.client.widget.sharing;

import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AccessMenuButton implements AccessMenuButtonView.Presenter {

	public static enum AccessLevel { PUBLIC, SHARED, PRIVATE }
	
	private AccessMenuButtonView view;
	private NodeType nodeType;
	private String nodeId;
	private AccessControlListEditor accessControlListEditor;
	
	@Inject
	public AccessMenuButton(AccessMenuButtonView view, AccessControlListEditor accessControlListEditor) {
		this.view = view;
		this.accessControlListEditor = accessControlListEditor;
		view.setPresenter(this);
	}	
	
	public void setAccessLevel(AccessLevel level) {
		view.setAccessLevel(level);
	}

	public Widget asWidget() {
		return view.asWidget();
	}

	public void setResource(NodeType type, String id) {
		nodeType = type;
		nodeId = id;
		accessControlListEditor.setResource(type, id);
		view.setAccessControlListEditor(accessControlListEditor);
	}	
	
}
