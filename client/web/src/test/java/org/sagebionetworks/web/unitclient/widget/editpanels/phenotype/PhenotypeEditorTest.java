package org.sagebionetworks.web.unitclient.widget.editpanels.phenotype;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.client.ontology.StaticOntologies;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.ColumnDefinitionEditor;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.ColumnDefinitionEditorView;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.ColumnMappingEditor;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.ColumnMappingEditorView;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.PhenotypeEditor;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.PhenotypeEditorView;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.PhenotypeMatrix;
import org.sagebionetworks.web.client.widget.editpanels.phenotype.PhenotypeMatrixView;

public class PhenotypeEditorTest {
	
	PhenotypeEditor phenotypeEditor;
	PhenotypeEditorView mockView;
	NodeServiceAsync mockNodeService;	
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;
	
	@Before
	public void setup(){
		StaticOntologies staticOntologies = new StaticOntologies();
		
		ColumnDefinitionEditor columnDefinitionEditor = new ColumnDefinitionEditor(mock(ColumnDefinitionEditorView.class), staticOntologies);
		ColumnMappingEditor columnMappingEditor = new ColumnMappingEditor(mock(ColumnMappingEditorView.class));
		PhenotypeMatrix phenotypeMatrix = new PhenotypeMatrix(mock(PhenotypeMatrixView.class));
		
		mockView = mock(PhenotypeEditorView.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		phenotypeEditor = new PhenotypeEditor(mockView, mockNodeService,
				mockNodeModelCreator, mockAuthenticationController,
				staticOntologies, columnDefinitionEditor, columnMappingEditor,
				phenotypeMatrix);		
		
		verify(mockView).setPresenter(phenotypeEditor);
	}	
	
	@Test
	public void testAsWidget() {
		resetMocks();
		phenotypeEditor.asWidget();
		
		verify(mockView).setPresenter(phenotypeEditor);
		verify(mockView).asWidget();
	}
	
	/*
	 * Private methods
	 */
	private void resetMocks() {
		reset(mockView);
		reset(mockNodeService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
	}	

}

