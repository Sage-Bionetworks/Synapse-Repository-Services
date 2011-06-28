package org.sagebionetworks.web.client.presenter;

import java.util.List;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.cookie.CookieUtils;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.view.DatasetsHomeView;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class DatasetsHomePresenter extends AbstractActivity implements DatasetsHomeView.Presenter {
	
	public static final String KEY_DATASETS_SELECTED_COLUMNS_COOKIE = "org.sagebionetworks.selected.dataset.columns";

	private DatasetsHome place;
	private DatasetsHomeView view;
	private ColumnsPopupPresenter columnsPopupPresenter;
	private CookieProvider cookieProvider;
	private PlaceController placeController; 
	private PlaceChanger placeChanger;
	
	@Inject
	public DatasetsHomePresenter(DatasetsHomeView view, ColumnsPopupPresenter columnsPopupPresenter, CookieProvider cookieProvider){
		this.view = view;
		this.columnsPopupPresenter = columnsPopupPresenter;
		// Set the presenter on the view
		this.cookieProvider = cookieProvider;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		this.placeController = DisplayUtils.placeController;		
		this.placeChanger = new PlaceChanger() {			
			@Override
			public void goTo(Place place) {
				placeController.goTo(place);
			}
		};
		this.view.setPresenter(this);

		// Setup the columns
		setVisibleColumns();
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(DatasetsHome place) {
		this.place = place;
	}

	@Override
	public void onEditColumns() {
		// Start with a null list
		List<String> currentSelection = null;
		String cookieValue = this.cookieProvider.getCookie(KEY_DATASETS_SELECTED_COLUMNS_COOKIE);
		if(cookieValue != null){
			currentSelection = CookieUtils.createListFromString(cookieValue);
		}

		// Show the view.
		columnsPopupPresenter.showPopup(ObjectType.dataset.name(), currentSelection, new ColumnSelectionChangeListener() {
			
			@Override
			public void columnSelectionChanged(List<String> newSelection) {
				// Save the value back in a cookie
				String newValue = CookieUtils.createStringFromList(newSelection);
				cookieProvider.setCookie(KEY_DATASETS_SELECTED_COLUMNS_COOKIE, newValue);
				setVisibleColumns();
			}
		});
	}
	
	private void setVisibleColumns(){
		List<String> currentSelection = null;
		String cookieValue = this.cookieProvider.getCookie(KEY_DATASETS_SELECTED_COLUMNS_COOKIE);
		if(cookieValue != null){
			currentSelection = CookieUtils.createListFromString(cookieValue);
			view.setVisibleColumns(currentSelection);
		}
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

}
