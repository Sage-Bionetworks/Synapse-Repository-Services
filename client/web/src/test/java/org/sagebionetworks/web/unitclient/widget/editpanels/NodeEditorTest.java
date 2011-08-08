package org.sagebionetworks.web.unitclient.widget.editpanels;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.editpanels.FormField;
import org.sagebionetworks.web.client.widget.editpanels.FormField.ColumnType;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditorDisplayHelper;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditorView;
import org.sagebionetworks.web.shared.NodeType;

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

	// TODO : remove JSONObjects from NodeEditor so this test can be run
	@Ignore
	@Test
	public void testPersist() {
		List<FormField> formFields = new ArrayList<FormField>();
		String key1 = "key1";
		String key2 = "key2";
		String value1 = "value1";
		String value2 = null;
		formFields.add(new FormField(key1, value1, ColumnType.STRING));
		formFields.add(new FormField(key2, value2, ColumnType.STRING));

		nodeEditor.asWidget(NodeType.DATASET);
		
		reset(mockNodeService);
		nodeEditor.persist(formFields);		
	}
	
}
