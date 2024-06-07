package org.sagebionetworks.repo.manager.password;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

/**
 * 
 */
public class PasswordValidatorImpl implements PasswordValidator {

	static final int PASSWORD_MIN_LENGTH = 8;

	static final Set<Character> SPECIAL = Set.of('~', '!', '@', '#', '$', '%', '^', '&', '*', '_', '-', '+', '=', '`',
			'|', '\\', '(', ')', '{', '}', '[', ']', ':', ';', '"', '\'', '<', '>', ',', '.', '?', '/');

	public static final String INVALID_PASSWORD_MESSAGE = String.format(
			"A valid password must be at least %d characters long and must include letters, digits (0-9), and special characters %s",
			PASSWORD_MIN_LENGTH, setToString(SPECIAL));

	static String setToString(Set<Character> set) {
		StringBuilder builder = new StringBuilder();
		set.stream().sorted().forEach(c->{
			builder.append(c);
		});
		return builder.toString();
	}

	// from
	// https://github.com/danielmiessler/SecLists/blob/master/Passwords/Common-Credentials/10-million-password-list-top-100000.txt
	@Value("classpath:10-million-password-list-top-100000.txt")
	private Resource bannedPasswordsFile;

	Set<String> bannedPasswordSet;

	@Override
	public void validatePassword(String password) {
		ValidateArgument.required(password, "password");
		if (password.length() < PASSWORD_MIN_LENGTH || !containsLetterAndDigitAndSpecial(password)) {
			throw new InvalidPasswordException(INVALID_PASSWORD_MESSAGE);
		}

		String lowerPassword = password.toLowerCase();
		if (bannedPasswordSet.contains(lowerPassword) | lowerPassword.contains("synapse")) {
			throw new InvalidPasswordException(
					"This password is known to be a commonly used password. Please choose another password!");
		}
	}

	/**
	 * Does the provide password contain letters, digits, and special characters?
	 * 
	 * @param password
	 * @return
	 */
	static boolean containsLetterAndDigitAndSpecial(String password) {
		boolean hasLetter = false;
		boolean hasDigit = false;
		boolean hasSpecial = false;
		for(int i=0; i < password.length(); i++){
			char c = password.charAt(i);
			if(c >= 'a' && c <= 'z'){
				hasLetter = true;
			}
			if(c >='A' && c <= 'Z'){
				hasLetter = true;
			}
			if(c >= '0' && c <= '9'){
				hasDigit = true;
			}
			if(SPECIAL.contains(c)){
				hasSpecial = true;
			}
			if(hasLetter && hasDigit && hasSpecial) {
				return true;
			}
		}
		return false;
	}

	// Called by Spring after fields are injected to initialize the
	// bannedPasswordSet
	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(bannedPasswordsFile.getInputStream()))) {
			bannedPasswordSet = reader.lines()
					.filter(password -> password.length() >= PASSWORD_MIN_LENGTH
							&& containsLetterAndDigitAndSpecial(password))
					.map(String::toLowerCase)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
			System.out.println(
					"Number of common passwords that meet the minimum requirements: " + bannedPasswordSet.size());
			System.out.println(bannedPasswordSet.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
