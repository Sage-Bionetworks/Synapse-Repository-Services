package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.PhenoEdit;
import org.sagebionetworks.web.client.presenter.PhenoEditPresenter;
import org.sagebionetworks.web.client.view.PhenoEditView;

public class PhenoEditPresenterTest {

	PhenoEditPresenter phenoEditPresenter;
	PhenoEditView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	
	@Before
	public void setup(){
		mockView = mock(PhenoEditView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		phenoEditPresenter = new PhenoEditPresenter(mockView, mockGlobalApplicationState);
		
		verify(mockView).setPresenter(phenoEditPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		PhenoEdit place = Mockito.mock(PhenoEdit.class);
		phenoEditPresenter.setPlace(place);
	}	
}
