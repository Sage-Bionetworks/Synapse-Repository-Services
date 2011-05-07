package org.sagebionetworks.web.client.widget.login;

import org.sagebionetworks.web.client.IconsImageBundle;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginWidgetViewImpl extends LayoutContainer implements
		LoginWidgetView {

	private Presenter presenter;
	private VerticalPanel vp;
	private FormData formData;
	private Label messageLabel;
	private IconsImageBundle iconsImageBundle;

	@Inject
	public LoginWidgetViewImpl(IconsImageBundle iconsImageBundle) {
		this.iconsImageBundle = iconsImageBundle;
		messageLabel = new Label();
	}

	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);
		formData = new FormData("-20");
		vp = new VerticalPanel();
		vp.setSpacing(10);
		createForm1();
		add(vp);
	}

	private void createForm1() {
		FormPanel simple = new FormPanel();
		simple.setHeading("Login");
		simple.setFrame(true);
		simple.setWidth(350);
		simple.setLabelWidth(85);

		final TextField<String> firstName = new TextField<String>();
		firstName.setFieldLabel("Email Address");
		firstName.setAllowBlank(false);
		firstName.getFocusSupport()
				.setPreviousId(simple.getButtonBar().getId());
		simple.add(firstName, formData);

		final TextField<String> password = new TextField<String>();
		password.setFieldLabel("Password");
		password.setAllowBlank(false);
		password.setPassword(true);
		simple.add(password, formData);

		simple.add(messageLabel);
		
		Button b = new Button("Login");
		simple.addButton(b);
		b.addListener(Events.OnClick, new Listener<BaseEvent>() {
			@Override
			public void handleEvent(BaseEvent be) {
				messageLabel.setText(""); 
				presenter.setUsernameAndPassword(firstName.getValue(), password.getValue());
			}
		});

		simple.setButtonAlign(HorizontalAlignment.CENTER);
		
		FormButtonBinding binding = new FormButtonBinding(simple);
		binding.addButton(b);

		vp.add(simple);
	}

	@Override
	public Widget asWidget() {
		return this;
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showError(String message) {
		com.google.gwt.user.client.Window.alert(message);
	}

	@Override
	public void showAuthenticationFailed() {
		messageLabel.setStyleAttribute("color", "red");
		messageLabel.setText(AbstractImagePrototype.create(iconsImageBundle.warning16()).getHTML() + " Invalid username or password.");
	}

}
