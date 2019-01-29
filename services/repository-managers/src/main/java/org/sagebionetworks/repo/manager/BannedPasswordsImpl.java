package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;


public class BannedPasswordsImpl implements BannedPasswords{
	//from https://github.com/danielmiessler/SecLists/blob/master/Passwords/Common-Credentials/10k-most-common.txt
	@Value("classpath:10k-most-common-passwords.txt")
	private Resource bannedPasswordsFile;

	private Set<String> bannedPasswordSet;

	@Override
	public boolean isPasswordBanned(String password) {
		ValidateArgument.required(password, "password");
		return bannedPasswordSet.contains(password.toLowerCase());
	}

	//Called by Spring after fields are injected to initialize the bannedPasswordSet
	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		try (Stream<String> lineStream =
					 new BufferedReader( new InputStreamReader(bannedPasswordsFile.getInputStream()) ).lines()){
			bannedPasswordSet = lineStream
					.map(String::toLowerCase)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

