package org.sagebionetworks.web.client.view;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.entity.EntityPageTop;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityViewImpl extends Composite implements EntityView {

	public interface EntityViewImplUiBinder extends UiBinder<Widget, EntityViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel entityPageTopPanel;
		
	@SuppressWarnings("unused")
	private Presenter presenter;
	private Header headerWidget;
	private EntityPageTop entityPageTop;
	
	@Inject
	public EntityViewImpl(
			EntityViewImplUiBinder binder,
			Header headerWidget,
			Footer footerWidget,
			EntityPageTop entityPageTop) {		
		initWidget(binder.createAndBindUi(this));

		this.headerWidget = headerWidget;
		this.entityPageTop = entityPageTop;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		// TODO : need to dynamically set the header widget
		//headerWidget.setMenuItemActive(MenuItems.PROJECTS);
	}


	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();
	}

	@Override
	public void setEntity(Entity entity) {
		entityPageTop.clearState();
		entityPageTop.setEntity(entity);
		entityPageTop.addEntityUpdatedHandler(new EntityUpdatedHandler() {			
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				presenter.refresh();
			}
		});
		entityPageTopPanel.clear();
		entityPageTopPanel.add(entityPageTop.asWidget());
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
