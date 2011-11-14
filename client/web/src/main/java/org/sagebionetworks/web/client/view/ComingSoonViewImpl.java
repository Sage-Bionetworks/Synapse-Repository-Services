package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.entity.EntityPageTop;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ComingSoonViewImpl extends Composite implements ComingSoonView {

	public interface ComingSoonViewImplUiBinder extends UiBinder<Widget, ComingSoonViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel entityView;
		
	private Presenter presenter;
	private IconsImageBundle icons;
	private Header headerWidget;

	@Inject
	public ComingSoonViewImpl(ComingSoonViewImplUiBinder binder,
			Header headerWidget, Footer footerWidget, IconsImageBundle icons,
			SageImageBundle imageBundle,
			QueryServiceTableResourceProvider queryServiceTableResourceProvider,
			final NodeEditor nodeEditor,
			EntityPageTop entityPageTop) {		
		initWidget(binder.createAndBindUi(this));

		this.icons = icons;
		this.headerWidget = headerWidget;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		
//		entityPageTop.setEntity(new Dataset());
//		entityView.add(entityPageTop.asWidget());
		
	}


	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();								
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

}
