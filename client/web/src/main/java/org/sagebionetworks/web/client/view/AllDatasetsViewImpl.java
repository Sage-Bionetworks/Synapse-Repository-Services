/**
 * 
 */
package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

//import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;

import org.sagebionetworks.web.client.DatasetConstants;
import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.shared.LayerLink;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import com.google.inject.Inject;

/**
 * @author jmhill
 * 
 */
public class AllDatasetsViewImpl extends Composite implements AllDatasetsView {

	static Logger logger = Logger.getLogger(AllDatasetsViewImpl.class.getName());

	public interface Binder extends UiBinder<Widget, AllDatasetsViewImpl> {}
	// To pass the cell table the CSS, we need to call a constructor
	// so we are providing the instance of this class
	@UiField (provided=true)
	CellTable<DatasetRow> cellTable;
	@UiField
	SimplePager pager = new SimplePager();
	private DatasetConstants constants;
	private Presenter presenter;
	private SageImageBundle bundle;
	private ImagePrototypeSingleton prototype;
	private List<SortableHeader> sortableHeaders = new ArrayList<SortableHeader>();

	/**
	 */
	@Inject
	public AllDatasetsViewImpl(final Binder uiBinder,
			DatasetConstants constants, final SageImageBundle bundle,
			final DatasetCellTableResource cellTableResource, ImagePrototypeSingleton prototype) {
		// 
		cellTable = new CellTable<DatasetRow>(10, cellTableResource);
		initWidget(uiBinder.createAndBindUi(this));
		this.constants = constants;
		this.bundle = bundle;
		this.prototype = prototype;
		
		// The name column is made up of links
		Column<DatasetRow, String> nameColumn = new Column<DatasetRow, String>(new ClickableTextCell( new TrustedHtmlRenderer())) {
			@Override
			public String getValue(DatasetRow row) {
				// Since the renderer will not escape html, we must do it here
				String safeName =SafeHtmlUtils.htmlEscape(row.getName());
				String safeLink = SafeHtmlUtils.htmlEscape(row.getLinkString());
				Hyperlink link = new Hyperlink(safeName, safeLink);
				return link.toString();
			}
		};
		
		
		Column<DatasetRow, List<ImageResource>> allImages = new Column<DatasetRow, List<ImageResource>>(
				new ImageResourceListCell()) {
			@Override
			public List<ImageResource> getValue(DatasetRow object) {
				int mask = object.getLayersMask();
				List<ImageResource> results = new LinkedList<ImageResource>();
				// Map to e
				if ((mask & LayerLink.Type.E.getMask()) > 0) {
					results.add(bundle.iconGeneExpression16());
				}else{
					results.add(bundle.iconTransparent16());
				}
				// Map to G
				if ((mask & LayerLink.Type.G.getMask()) > 0) {
					results.add(bundle.iconGenotype16());
				}else{
					results.add(bundle.iconTransparent16());
				}
				// Map to C
				if ((mask & LayerLink.Type.C.getMask()) > 0) {
					results.add(bundle.iconPhenotypes16());
				}else{
					results.add(bundle.iconTransparent16());
				}
				return results;
			}
		};

		TextColumn<DatasetRow> statusColumn = new TextColumn<DatasetRow>() {
			public String getValue(DatasetRow row) {
				return row.getStatus();
			}
		};

		TextColumn<DatasetRow> creatorColumn = new TextColumn<DatasetRow>() {
			public String getValue(DatasetRow row) {
				return row.getCreator();
			}
		};

		Column<DatasetRow, Date> createdColumn = new Column<DatasetRow, Date>(
				new DateCell()) {
			@Override
			public Date getValue(DatasetRow row) {
				return row.getCreatedOn();
			}
		};

		Column<DatasetRow, Date> modifiedColumn = new Column<DatasetRow, Date>(
				new DateCell()) {
			@Override
			public Date getValue(DatasetRow row) {
				return row.getModifiedColumn();
			}
		};
		
		cellTable.addColumn(nameColumn, this.constants.name());
		
		cellTable.addColumn(allImages, this.constants.layers());
		cellTable.addColumn(statusColumn, createHeader(this.constants.status(), "status"));
		cellTable.addColumn(creatorColumn, createHeader(this.constants.investigator(), "creator"));
		cellTable.addColumn(createdColumn, createHeader(this.constants.datePosted(), "creationDate"));
		cellTable.addColumn(modifiedColumn, createHeader(this.constants.dateModified(), "releaseDate"));

		// Set the cellList as the display.
		pager.setDisplay(cellTable);

		// Add the pager and list to the page.

		// The pager will trigger these
		cellTable.addRangeChangeHandler(new Handler() {
			@Override
			public void onRangeChange(RangeChangeEvent event) {
				Range newRange = event.getNewRange();
				presenter.pageTo(newRange.getStart(), newRange.getLength());
			}
		});
	}
	
	private SortableHeader createHeader(String display, String key){
		final SortableHeader header = new SortableHeader(display, prototype, key);
		sortableHeaders.add(header);
		header.setUpdater(new ValueUpdater<String>() {
			@Override
			public void update(String value) {
				presenter.toggleSort(header.getColumnKey());
			}
		});
		return header;
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		Window.alert(message);
	}

	@Override
	public void setDatasetRows(List<DatasetRow> rows, int offset, int limit,
			int totalCount, String sortKey, boolean ascending) {
		// update the table
		cellTable.setRowCount(totalCount, true);
		// Push the data into the widget.
		cellTable.setRowData(offset, rows);
		cellTable.setPageSize(limit);
		cellTable.setPageStart(offset);
		// Update the sorting
		updateSortColumns(sortKey, ascending);
	}
	
	private void updateSortColumns(String sortKey, boolean ascending){
		// Set the sorting state
		for(SortableHeader header: sortableHeaders){
			// If the sort key is null then turn off sorting
			if(sortKey != null && sortKey.equals(header.getColumnKey())){
				header.setSorting(true);
				header.setSortAscending(ascending);
			}else{
				header.setSorting(false);
			}
		}
		// Need to re-draw the headers
		cellTable.redrawHeaders();
	}

}
