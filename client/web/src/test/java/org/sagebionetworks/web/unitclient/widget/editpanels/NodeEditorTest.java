package org.sagebionetworks.web.unitclient.widget.editpanels;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditorDisplayHelper;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditorView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.shared.NodeType;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class NodeEditorTest {
	
	NodeEditor nodeEditor;
	NodeEditorView mockView;
	NodeServiceAsync mockNodeService;
	NodeModelCreator mockNodeModelCreator;
	NodeEditorDisplayHelper nodeEditorDisplayHelper;
	
	@Before
	public void setup(){
		mockView = mock(NodeEditorView.class);
		mockNodeService = mock(NodeServiceAsync.class);
		nodeEditorDisplayHelper = new NodeEditorDisplayHelper(); 
		mockNodeModelCreator = mock(NodeModelCreator.class);
		
		nodeEditor = new NodeEditor(mockView, mockNodeService, nodeEditorDisplayHelper, mockNodeModelCreator);		
		
		verify(mockView).setPresenter(nodeEditor);
	}	
	
	@Test
	public void testAsWidget() {
		reset(mockView);		
		nodeEditor.asWidget(NodeType.PROJECT);		
		verify(mockView).setPresenter(nodeEditor);
		
		reset(mockView);		
		nodeEditor.asWidget(NodeType.PROJECT, "editId");				
		verify(mockView).setPresenter(nodeEditor);
		
		reset(mockView);		
		nodeEditor.asWidget(NodeType.PROJECT, "editId", "parentId");				
		verify(mockView).setPresenter(nodeEditor);		
	}
	
}
