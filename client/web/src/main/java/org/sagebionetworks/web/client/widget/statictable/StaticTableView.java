package org.sagebionetworks.web.client.widget.statictable;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.IsWidget;

public interface StaticTableView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	public void clear();

	public void setDataAndColumnsInOrder(List<Map<String, String>> rows, List<StaticTableColumn> columnsInOrder);
	
	public void setDataAndColumnOrder(List<Map<String,String>> rows, List<String> columnOrder);
	
	public void setData(List<Map<String,String>> rows);
	
	public void setColumnOrder(List<String> columnOrder);	
	
	public void setDimensions(int width, int height);

	public void setTitleText(String title);
	
	public void showTitleBar(boolean showTitleBar);	
	
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}


	

}
