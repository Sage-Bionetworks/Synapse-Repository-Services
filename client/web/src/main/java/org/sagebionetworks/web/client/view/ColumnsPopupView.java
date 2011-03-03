package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.presenter.ColumnSelectionChangeListener;
import org.sagebionetworks.web.shared.HeaderData;

public interface ColumnsPopupView {
	
	public void setPresenter(Presenter presenter);
	
	/**
	 * Show the popup
	 */
	public void show();
	
	/**
	 * Hide the popup
	 */
	public void hide();
	
	/**
	 * Notify the view of the columns.
	 * @param selection
	 */
	public void setColumns(List<HeaderData> defaults, List<HeaderData> additional);
	
	public interface Presenter {
		
		/**
		 * 
		 * @param type
		 * @param selected
		 */
		public void showPopup(String type, List<String> selected, ColumnSelectionChangeListener listner);
		
		/**
		 * The user selected apply
		 */
		public void apply();
		
		/**
		 * The user selected cancel
		 */
		public void cancel();
		
		/**
		 * Should be called when the user toggles the column selection.
		 * @param name
		 * @param selected
		 */
		public void setColumnSelected(String name, boolean selected);
		
		/**
		 * Is a column selected.
		 * @param name
		 * @return
		 */
		public boolean isSelected(String name);
		
	}

	/**
	 * Show an error dialog
	 * @param message
	 */
	public void showError(String message);


}
