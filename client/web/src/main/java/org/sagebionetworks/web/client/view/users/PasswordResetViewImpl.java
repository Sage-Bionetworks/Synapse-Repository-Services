package org.sagebionetworks.web.client.view.users;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.MessageBox.MessageBoxType;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PasswordResetViewImpl extends Composite implements PasswordResetView {

	public interface PasswordResetViewImplUiBinder extends UiBinder<Widget, PasswordResetViewImpl> {}
	
	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel contentPanel;	
	@UiField 
	SpanElement contentHtml;

	private Presenter presenter;
	private FormPanel requestFormPanel;
	private FormPanel resetFormPanel;
	private FormData formData;  
	private IconsImageBundle iconsImageBundle;
	
	@Inject
	public PasswordResetViewImpl(PasswordResetViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle iconsImageBundle, QueryFilter filter, SageImageBundle imageBundle) {		
		initWidget(binder.createAndBindUi(this));

		this.iconsImageBundle = iconsImageBundle;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		
		formData = new FormData("-20");  
		createRequestForm();
		createResetForm();
		
		contentPanel.add(requestFormPanel);
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}


	 private void createRequestForm() {  
	     requestFormPanel = new FormPanel();  
	     requestFormPanel.setFrame(true);
	     requestFormPanel.setHeaderVisible(false);  
	     requestFormPanel.setWidth(350);  
	     requestFormPanel.setLayout(new FlowLayout());  
	   
	     FieldSet fieldSet = new FieldSet();  
	     fieldSet.setHeading("Send Password Change Request&nbsp;");  
	     fieldSet.setCheckboxToggle(false);  
	   
	     FormLayout layout = new FormLayout();  
	     layout.setLabelWidth(100);  
	     fieldSet.setLayout(layout);  
	   
	     final TextField<String> emailAddress = new TextField<String>();  
	     emailAddress.setFieldLabel("Email Address");  
	     emailAddress.setAllowBlank(false);  
	     fieldSet.add(emailAddress, formData);  
	   
	     requestFormPanel.add(fieldSet);  
	   
	     requestFormPanel.setButtonAlign(HorizontalAlignment.CENTER);  
	     Button sendChangeRequest = new Button("Send", new SelectionListener<ButtonEvent>() {				
				@Override
				public void componentSelected(ButtonEvent ce) {
					presenter.requestPasswordReset(emailAddress.getValue());
				}
		 });
	     requestFormPanel.addButton(sendChangeRequest);

	     FormButtonBinding binding = new FormButtonBinding(requestFormPanel);
		 binding.addButton(sendChangeRequest);

	   }  

	 private void createResetForm() {  
//	     resetFormPanel = new FormPanel();  
//	     resetFormPanel.setFrame(true);
//	     resetFormPanel.setHeaderVisible(false);  
//	     resetFormPanel.setWidth(350);  
//	     resetFormPanel.setLayout(new FlowLayout());  
//	   
//	     FieldSet fieldSet = new FieldSet();  
//	     fieldSet.setHeading("Enter a New Password&nbsp;");  
//	     fieldSet.setCheckboxToggle(false);  
//	   
//	     FormLayout layout = new FormLayout();  
//	     layout.setLabelWidth(100);  
//	     fieldSet.setLayout(layout);  
//	   
//	     final TextField<String> newPassword = new TextField<String>();  
//	     newPassword.setFieldLabel("New Password");  
//	     newPassword.setAllowBlank(false);
//	     newPassword.setPassword(true);
//	     fieldSet.add(newPassword, formData);  
//	   
//	     final TextField<String> newPasswordConfirm = new TextField<String>();  
//	     newPasswordConfirm.setFieldLabel("Confirm Password");  
//	     newPasswordConfirm.setAllowBlank(false);
//	     newPasswordConfirm.setPassword(true);
//	     fieldSet.add(newPasswordConfirm, formData);  
//	   
//	     resetFormPanel.add(fieldSet);  
//	   
//	     resetFormPanel.setButtonAlign(HorizontalAlignment.CENTER);  
//	     Button sendReset = new Button("Submit");
//	     sendReset.addSelectionListener(new SelectionListener<ButtonEvent>() {				
//			@Override
//			public void componentSelected(ButtonEvent ce) {
//				if(newPassword.getValue() != null && newPasswordConfirm.getValue() != null && newPassword.getValue().equals(newPasswordConfirm.getValue())) {				
//					presenter.resetPassword(newPassword.getValue());
//				} else {
//					MessageBox.alert("Error", "Passwords do not match. Please re-enter your new password.", null);
//				}
//				
//			}
//	     });
//	     resetFormPanel.addButton(sendReset);  		       		  
	   }


	@Override
	public void showRequestForm() {
		contentHtml.setInnerHTML("");		
		contentPanel.clear();
		contentPanel.add(requestFormPanel);
	}


	@Override
	public void showResetForm() {
		contentHtml.setInnerHTML("");
		contentPanel.clear();
		contentPanel.add(resetFormPanel);
	}


	@Override
	public void showPasswordResetSuccess() {
		contentPanel.clear();		
		contentHtml.setInnerHTML(AbstractImagePrototype.create(iconsImageBundle.informationBalloon16()).getHTML() + " Your password has been changed.");
	}


	@Override
	public void showErrorMessage(String errorMessage) {
		MessageBox.info("Error", errorMessage, null);
	}


	@Override
	public void showRequestSentSuccess() {
		contentPanel.clear();		
		contentHtml.setInnerHTML(AbstractImagePrototype.create(iconsImageBundle.informationBalloon16()).getHTML() + " Your password reset request has been sent. Please check your Email.");
	}


	@Override
	public void showInfo(String infoMessage) {
		Info.display("Message", infoMessage);
	}


	@Override
	public void showMessage(String message) {
		contentHtml.setInnerHTML(message);
	}  
}
