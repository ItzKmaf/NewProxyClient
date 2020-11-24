package com.kmaf.ProxyClient.Auth;

import com.ItzKmaf.configuration.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

/*
Most of this was copied from TGRHavoc's AiBot.
Cheers to him and hopefully he is okay with me skidding it.
 */

public class Yggdrasil {
	
	private final String NAME = "Minecraft";
	private final Integer VERSION = 1;
	
	private final String email;
	private final String password;
	private final Config accountConfig;
	private String clientIdentifier;
	private String username;
	private String accessToken;
	private String profileID;
	
	public Yggdrasil(String email, String password, UUID clientIdentifier, Config accountConfig) {
		this.email = email;
		this.password = password;
		System.out.println("Email: " + email);
		System.out.println("Password: " + password);
		this.clientIdentifier = clientIdentifier.toString();
		this.accountConfig = accountConfig;
		
	}
	
	public boolean loadFromConfig() {
		String base = email + ".";
		String clientIdentifier = accountConfig.getConfig().getString(base + "uuid");
		String accessToken = accountConfig.getConfig().getString(base + "accessToken");
		String username = accountConfig.getConfig().getString(base + "username");
		String profileID = accountConfig.getConfig().getString(base + "profileID");
		if (clientIdentifier == null || accessToken == null || profileID == null || clientIdentifier.isEmpty() || accessToken.isEmpty() || profileID.isEmpty()) {
			return false;
		} else {
			this.clientIdentifier = clientIdentifier;
			this.accessToken = accessToken;
			this.username = username;
			this.profileID = profileID;
			return true;
		}
		
	}
	
	public void saveToConfig() {
		String base = email + ".";
		accountConfig.getConfig().set(base + "uuid", clientIdentifier);
		accountConfig.getConfig().set(base + "accessToken", accessToken);
		accountConfig.getConfig().set(base + "username", username);
		accountConfig.getConfig().set(base + "profileID", profileID);
		accountConfig.saveConfig();
	}
	
	public String getEmail() {
		return email;
	}
	
	private HttpsURLConnection sendRequest(JSONObject requestJSON, String urlString) throws AuthenticationException {
		try {
			String request = requestJSON.toJSONString();
			URL url = new URL(urlString);
			HttpsURLConnection con;
			con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Content-Length", Integer.toString(request.length()));
			con.setRequestProperty("Content-Language", "en-US");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			
			DataOutputStream out = new DataOutputStream(con.getOutputStream());
			out.writeBytes(request);
			out.flush();
			return con;
		} catch (IOException e) {
			throw new AuthenticationException("Error Posting Request to Auth Server with url: " + urlString);
		}
	}
	
