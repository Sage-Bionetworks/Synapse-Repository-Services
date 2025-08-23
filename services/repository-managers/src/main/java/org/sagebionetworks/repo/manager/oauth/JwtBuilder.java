package org.sagebionetworks.repo.manager.oauth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.KeyPairUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class JwtBuilder {
	
	@Autowired
	private StackConfiguration stackConfiguration;

	public String createSignedJWT(Claims claims) {
		// grab the latest private key to be used for signing
		List<String> pemEncodedRsaPrivateKeys = stackConfiguration.getOIDCSignatureRSAPrivateKeys();
		KeyPair keyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(pemEncodedRsaPrivateKeys.get(pemEncodedRsaPrivateKeys.size()-1));
		PrivateKey oidcSignaturePrivateKey=keyPair.getPrivate();
		String oidcSignatureKeyId = KeyPairUtil.computeKeyId(keyPair.getPublic());

		return Jwts.builder()
			.setClaims(claims)
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setHeaderParam(JwsHeader.KEY_ID, oidcSignatureKeyId)
			.signWith(oidcSignaturePrivateKey, SignatureAlgorithm.RS256)
			.compact();
	}

}
