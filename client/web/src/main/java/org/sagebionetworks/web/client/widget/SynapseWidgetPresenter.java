package org.sagebionetworks.web.client.widget;

import org.sagebionetworks.web.client.PlaceChanger;

import com.google.gwt.user.client.ui.Widget;

public interface SynapseWidgetPresenter {

	public Widget asWidget();
	
    public void setPlaceChanger(PlaceChanger placeChanger);

}
