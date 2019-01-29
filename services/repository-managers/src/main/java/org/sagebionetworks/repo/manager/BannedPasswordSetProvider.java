package org.sagebionetworks.repo.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

public class BannedPasswordSetProvider {
	@Value("classpath:10k-most-common-passwords.txt")
	private Resource bannedPasswordsFile;

	//Passwords that are very common and used in dictionary attacks. https://github.com/danielmiessler/SecLists/blob/master/Passwords/Common-Credentials/10k-most-common.txt
	private Set<String> bannedPasswordSet;

	public Set<String> getBannedPasswordSet() {
		if (bannedPasswordSet != null){
			return bannedPasswordSet;
		}

		try (Stream<String> lineStream = Files.lines(bannedPasswordsFile.getFile().toPath())){
			bannedPasswordSet = lineStream
					.map(String::toLowerCase)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
			return bannedPasswordSet;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

