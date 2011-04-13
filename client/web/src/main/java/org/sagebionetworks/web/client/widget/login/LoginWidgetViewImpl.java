package org.sagebionetworks.web.client.widget.login;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginWidgetViewImpl extends LayoutContainer implements
		LoginWidgetView {

	private Presenter presenter;
	private VerticalPanel vp;
	private FormData formData;

	@Inject
	public LoginWidgetViewImpl() {

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
		simple.setHeading("Login:");
		simple.setFrame(true);
		simple.setWidth(350);

		final TextField<String> firstName = new TextField<String>();
		firstName.setFieldLabel("Name");
		firstName.setAllowBlank(false);
		firstName.getFocusSupport()
				.setPreviousId(simple.getButtonBar().getId());
		simple.add(firstName, formData);

		final TextField<String> password = new TextField<String>();
		password.setFieldLabel("Password");
		password.setAllowBlank(false);
		password.setPassword(true);
		simple.add(password, formData);

		Button b = new Button("Login");
		simple.addButton(b);
		b.addListener(Events.OnClick, new Listener<BaseEvent>() {
			@Override
			public void handleEvent(BaseEvent be) {
				presenter.setUsernameAndPassword(firstName.getValue(), password.getValue());
			}
		});
		simple.addButton(new Button("Cancel"));

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

}
