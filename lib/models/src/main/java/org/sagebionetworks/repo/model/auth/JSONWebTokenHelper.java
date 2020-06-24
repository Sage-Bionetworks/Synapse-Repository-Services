package org.sagebionetworks.repo.model.auth;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.util.ValidateArgument;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

public class JSONWebTokenHelper {
	public static final String RSA = "RSA";
	
	/**
	 * Given a signed JSON Web Token (JWT) and a list of signature verification public keys,
	 * parse the token and check the signature and expiration date.  The JWT will have a key 
	 * ID in its header which indicates which public key to use to validate the signature.
	 * 
	 * @param token
	 * @param jsonWebKeySet
	 * @return
	 * @throws IllegalArgumentException if the token is invalid or the signature incorrect.
	 * @throws io.jsonwebtoken.ExpiredJwtException if the token is expired
	 */
	public static Jwt<JwsHeader,Claims> parseJWT(String token, JsonWebKeySet jsonWebKeySet) {
		ValidateArgument.required(token, "JSON Web Token");
		ValidateArgument.required(jsonWebKeySet, "JSON Web Key Set");
		// This is a little awkward:  We first have to parse the token to
		// find the key Id, then, once we map the key Id to the signing key,
		// we parse again, setting the matching public key for verification
		String[] pieces = token.split("\\.");
		if (pieces.length!=3) throw new IllegalArgumentException("Expected three sections of the token but found "+pieces.length);
		String unsignedToken = pieces[0]+"."+pieces[1]+".";
		JsonWebKey matchingKey=null;
		{
			// Expiration time is checked by the parser
			Jwt<Header,Claims> unsignedJwt = Jwts.parser().parseClaimsJwt(unsignedToken);

			String keyId = (String)unsignedJwt.getHeader().get(JwsHeader.KEY_ID);
			for (JsonWebKey jwk : jsonWebKeySet.getKeys()) {
				if (jwk.getKid().equals(keyId)) {
					matchingKey = jwk;
					break;
				}
			}
			if (matchingKey==null) {
				throw new IllegalArgumentException("Could not find token key, "+keyId+" in the list of available public keys.");
			}
		}
		Jwt<JwsHeader,Claims> result = null;
		try {
			Key rsaPublicKey = getRSAPublicKeyForJsonWebKeyRSA((JsonWebKeyRSA)matchingKey);
			result = Jwts.parser().setSigningKey(rsaPublicKey).parse(token);
		} catch (SignatureException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return result;
	}
	
	private static BigInteger base64URLEncodedToBigInteger(String s) {
		byte[] bytes = Base64.getUrlDecoder().decode(s);
		return new BigInteger(bytes);
	}

	public static RSAPublicKey getRSAPublicKeyForJsonWebKeyRSA(JsonWebKeyRSA jwkRsa) {
		BigInteger modulus = base64URLEncodedToBigInteger(jwkRsa.getN());
		BigInteger publicExponent = base64URLEncodedToBigInteger(jwkRsa.getE());
		RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
		try {
			KeyFactory kf = KeyFactory.getInstance(RSA);
			return (RSAPublicKey)kf.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		} 
	}
}
