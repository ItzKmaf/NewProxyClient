package com.kmaf.ProxyClient.Auth;

public class AuthenticationException extends Exception {
	public AuthenticationException(String url) {
		super(url);
	}
}
