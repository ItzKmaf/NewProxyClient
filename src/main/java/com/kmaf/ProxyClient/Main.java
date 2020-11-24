package com.kmaf.ProxyClient;

import com.ItzKmaf.configuration.Config;
import com.kmaf.ProxyClient.Auth.AuthenticationException;
import com.kmaf.ProxyClient.Auth.Yggdrasil;
import com.kmaf.ProxyClient.serverConnection.ServerConnectionManager;

import java.io.IOException;
import java.util.UUID;

public class Main {
	
	
	public static void main(String[] args) {
		Config config = new Config("config.yml");
		Config accounts = new Config("accounts.yml");
		String serverDomain = config.getConfig().getString("serverDomain");
		int serverPort = config.getConfig().getInt("serverPort");
		System.out.println("[Config]: Server Domain: " + serverDomain);
		System.out.println("[Config]: Server Port: " + serverPort);
		Yggdrasil yggdrasil = new Yggdrasil(config.getConfig().getString("email"), config.getConfig().getString("password"), UUID.randomUUID(), accounts);
		System.out.println("[AUTH]: Starting login handshake with Mojang Auth servers.");
		try {
			if (yggdrasil.loadFromConfig()) {
				System.out.println("[AUTH]: Found existing cached credentials. Attempting to validate them with Mojang.");
				try {
					yggdrasil.validate();
				} catch (AuthenticationException aE) {
					System.out.println("[AUTH]: Failed to Validate Existing cached credentials. Attempting to Authenticate from scratch.");
					yggdrasil.authenticate();
				}
			} else {
				System.out.println("[AUTH]: Failed to find existing cached credentials. Attempting to Authenticate from scratch.");
				yggdrasil.authenticate();
			}
			System.out.println("[AUTH]: Successfully Authenticated with Mojang Server.");
			System.out.println("[AUTH]: Caching credentials in accounts.yml for faster start up next time");
			yggdrasil.saveToConfig();
			System.out.println("[AUTH]: Caching seems to have been successful");
		} catch (AuthenticationException aE) {
			System.out.println("[AUTH]: Failed to AUTH (reason: " + aE.getMessage() + ")");
			System.out.println("Closing Console Client in 5 Seconds...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.out.println();
			}
			return;
		}
		// Starting Console Client
		ServerConnectionManager manager = new ServerConnectionManager(serverDomain, serverPort, yggdrasil);
		try {
			manager.connect();
			while (true) {
				manager.processPacket();
				manager.readIncomingPacket();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
