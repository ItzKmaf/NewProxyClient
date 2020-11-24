package com.kmaf.ProxyClient.Auth;

public class AuthenticationResponseException extends AuthenticationException {
	public AuthenticationResponseException(String url) {
		super("AuthResponseException " + url);
	}
}
