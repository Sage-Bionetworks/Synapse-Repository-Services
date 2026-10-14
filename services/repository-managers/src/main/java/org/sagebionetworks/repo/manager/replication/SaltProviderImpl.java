package org.sagebionetworks.repo.manager.replication;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class SaltProviderImpl implements SaltProvider {

	private final Random random;

	public SaltProviderImpl(Random random) {
		super();
		this.random = random;
	}

	@Override
	public Long nextLong() {
		return random.nextLong();
	}

}
