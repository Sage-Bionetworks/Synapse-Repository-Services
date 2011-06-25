package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.widget.login.LoginWidget;
import org.sagebionetworks.web.client.widget.login.UserListener;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginViewImpl extends Composite implements LoginView {
	
	@UiField
	SimplePanel loginWidgetPanel;
	@UiField
	SimplePanel passwordResetButtonPanel;
	@UiField 
	SimplePanel registerButtonPanel;
	@UiField
	SimplePanel logoutPanel;

	private Presenter presenter;
	private LoginWidget loginWidget;
	private IconsImageBundle iconsImageBundle;
	private SageImageBundle sageImageBundle;
	private Window logginInWindow;

	public interface Binder extends UiBinder<Widget, LoginViewImpl> {}
	
	@Inject
	public LoginViewImpl(Binder uiBinder, IconsImageBundle icons, SageImageBundle sageImageBundle, LoginWidget loginWidget){
		initWidget(uiBinder.createAndBindUi(this));
		this.loginWidget = loginWidget;
		this.iconsImageBundle = icons;
		this.sageImageBundle = sageImageBundle;
	}

	@Override
	public void setPresenter(Presenter loginPresenter) {
		this.presenter = loginPresenter;
	}

	@Override
	public void clear() {
		loginWidget.clear();
	}
	
	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}

	@Override
	public void showLoggingInLoader() {
		if(logginInWindow == null) {
			logginInWindow = new Window();
			logginInWindow.setModal(true);		
			logginInWindow.setHeight(114);
			logginInWindow.setWidth(221);		
			logginInWindow.setBorders(false);
			logginInWindow.add(new Html(DisplayUtils.getIconHtml(sageImageBundle.loading31()) + " " + DisplayConstants.LABEL_SINGLE_SIGN_ON_LOGGING_IN), new MarginData(20, 0, 0, 45));		
			logginInWindow.setBodyStyleName("whiteBackground");
		}
		logginInWindow.show();
	}

	@Override
	public void hideLoggingInLoader() {
		logginInWindow.hide();
	}

	@Override
	public void showLogout(boolean isSsoLogout) {
		clearAllPanels();
		
		ContentPanel cp = new ContentPanel();
		cp.setWidth(385);
		cp.setHeaderVisible(false);
		cp.setBorders(true);						
		cp.setBodyStyleName("lightGreyBackground");
		cp.add(new HTML("<h2>"+ DisplayConstants.LABEL_LOGOUT_TEXT +"</h2>"), new MarginData(0, 0, 0, 10));
		
		HTML message = new HTML();
		if(isSsoLogout) {
			message.setHTML(DisplayUtils.getIconHtml(iconsImageBundle
					.informationBalloon16())
					+ " "
					+ DisplayConstants.LOGOUT_TEXT
					+ "<br/><br/>"
					+ DisplayUtils.getIconHtml(iconsImageBundle.warning16())
					+ " " + DisplayConstants.LOGOUT_SSO_TEXT);
		} else {
			message.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.informationBalloon16()) + " " + DisplayConstants.LOGOUT_TEXT);
		}
		cp.add(message, new MarginData(0, 0, 0, 10));
		
		Button loginAgain = new Button(DisplayConstants.BUTTON_LOGIN_AGAIN);
		loginAgain.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new LoginPlace("0"));
			}
		});
		cp.add(loginAgain, new MarginData(16, 0, 10, 10));
		
		logoutPanel.add(cp);
	}

	@Override
	public void showLogin(String openIdActionUrl, String openIdReturnUrl) {
		clearAllPanels();
		
		loginWidget.setOpenIdActionUrl(openIdActionUrl);
		loginWidget.setOpenIdReturnUrl(openIdReturnUrl);
		
		// Add the widget to the panel
		loginWidgetPanel.clear();
		loginWidgetPanel.add(loginWidget.asWidget());
		loginWidget.addUserListener(new UserListener() {
			
			@Override
			public void userChanged(UserData newUser) {
				presenter.setNewUser(newUser);
			}
		});
				
		Button forgotPasswordButton = new Button("Forgot Password?", AbstractImagePrototype.create(iconsImageBundle.help16()), new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new PasswordReset("0"));								
			}
		});
		passwordResetButtonPanel.clear();
		passwordResetButtonPanel.add(forgotPasswordButton);
		

		Button registerButton = new Button("Register for a Synapse Account", AbstractImagePrototype.create(iconsImageBundle.userBusiness16()), new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				presenter.goTo(new RegisterAccount("0"));
			}
		});
		registerButton.disable();
		registerButtonPanel.add(registerButton);
	}

	
	/*
	 * Private Methods
	 */
	private void clearAllPanels() {
		if(logginInWindow != null) logginInWindow.hide();
		loginWidgetPanel.clear();
		passwordResetButtonPanel.clear();
		registerButtonPanel.clear();
		logoutPanel.clear();
	}

}
