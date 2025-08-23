package org.sagebionetworks.repo.manager.oauth;

public class JWTTestHelper {

	/*
	 * When we mock stack configuration we have to introduce a (valid, though NOT production)
	 * RSA key that can be used to sign tokens.
	 */
	public static final String TEST_RSA_KEY_PAIR =
			"MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDinbEq1zwFsfSA" + 
			"FCajV9qrk2Qok9EiF/jlp/bRknCMEgj8j5oYiDxmnFXvnrSHJVk1NUOo4ON8m3WR" + 
			"MCLpN7t0ygIKvx30L7xdVlnvoldv/lYx87KqGEVadEnl/lGEaGRxpdXkiwgJem9l" + 
			"Ht7V8TquJiIcqQviwCc1ZKjtKtSbZtcpA0MxT/YNATf7GBWWD5KH39qL6/hHrCIa" + 
			"BYCqbNQt594/UZEgxRfB6sKyb2744Jshiq41Y25TJc4gNUq69EBMkaQrC9V67as6" + 
			"TJG2GJhfmAPsXgr7Lbyk5/kTsC8YXAasAzZnpA+ljLNv6pIdpyizx9XNrz13DFrY" + 
			"RK8lbHkrAgMBAAECggEANIuT9PcLN9bXdos1mlJYpcf7RV1g9KLSV43msRlfd1sH" + 
			"Mmiptl6AgtplIraN7Xg/gxLiqVnb5Zy2Wf/rWGBP2visGInQDDq1Vn8bQ3FFDPbQ" + 
			"TazQFJikHCEysV2S0TzTbXaibee+6VO2WKAb00en75Fv/21DEES10q+Qa82ulomG" + 
			"THIy7RitkdhkKPv16Lz7EQFcrdbZieldppijm15EK+wAKl8TAShIyZaAC8QY61F1" + 
			"hqqLApigI1GL7knP4jnii6sF7ykEpHb5huf3RI18DGwtUZOWLqimHgvmfn3p5NLT" + 
			"d/Xo56SOazhTBlvkNlUQccIQDzA8M2pjLcxxnMLcoQKBgQD0eRY63R3ZJh72OLCm" + 
			"C1iRS8NnQmdC+iR3zw7J5jvOkHSDctI1Zf6X+D4iyPLr1+tjnLy5YgTDP9PUJi9h" + 
			"3FRbnGkPD7I5q+19cvV5bQvxn36+kKZunfgLQYYe1Zk/6BbItOli5fzyQK3QTSp8" + 
			"eIKdQ02VQiFNV4UESbrb00UWNQKBgQDtTQ/hun6eYCVW9n3TmTi5+WRBb1R4X3kd" + 
			"/tifYhzy2Eab2pgQ3BN9INENlBsPb8J1+86rWwY2KVO9vHhctK47ytEn1kv//KCo" + 
			"ugo870dEZuAqX9ACNtG/Ba52IkCSjcpTJRo3R3lTLASV7vLZs4q5AyKNWCJD49kr" + 
			"KC9nNpW93wKBgQDRkYtQ4oPXxin8gBROAqPlycC0H+RNMglY+xJ+WPMj3AlFNYSl" + 
			"ac2ZkKATSZeUPP/34ECX2kKi7XA1CJbNmQZnkektlBMABTYMuCNd9/CpLESGL5G8" + 
			"eYZMf9rtS8WXVulRHGSE9wqi0HcvfTbShKvTDALR1GKf3kqUpm+cSbuLkQKBgCfS" + 
			"hNXGrDT7wYhkeR0nW2OqPG7WtgA1VWf5OnUUy/Lc5IyHFHnP1N1swmha8GeYw7N0" + 
			"/Gu5LMOuD8WJeVFlaM/T62GaDsr4pCVsgwdSyEzsTrYNuiSE+pHp7Csa+GcfsFJf" + 
			"qZSZQ/z3KBXZMZvjC2ac5hF+NtHZzLn3Vm0ltd9VAoGBAJcs9PBjJ9XqAHo6x88g" + 
			"7KxK1mfXRrVoxMLZD2njx4xWTK6OWOAECdWBIPbsstT57BVUeSa5wAVy/SkgfZ3E" + 
			"MjjcxwFQoYuOR/cgYAFyXY/jMXb6kKSdc02437hTXjTotvkgAJfO7gxVy8s3ipI7" + 
			"pzvMOgD+6P3bCeoBuyKg2xhY";
}