	@SuppressWarnings("unchecked")
	public String authenticate() throws AuthenticationException {
		JSONObject agent = new JSONObject();
		JSONObject request = new JSONObject();
		agent.put("name", NAME);
		agent.put("version", VERSION);
		request.put("agent", agent);
		request.put("username", email);
		request.put("password", password);
		request.put("clientToken", clientIdentifier);
		request.put("requestUser", true);
		HttpsURLConnection con = sendRequest(request, "https://authserver.mojang.com/authenticate");
		try {
			if (con.getResponseCode() == 200) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					Object response;
					StringBuilder responseBuilder = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						if (responseBuilder.length() > 0)
							responseBuilder.append('\n');
						responseBuilder.append(line);
					}
					if (responseBuilder.toString().trim().isEmpty()) {
						return null;
					}
					JSONParser parser = new JSONParser();
					response = parser.parse(responseBuilder.toString());
					if (!(response instanceof JSONObject)) {
						throw new AuthenticationException("The Auth Server did not send a valid response.");
					}
					JSONObject resp = (JSONObject) response;
					String accessTokenValue = (String) resp.get("accessToken");
					JSONObject selectedProfileValue = (JSONObject) resp.get("selectedProfile");
					username = (String) selectedProfileValue.get("name");
					profileID = (String) selectedProfileValue.get("userId");
					if (profileID == null) {
						profileID = (String) selectedProfileValue.get("id");
					}
					this.accessToken = accessTokenValue;
					accountConfig.getConfig().set(email + ".accessToken", accessToken);
					accountConfig.getConfig().set(email + ".username", username);
					accountConfig.getConfig().set(email + ".profileID", profileID);
					accountConfig.saveConfig();
					return username;
				}
			} else {
				throw new AuthenticationException("Mojang's Auth Server did not validate your login.");
			}
		} catch (IOException e) {
			throw new AuthenticationException("The Auth Server did not send a valid response");
		} catch (ParseException e) {
			throw new AuthenticationException(e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	private String refresh() throws AuthenticationException {
		JSONObject request = new JSONObject();
		request.put("accessToken", accessToken);
		request.put("clientToken", clientIdentifier);
		HttpsURLConnection con = sendRequest(request, "https://authserver.mojang.com/refresh");
		try {
			if (con.getResponseCode() == 200) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					Object response;
					StringBuilder responseBuilder = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						if (responseBuilder.length() > 0)
							responseBuilder.append('\n');
						responseBuilder.append(line);
					}
					if (responseBuilder.toString().trim().isEmpty()) {
						return null;
					}
					JSONParser parser = new JSONParser();
					response = parser.parse(responseBuilder.toString());
					if (!(response instanceof JSONObject)) {
						throw new AuthenticationException("The Auth Server did not send a valid response.");
					}
					JSONObject resp = (JSONObject) response;
					String accessTokenValue = (String) resp.get("accessToken");
					JSONObject selectedProfileValue = (JSONObject) resp.get("selectedProfile");
					username = (String) selectedProfileValue.get("name");
					profileID = (String) selectedProfileValue.get("userId");
					if (profileID == null) {
						profileID = (String) selectedProfileValue.get("id");
					}
					this.accessToken = accessTokenValue;
					accountConfig.getConfig().set(email + ".accessToken", accessToken);
					accountConfig.getConfig().set(email + ".username", username);
					accountConfig.getConfig().set(email + ".profileID", profileID);
					accountConfig.saveConfig();
					
					
					return username;
				}
			} else {
				throw new AuthenticationException("Mojang's Auth Server did not validate your login.");
			}
		} catch (IOException e) {
			throw new AuthenticationException("The Auth Server did not send a valid response");
		} catch (ParseException e) {
			throw new AuthenticationException(e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void validate() throws AuthenticationException {
		JSONObject request = new JSONObject();
		request.put("accessToken", accessToken);
		request.put("clientToken", clientIdentifier);
		HttpsURLConnection con = sendRequest(request, "https://authserver.mojang.com/validate");
		try {
			if (con.getResponseCode() == 204) {
			} else {
				refresh();
			}
		} catch (IOException e) {
			throw new AuthenticationException("The Auth Server did not send a valid response");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void login(String serverID) throws AuthenticationException, IOException {
		JSONObject request = new JSONObject();
		request.put("accessToken", accessToken);
		request.put("selectedProfile", profileID);
		request.put("serverId", serverID);
		HttpsURLConnection con = sendRequest(request, "https://sessionserver.mojang.com/session/minecraft/join");
		if (con.getResponseCode() == 204) {
			System.out.println("Successfully Joined Mojang session server");
			return;
		} else {
			System.out.println(con.getResponseMessage());
			Object response;
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
				StringBuilder responseBuilder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					if (responseBuilder.length() > 0)
						responseBuilder.append('\n');
					responseBuilder.append(line);
				}
				if (responseBuilder.toString().trim().isEmpty()) {
					return;
				}
				JSONParser parser = new JSONParser();
				response = parser.parse(responseBuilder.toString());
				if (!(response instanceof JSONObject)) {
					throw new AuthenticationException("The Auth Server did not send a valid response.");
				}
				JSONObject resp = (JSONObject) response;
				System.out.println(resp.toJSONString());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			throw new AuthenticationException("Failed to join server");
		}
	}
	
	public String getUsername() {
		return username;
	}
}
