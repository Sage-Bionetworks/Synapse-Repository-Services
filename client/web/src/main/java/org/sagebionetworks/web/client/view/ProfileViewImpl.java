package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ProfileViewImpl extends Composite implements ProfileView {

	public interface ProfileViewImplUiBinder extends UiBinder<Widget, ProfileViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel updateUserInfoPanel;
	@UiField
	SimplePanel changePasswordPanel;
	@UiField
	SimplePanel setupPasswordButtonPanel;
	@UiField
	SimplePanel updateWithLinkedInPanel;
	
	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private NodeEditor nodeEditor;
	private Header headerWidget;
	private FormPanel resetFormPanel;
	private Button createPasswordButton;
	private SageImageBundle sageImageBundle;
	private Button changePasswordButton;
	private Html changePasswordLabel;
	private FormPanel userFormPanel;
	private Button updateWithLinkedInButton;
	private Button updateUserInfoButton;
	private Html updateUserInfoLabel;
	private FormPanel connectFormPanel;
	private Button linkedInButton;
	private Html linkedInLabel;
	private Button linkedInConnectButton;
	private Button viewProfileButton;

	@Inject
	public ProfileViewImpl(ProfileViewImplUiBinder binder,
			Header headerWidget, Footer footerWidget, IconsImageBundle icons,
			SageImageBundle imageBundle, final NodeEditor nodeEditor, SageImageBundle sageImageBundle) {		
		initWidget(binder.createAndBindUi(this));

		this.iconsImageBundle = icons;
		this.nodeEditor = nodeEditor;
		this.headerWidget = headerWidget;
		this.sageImageBundle = sageImageBundle;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.PROJECTS);
	}


	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;		
		headerWidget.refresh();				
	}
	
	@Override
	public void render() {
		createResetForm();
		createProfileForm();
		
		linkedInButton = new Button();
	    linkedInButton.setIcon(AbstractImagePrototype.create(sageImageBundle.linkedinsmall()));
	    linkedInButton.setSize(sageImageBundle.linkedinsmall().getWidth() + 1, sageImageBundle.linkedinsmall().getHeight() + 1);
	    linkedInButton.setBorders(false);
	    linkedInButton.addSelectionListener(new SelectionListener<ButtonEvent>() {				
	    	@Override
	    	public void componentSelected(ButtonEvent ce) {
	    		presenter.redirectToLinkedIn();
	    	}
	    });
		
	    updateWithLinkedInPanel.clear();
	    updateWithLinkedInPanel.add(linkedInButton);
	    
		updateUserInfoLabel.setHtml("");
		
		updateUserInfoPanel.clear();
		updateUserInfoPanel.add(userFormPanel);
		setUpdateUserInfoDefaultIcon();
		
		changePasswordLabel.setHtml("");
		
		changePasswordPanel.clear();
		changePasswordPanel.add(resetFormPanel);
		setChangePasswordDefaultIcon();
		
		createPasswordButton = new Button(DisplayConstants.BUTTON_SETUP_API_PASSWORD, AbstractImagePrototype.create(iconsImageBundle.addSquare16()), new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				createPasswordButton.setIcon(AbstractImagePrototype.create(sageImageBundle.loading16()));
				createPasswordButton.disable();
				presenter.createSynapsePassword();
			}
		});
		createPasswordButton.enable();
		
		setupPasswordButtonPanel.clear();
		setupPasswordButtonPanel.add(createPasswordButton);
	}

	@Override
	public void showPasswordChangeSuccess() {
		changePasswordButton.setText(DisplayConstants.BUTTON_CHANGE_PASSWORD);
		changePasswordButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.checkGreen16()));
		changePasswordLabel.setHtml(DisplayUtils.getIconHtml(iconsImageBundle.informationBalloon16()) + " Password Changed");
	}

	@Override
	public void passwordChangeFailed() {
		changePasswordButton.setText(DisplayConstants.BUTTON_CHANGE_PASSWORD);
		changePasswordButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.error16()));		
	}

	@Override
	public void showRequestPasswordEmailSent() {
		createPasswordButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.checkGreen16()));
		createPasswordButton.setText("Email Sent");		
	}
	
	@Override
	public void requestPasswordEmailFailed() {
		createPasswordButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.error16()));
		createPasswordButton.setText("Email Send Failed");
	}

	@Override
	public void showUserUpdateSuccess() {
		updateUserInfoButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.checkGreen16()));
		updateUserInfoButton.setText("Profile Updated");
	}

	@Override
	public void userUpdateFailed() {
		updateUserInfoButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.error16()));
		updateUserInfoButton.setText("Profile Update Failed");
	}

	/*
	 * Private Methods
	 */	
	 private void createResetForm() {  		 
		 resetFormPanel = new FormPanel();
		 FormData formData = new FormData("-20");
		   
	     resetFormPanel.setFrame(true);
	     resetFormPanel.setHeaderVisible(false);  
	     resetFormPanel.setWidth(350);  
	     resetFormPanel.setLayout(new FlowLayout());  
	   
	     FieldSet fieldSet = new FieldSet();  
	     fieldSet.setHeading(" ");  
	     fieldSet.setCheckboxToggle(false);	    
	     fieldSet.setCollapsible(false);
	   
	     FormLayout layout = new FormLayout();  
	     layout.setLabelWidth(100);  
	     fieldSet.setLayout(layout);  
	   
	     final TextField<String> currentPassword = new TextField<String>();  
	     currentPassword.setFieldLabel("Current Password");  
	     currentPassword.setAllowBlank(false);
	     currentPassword.setPassword(true);
	     fieldSet.add(currentPassword, formData);  
	   
	     final TextField<String> newPassword = new TextField<String>();  
	     newPassword.setFieldLabel("New Password");  
	     newPassword.setAllowBlank(false);
	     newPassword.setPassword(true);
	     fieldSet.add(newPassword, formData);  
	   
	     final TextField<String> newPasswordConfirm = new TextField<String>();  
	     newPasswordConfirm.setFieldLabel("Confirm Password");  
	     newPasswordConfirm.setAllowBlank(false);
	     newPasswordConfirm.setPassword(true);
	     fieldSet.add(newPasswordConfirm, formData);  

	     changePasswordLabel = new Html();
	     fieldSet.add(changePasswordLabel);
	     
	     resetFormPanel.add(fieldSet);  
	   
	     resetFormPanel.setButtonAlign(HorizontalAlignment.CENTER);  
	     changePasswordButton = new Button(DisplayConstants.BUTTON_CHANGE_PASSWORD);
	     changePasswordButton.addSelectionListener(new SelectionListener<ButtonEvent>() {				
	    	 @Override
	    	 public void componentSelected(ButtonEvent ce) {
	    		 if(newPassword.getValue() != null && newPasswordConfirm.getValue() != null && newPassword.getValue().equals(newPasswordConfirm.getValue())) {
	    			 changePasswordLabel.setHtml("");
	    			 DisplayUtils.changeButtonToSaving(changePasswordButton, sageImageBundle);
	    			 presenter.resetPassword(currentPassword.getValue(), newPassword.getValue());
	    		 } else {
	    			 MessageBox.alert("Error", "Passwords do not match. Please re-enter your new password.", null);
	    		 }
		
	    	 }
	     });	     
	     setChangePasswordDefaultIcon();
	     resetFormPanel.addButton(changePasswordButton);

	     // Enter key submits
	     new KeyNav<ComponentEvent>(resetFormPanel) {
	    	 @Override
	    	 public void onEnter(ComponentEvent ce) {
	    		 super.onEnter(ce);
	    		 if (changePasswordButton.isEnabled())
	    			 changePasswordButton.fireEvent(Events.Select);
	    	 }
	     };

	     // form binding so submit button is greyed out until all fields are filled 
	     final FormButtonBinding binding = new FormButtonBinding(resetFormPanel);
	     binding.addButton(changePasswordButton);
	 }
	 
	 private void createProfileForm() {
		 userFormPanel = new FormPanel();
		 FormData formData = new FormData("-20");
		   
	     userFormPanel.setFrame(true);
	     userFormPanel.setHeaderVisible(false);  
	     userFormPanel.setWidth(350);  
	     userFormPanel.setLayout(new FlowLayout());
	     
	     FieldSet fieldSet = new FieldSet();  
	     fieldSet.setHeading(" ");  
	     fieldSet.setCheckboxToggle(false);	    
	     fieldSet.setCollapsible(false);
	   
	     FormLayout layout = new FormLayout();  
	     layout.setLabelWidth(100);  
	     fieldSet.setLayout(layout);  
	   
	     final TextField<String> firstName = new TextField<String>();  
	     firstName.setFieldLabel("First Name");  
	     firstName.setAllowBlank(false);
	     fieldSet.add(firstName, formData);
	   
	     final TextField<String> lastName = new TextField<String>();  
	     lastName.setFieldLabel("Last Name");  
	     lastName.setAllowBlank(false);
	     fieldSet.add(lastName, formData);

	     updateUserInfoLabel = new Html();
	     fieldSet.add(updateUserInfoLabel);
	     
	     userFormPanel.add(fieldSet);  
	   
	     userFormPanel.setButtonAlign(HorizontalAlignment.CENTER);  
	     updateUserInfoButton = new Button(DisplayConstants.BUTTON_CHANGE_USER_INFO);
	     updateUserInfoButton.addSelectionListener(new SelectionListener<ButtonEvent>() {				
	    	 @Override
	    	 public void componentSelected(ButtonEvent ce) {
	    		 if(firstName.getValue().trim().equals("") || firstName.getValue().trim() == null) {
	    			 MessageBox.alert("Error", "Please enter your first name.", null);
	    		 } else if(lastName.getValue().trim().equals("") || lastName.getValue() == null) {
	    			 MessageBox.alert("Error", "Please enter your last name.", null);
	    		 } else {
	    			 updateUserInfoLabel.setHtml("");
	    			 DisplayUtils.changeButtonToSaving(updateUserInfoButton, sageImageBundle);
	    			 presenter.updateProfile(firstName.getValue(), lastName.getValue());
	    		 }
		
	    	 }
	     });	     
	     setUpdateUserInfoDefaultIcon();
	     userFormPanel.addButton(updateUserInfoButton);

	     // Enter key submits
	     new KeyNav<ComponentEvent>(userFormPanel) {
	    	 @Override
	    	 public void onEnter(ComponentEvent ce) {
	    		 super.onEnter(ce);
	    		 if (updateUserInfoButton.isEnabled())
	    			 updateUserInfoButton.fireEvent(Events.Select);
	    	 }
	     };

	     // form binding so submit button is greyed out until all fields are filled 
	     final FormButtonBinding binding = new FormButtonBinding(userFormPanel);
	     binding.addButton(updateUserInfoButton);

	     userFormPanel.addButton(viewProfileButton);

	 }
	 
	 private void setChangePasswordDefaultIcon() {
		 changePasswordButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.arrowCurve16()));			
	 }
	 
	 private void setUpdateUserInfoDefaultIcon() {
		 updateUserInfoButton.setIcon(AbstractImagePrototype.create(iconsImageBundle.arrowCurve16()));
	 }

	@Override
	public void refreshHeader() {
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
		updateUserInfoPanel.clear();
		changePasswordPanel.clear();
		setupPasswordButtonPanel.clear();
		updateWithLinkedInPanel.clear();
	}

}
