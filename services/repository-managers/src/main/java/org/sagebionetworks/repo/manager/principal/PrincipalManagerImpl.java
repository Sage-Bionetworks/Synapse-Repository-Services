package org.sagebionetworks.repo.manager.principal;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.WordUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

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
	private AmazonSimpleEmailService amazonSESClient;

	public static final String PARAMETER_CHARSET = Charset.forName("utf-8").name();
	public static final String EMAIL_VALIDATION_FIRST_NAME_PARAM = "firstname";
	public static final String EMAIL_VALIDATION_LAST_NAME_PARAM = "lastname";
	public static final String EMAIL_VALIDATION_EMAIL_PARAM = "email";
	public static final String EMAIL_VALIDATION_TIME_STAMP_PARAM = "timestamp";
	public static final String EMAIL_VALIDATION_SIGNATURE_PARAM = "mac";
	public static final String DATE_FORMAT_ISO8601 = "yyyy-mm-ddTHH:MM:SS.SSS";
	public static final String AMPERSAND = "&";
	public static final String EQUALS = "=";
	public static final long EMAIL_VALIDATION_TIME_LIMIT_MILLIS = 24*3600*1000L; // 24 hours as milliseconds
	
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
	
	public static String generateSignature(String payload) {
		try {
			byte[] secretKey = StackConfiguration.getEncryptionKey().getBytes(PARAMETER_CHARSET);
			byte[] signatureAsBytes = HMACUtils.generateHMACSHA1SignatureFromRawKey(payload, secretKey);
			return new String(signatureAsBytes, PARAMETER_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String generateSignatureForNewAccount(String firstName, String lastName, String email, String timestamp) {
		return generateSignature(email+timestamp);
	}
	
	public static String createTokenForNewAccount(NewUser user, Date now) {
		try {
			StringBuilder sb = new StringBuilder();
			String urlEncodedFirstName = URLEncoder.encode(user.getFirstName(), PARAMETER_CHARSET);
			sb.append(EMAIL_VALIDATION_FIRST_NAME_PARAM+EQUALS+urlEncodedFirstName);
			String urlEncodedLastName = URLEncoder.encode(user.getLastName(), PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_LAST_NAME_PARAM+EQUALS+urlEncodedLastName);
			String urlEncodedEmail = URLEncoder.encode(user.getEmail(), PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_EMAIL_PARAM+EQUALS+urlEncodedEmail);
			DateFormat df = new SimpleDateFormat(DATE_FORMAT_ISO8601);
			String timestampString = df.format(now);
			String urlEncodedTimeStampString = URLEncoder.encode(timestampString, PARAMETER_CHARSET);
			sb.append(AMPERSAND+EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS+urlEncodedTimeStampString);
			String mac = generateSignatureForNewAccount(urlEncodedFirstName, urlEncodedLastName, urlEncodedEmail, urlEncodedTimeStampString);
			sb.append(AMPERSAND+EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS+URLEncoder.encode(mac, PARAMETER_CHARSET));
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	// note, we pass the current time as a parameter to facilitate testing
	public static void validateNewAccountToken(String token, Date now) {
		String firstName = null;
		String lastName = null;
		String email = null;
		String tokenTimestampString = null;
		String mac = null;
		String[] requestParams = token.split(AMPERSAND);
		for (String param : requestParams) {
			if (param.startsWith(EMAIL_VALIDATION_FIRST_NAME_PARAM+EQUALS)) {
				firstName = param.substring((EMAIL_VALIDATION_FIRST_NAME_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_LAST_NAME_PARAM+EQUALS)) {
				lastName = param.substring((EMAIL_VALIDATION_LAST_NAME_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_EMAIL_PARAM+EQUALS)) {
				email = param.substring((EMAIL_VALIDATION_EMAIL_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS)) {
				tokenTimestampString = param.substring((EMAIL_VALIDATION_TIME_STAMP_PARAM+EQUALS).length());
			} else if (param.startsWith(EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS)) {
				mac = param.substring((EMAIL_VALIDATION_SIGNATURE_PARAM+EQUALS).length());
			}
		}
		if (firstName==null) throw new IllegalArgumentException("first name is missing.");
		if (lastName==null) throw new IllegalArgumentException("last name is missing.");
		if (email==null) throw new IllegalArgumentException("email is missing.");
		if (tokenTimestampString==null) throw new IllegalArgumentException("time stamp is missing.");
		if (mac==null) throw new IllegalArgumentException("digital signature is missing.");
		Date tokenTimestamp = null;
		DateFormat df = new SimpleDateFormat(DATE_FORMAT_ISO8601);
		try {
			tokenTimestamp = df.parse(tokenTimestampString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		if (now.getTime()-tokenTimestamp.getTime()>EMAIL_VALIDATION_TIME_LIMIT_MILLIS) 
			throw new IllegalArgumentException("Email validation link is out of date.");
		if (!mac.equals(generateSignatureForNewAccount(firstName, lastName, email, tokenTimestampString)))
			throw new IllegalArgumentException("Invalid digital signature.");
	}
	
	public static void validateSynapsePortalHost(String portalHost) {
		portalHost = portalHost.toLowerCase().trim();
		if (portalHost.endsWith("synapse.org")) return;
		if (portalHost.endsWith("sagebase.org")) return;
		if (portalHost.equals("localhost")) return;
		throw new IllegalArgumentException("The provided parameter is not a valid Synapse endpoint.");
	}
	
	// will throw exception for invalid email, invalid endpoint, invalid domain, or an email which is already taken
	@Override
	public void newAccountEmailValidation(NewUser user, String portalEndpoint,
			DomainType domain) {
		AliasEnum.USER_EMAIL.validateAlias(user.getEmail());
		
		if (domain.equals(DomainType.SYNAPSE)) {
			String token = createTokenForNewAccount(user, new Date());
			String urlString = portalEndpoint+token;
			URL url = null;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("The provided endpoint creates an invalid URL");
			}
			validateSynapsePortalHost(url.getHost());
			// is the email taken?
			if (!principalAliasDAO.isAliasAvailable(user.getEmail())) {
				throw new NameConflictException("The email address provided is already used.");
			}
			
			// all requirements are met, so send the email
			String domainString = WordUtils.capitalizeFully(domain.name());
			String subject = "Welcome to " + domain + "!";
			Map<String,String> fieldValues = new HashMap<String,String>();
			fieldValues.put(EmailUtils.TEMPLATE_KEY_DISPLAY_NAME, user.getFirstName()+" "+user.getLastName());
			fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, domainString);
			fieldValues.put(EmailUtils.TEMPLATE_KEY_WEB_LINK, urlString);
			String messageBody = EmailUtils.readMailTemplate("message/CreateAccountTemplate.txt", fieldValues);
			SendEmailRequest sendEmailRequest = EmailUtils.createEmailRequest(user.getEmail(), subject, messageBody, false, null);
			amazonSESClient.sendEmail(sendEmailRequest);
		} else if (domain.equals(DomainType.BRIDGE)) {
			throw new IllegalArgumentException("Account creation for Bridge is not yet supported.");
		} else {
			throw new IllegalArgumentException("Unexpected Domain: "+domain);
		}
	}

	@Override
	public String createNewAccount(AccountSetupInfo accountSetupInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void additionalEmailValidation(UserInfo userInfo, String email,
			String portalEndoint, DomainType domain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addEmail(UserInfo userInfo, String emailValidationToken,
			boolean setAsNotificationEmail) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeEmail(UserInfo userInfo, String email) throws NotFoundException {
		if (email.equals(notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId())))
				throw new IllegalArgumentException("To remove this email from your account, first establish a different notification address.");
		PrincipalAlias emailAlias = findAliasForEmail(userInfo.getId(), email);
		principalAliasDAO.removeAliasFromPrincipal(userInfo.getId(), emailAlias.getAliasId());
	}
	
	private PrincipalAlias findAliasForEmail(Long principalId, String email) throws NotFoundException {
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL);
		for (PrincipalAlias principalAlias : aliases) {
			if (!principalAlias.getAlias().equals(email)) return principalAlias;
		}
		throw new NotFoundException("Cannot find alias for "+principalId+" matching "+email);
	}

	/**
	 * Set the email which is used for notification.
	 * 
	 * @param principalId
	 * @param email
	 * @throws NotFoundException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setNotificationEmail(UserInfo userInfo, String email) throws NotFoundException {
		PrincipalAlias emailAlias = findAliasForEmail(userInfo.getId(), email);
		notificationEmailDao.update(emailAlias);
	}
	
	/**
	 * Get the email which is used for notification.
	 * 
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 */
	@Override
	public Username getNotificationEmail(UserInfo userInfo) throws NotFoundException {
		String email= notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		Username dto = new Username();
		dto.setEmail(email);
		return dto;
	}

}
