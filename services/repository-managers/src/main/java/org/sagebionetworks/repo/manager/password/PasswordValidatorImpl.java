package org.sagebionetworks.repo.manager.password;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;


public class PasswordValidatorImpl implements PasswordValidator {
	static final int PASSWORD_MIN_LENGTH = 8;

	//from https://github.com/danielmiessler/SecLists/blob/master/Passwords/Common-Credentials/10-million-password-list-top-100000.txt
	@Value("classpath:10-million-password-list-top-100000.txt")
	private Resource bannedPasswordsFile;

	Set<String> bannedPasswordSet;

	@Override
	public void validatePassword(String password) {
		ValidateArgument.required(password, "password");
		if (password.length() < PASSWORD_MIN_LENGTH){
			throw new InvalidPasswordException("Password must contain "+PASSWORD_MIN_LENGTH+" or more characters .");
		}

		if (bannedPasswordSet.contains(password.toLowerCase())){
			throw new InvalidPasswordException("This password is known to be a commonly used password. Please choose another password!");
		}

	}

	//Called by Spring after fields are injected to initialize the bannedPasswordSet
	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		try (Stream<String> passwordPerLineStream =
					 new BufferedReader( new InputStreamReader(bannedPasswordsFile.getInputStream()) ).lines()){
			bannedPasswordSet = passwordPerLineStream
					.filter(password -> password.length() >= PASSWORD_MIN_LENGTH)
					.map(String::toLowerCase)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

