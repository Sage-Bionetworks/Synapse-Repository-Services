package org.sagebionetworks.web.client.widget.sharing;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.shared.NodeType;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.IconAlign;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AccessMenuButtonViewImpl extends LayoutContainer implements AccessMenuButtonView {

	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;	
	private Button button;
	private AccessLevel accessLevel;
	private AccessControlListEditor accessControlListEditor;
	
	private static final String buttonPrefix = "Access: ";
	
	@Inject
	public AccessMenuButtonViewImpl(IconsImageBundle iconsImageBundle) {
		this.iconsImageBundle = iconsImageBundle;				
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);		
		add(button);
	}
	
	@Override
	public void createAccessMenu(AccessLevel level, AccessControlListEditor accessControlListEditor) {
		this.accessLevel = level;
		this.accessControlListEditor = accessControlListEditor;

		button = new Button();					
		button.setIconAlign(IconAlign.LEFT);			
		button.setMenu(createAccessMenu());
		button.setHeight(25);					
		
		String levelText = "";
		ImageResource icon = null;
		switch(level) {
		case PUBLIC:
			levelText = "Public";
			icon = iconsImageBundle.lockUnlocked16();
			break;
		case PRIVATE:
			levelText = "Private";
			icon = iconsImageBundle.lock16();
			break;
		case SHARED:
			levelText = "Shared";
			icon = iconsImageBundle.lock16();
			break;
		}
		button.setText(buttonPrefix + levelText);
		button.setIcon(AbstractImagePrototype.create(icon));		
	}	
	
	@Override
	public Widget asWidget() {
		return this;
	}
	

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	/*
	 * Private Methods
	 */
	private Menu createAccessMenu() {
		Menu menu = new Menu();		
		MenuItem item = null; 
			
		item = new MenuItem("Sharing Settings...");
		item.setIcon(AbstractImagePrototype.create(iconsImageBundle.users16()));
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent menuEvent) {													
				final Window window = new Window();  
				window.setSize(550, 440);
				window.setPlain(true);
				window.setModal(true);
				window.setBlinkModal(true);
				window.setHeading(DisplayConstants.TITLE_SHARING_PANEL);
				window.setLayout(new FitLayout());
				window.add(accessControlListEditor.asWidget(), new FitData(4));
				Button closeButton = new Button("Close");
				closeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						window.hide();
					}
				});
				window.setButtonAlign(HorizontalAlignment.RIGHT);
				window.addButton(closeButton);
				window.show();
			}
		});		
		menu.add(item);
				
		return menu;
	}

}
