package org.sagebionetworks.repo.manager.principal;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.AccountCreationToken;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.SerializationUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * Basic implementation of the PrincipalManager.
 * @author John
 *
 */
public class PrincipalManagerImpl implements PrincipalManager {
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private SynapseEmailService sesClient;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private TokenGenerator tokenGenerator;

	@Override
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		return this.principalAliasDAO.isAliasAvailable(alias);
	}

	@Override
	public boolean isAliasValid(String alias, AliasType type) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		// Check the value
		try {
			AliasEnum.valueOf(type.name()).validateAlias(alias);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	// will throw exception for invalid email, invalid endpoint, or an email which is already taken
	@Override
	public void newAccountEmailValidation(NewUser user, String portalEndpoint, Date now) {
		AliasEnum.USER_EMAIL.validateAlias(user.getEmail());
		// is the email taken?
		if (!principalAliasDAO.isAliasAvailable(user.getEmail())) {
			throw new NameConflictException("The email address provided is already used.");
		}
		AccountCreationToken token = PrincipalUtils.createAccountCreationToken(user, now, tokenGenerator);
		String encodedToken = SerializationUtils.serializeAndHexEncode(token);
		String url = portalEndpoint+encodedToken;
		EmailUtils.validateSynapsePortalHost(url);
		String subject = "Welcome to Synapse!";
		Map<String,String> fieldValues = new HashMap<>();
		fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, "Synapse");
		fieldValues.put(EmailUtils.TEMPLATE_KEY_WEB_LINK, url);
		String messageBody = EmailUtils.readMailTemplate("message/CreateAccountTemplate.html", fieldValues);
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(user.getEmail())
				.withSubject(subject)
				.withBody(messageBody, BodyType.HTML)
				.withIsNotificationMessage(true)
				.build();	
		sesClient.sendRawEmail(sendEmailRequest);
	}

	@WriteTransaction
	@Override
	public LoginResponse createNewAccount(AccountSetupInfo accountSetupInfo) throws NotFoundException {
		String validatedEmail = PrincipalUtils.validateEmailValidationSignedToken(accountSetupInfo.getEmailValidationSignedToken(), new Date(), tokenGenerator);
		NewUser newUser = new NewUser();
		newUser.setEmail(validatedEmail);
		newUser.setFirstName(accountSetupInfo.getFirstName());
		newUser.setLastName(accountSetupInfo.getLastName());
		newUser.setUserName(accountSetupInfo.getUsername());
		long newPrincipalId = userManager.createUser(newUser);
		
		authManager.setPassword(newPrincipalId, accountSetupInfo.getPassword());
		return authManager.loginWithNoPasswordCheck(newPrincipalId);
	}

	@Override
	public void additionalEmailValidation(UserInfo userInfo, Username email, String portalEndpoint, Date now)
			throws NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(userInfo.getId()))
			throw new UnauthorizedException("Anonymous user may not add email address.");
		AliasEnum.USER_EMAIL.validateAlias(email.getEmail());
		// is the email taken?
		if (!principalAliasDAO.isAliasAvailable(email.getEmail())) {
			throw new NameConflictException("The email address provided is already used.");
		}
		EmailValidationSignedToken token = PrincipalUtils.createEmailValidationSignedToken(userInfo.getId(), email.getEmail(), now, tokenGenerator);
		String encodedToken = SerializationUtils.serializeAndHexEncode(token);
		String url = portalEndpoint+encodedToken;
		EmailUtils.validateSynapsePortalHost(url);

		// all requirements are met, so send the email
		String subject = "Request to add or change new email";
		Map<String,String> fieldValues = new HashMap<String,String>();
		UserProfile userProfile = userProfileDAO.get(userInfo.getId().toString());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_DISPLAY_NAME, userProfile.getFirstName()+" "+userProfile.getLastName());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_WEB_LINK, url);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_EMAIL, email.getEmail());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, "Synapse");
		fieldValues.put(EmailUtils.TEMPLATE_KEY_USERNAME, principalAliasDAO.getUserName(userInfo.getId()));
		String messageBody = EmailUtils.readMailTemplate("message/AdditionalEmailTemplate.html", fieldValues);
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(email.getEmail())
				.withSubject(subject)
				.withBody(messageBody, BodyType.HTML)
				.withIsNotificationMessage(true)
				.build();
		sesClient.sendRawEmail(sendEmailRequest);
	}

	@WriteTransaction
	@Override
	public void addEmail(UserInfo userInfo, EmailValidationSignedToken emailValidationSignedToken,
						 Boolean setAsNotificationEmail) throws NotFoundException {
		String newEmail = PrincipalUtils.validateAdditionalEmailSignedToken(emailValidationSignedToken, Long.toString(userInfo.getId()), new Date(), tokenGenerator);
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(newEmail);
		alias.setPrincipalId(userInfo.getId());
		alias.setType(AliasType.USER_EMAIL);
		alias = principalAliasDAO.bindAliasToPrincipal(alias);
		if (setAsNotificationEmail!=null && setAsNotificationEmail==true) notificationEmailDao.update(alias);
	}

	@WriteTransaction
	@Override
	public void removeEmail(UserInfo userInfo, String email) throws NotFoundException {
		if (email.equals(notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId())))
				throw new IllegalArgumentException("To remove this email from your account, first establish a different notification address.");
		PrincipalAlias emailAlias = findAliasForEmail(userInfo.getId(), email);
		principalAliasDAO.removeAliasFromPrincipal(userInfo.getId(), emailAlias.getAliasId());
	}
	
	@WriteTransaction
	@Override
	public void setNotificationEmail(UserInfo userInfo, String email) throws NotFoundException {
		PrincipalAlias emailAlias = findAliasForEmail(userInfo.getId(), email);
		notificationEmailDao.update(emailAlias);
	}

	@Override
	public Username getNotificationEmail(UserInfo userInfo) throws NotFoundException {
		String email= notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		Username dto = new Username();
		dto.setEmail(email);
		return dto;
	}

	@Override
	public PrincipalAliasResponse lookupPrincipalId(PrincipalAliasRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAlias(), "PrincipalAliasRequest.alias");
		ValidateArgument.required(request.getType(), "PrincipalAliasRequest.type");
		ValidateArgument.requirement(request.getType() == AliasType.USER_NAME, "Unsupported alias type "+request.getType());
		long principalId = principalAliasDAO.lookupPrincipalID(request.getAlias(), request.getType());
		PrincipalAliasResponse response = new PrincipalAliasResponse();
		response.setPrincipalId(principalId);
		return response;
	}

	private PrincipalAlias findAliasForEmail(Long principalId, String email) throws NotFoundException {
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL, email);
		if (aliases.size()==0) {
			throw new NotFoundException("Cannot find alias for "+principalId+" matching "+email);
		} else if (aliases.size()==1) {
			return aliases.get(0);
		} else {
			throw new DatastoreException("Expected 0-1 results but found "+aliases.size());
		}
	}
}
