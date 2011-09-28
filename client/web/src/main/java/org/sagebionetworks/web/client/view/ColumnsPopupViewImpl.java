package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ColumnsPopupViewImpl extends DialogBox implements ColumnsPopupView {


	public interface ColumnsPopupViewImplUiBinder extends	UiBinder<Widget, ColumnsPopupViewImpl> {}
	
	@UiField
	DialogBox dialogBox;
	@UiField
	Button cancelButton;
	@UiField
	Button applyButton;
	
	@UiField
	FlexTable defaultColumnList;
	@UiField
	FlexTable additionalColumnList;
	private Presenter presenter;
	

	@Inject
	public ColumnsPopupViewImpl(ColumnsPopupViewImplUiBinder binder) {
		setWidget(binder.createAndBindUi(this));
		
		// Bind the buttons to close
		cancelButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.cancel();
			}
		});
		applyButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				presenter.apply();
			}
		});
	}

	@Override
	public void show() {
		dialogBox.center();
		dialogBox.show();
	}

	@Override
	public void hide() {
		dialogBox.hide();
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setColumns(List<HeaderData> defaults,	List<HeaderData> additional) {
		defaultColumnList.removeAllRows();
		// Fill it up
		for(int i=0; i<defaults.size(); i++){
			HeaderData header = defaults.get(i);
			addChecBox(i, defaultColumnList, header);
		}
		// Clear the additional columns
		additionalColumnList.removeAllRows();
		for(int i=0; i<additional.size(); i++){
			HeaderData header = additional.get(i);
			addChecBox(i, additionalColumnList, header);
		}
	}


	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
	}

	
	/*
	 * Private Methods
	 */
	
	/**
	 * Adds a new checkbox to a table
	 * @param row
	 * @param table
	 * @param header
	 */
	private void addChecBox(int row, FlexTable table, HeaderData header){
		final CheckBox cb = new CheckBox(header.getDisplayName());
		final String id = header.getId();
		cb.setValue(presenter.isSelected(id));
		cb.addClickHandler(new  ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.setColumnSelected(id, cb.getValue());
				
			}
		});
		table.setWidget(row, 0, cb);	
	}

}
