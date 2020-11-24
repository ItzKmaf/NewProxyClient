package com.kmaf.ProxyClient.serverConnection;

import com.kmaf.ProxyClient.Auth.AuthenticationException;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets.L_EncryptionRequest_0x01_PB;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.outgoingPackets.L_EncryptionResponse_0x01_SB;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class EncryptionHandler {
	
	private final ServerConnectionManager manager;
	private final ServerConnection connection;
	private DataOutputStream outputStream;
	private DataInputStream inputStream;
	
	private PublicKey publicKey;
	private SecretKey secretKey;
	
	private byte[] sharedSecret;
	private byte[] verifyToken;
	
	private String serverID;
	private byte[] publicKeyByte;
	private byte[] verifyTokenByte;
	private boolean encrypting;
	private boolean decrypting;
	
	public EncryptionHandler(ServerConnectionManager manager, ServerConnection connection) {
		this.manager = manager;
		this.connection = connection;
	}
	
	public void setStreams() throws IOException {
		this.outputStream = new DataOutputStream(connection.getOutputStream());
		this.inputStream = new DataInputStream(connection.getInputStream());
	}
	
	public void handleEncryptionRequest(GenericPacket packet) throws IOException {
		System.out.println("[Encryption]: Received Encryption request");
		manager.pauseReading();
		L_EncryptionRequest_0x01_PB encryptionRequest = new L_EncryptionRequest_0x01_PB(manager, this);
		encryptionRequest.readPacket(packet);
		
		serverID = encryptionRequest.getServerID().trim();
		publicKeyByte = encryptionRequest.getPublicKeyByte();
		verifyTokenByte = encryptionRequest.getVerifyTokenByte();
		
		System.out.println("[Encryption]: Generating Keys...");
		generateKeypair();
		System.out.println("[Encryption]: Generated Keys");
		generateSharedSecret();
		loginMojang();
		
		System.out.println("[Encryption]: Sending Encryption response...");
		L_EncryptionResponse_0x01_SB encryptionResponse = new L_EncryptionResponse_0x01_SB(sharedSecret, verifyToken);
		manager.addOutgoingPacket(encryptionResponse.generateOutgoingPacket());
	}
	
	public void generateKeypair() {
		secretKey = EncryptionUtils.generateSecretKey();
		try {
			publicKey = EncryptionUtils.generatePublicKey(publicKeyByte);
		} catch (GeneralSecurityException exception) {
			throw new Error("Unable to generate public key", exception);
		}
	}
	
	private void generateSharedSecret() {
		try {
			this.sharedSecret = EncryptionUtils.cipher(1, publicKey, secretKey.getEncoded());
			this.verifyToken = EncryptionUtils.cipher(1, publicKey, verifyTokenByte);
		} catch (GeneralSecurityException exception) {
			throw new Error("Unable to cipher", exception);
		}
	}
	
	public void loginMojang() throws IOException{
		if (!serverID.equals("-")) {
			try {
				String hash = new BigInteger(EncryptionUtils.encrypt(serverID, publicKey, secretKey)).toString(16);
				manager.login(hash);
			} catch (AuthenticationException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void completeEncryption() {
		System.out.println("[Encryption]: Sent Encryption Response");
		if (secretKey != null) {
			System.out.println("Failed to Authenticate");
		}
		if (!encrypting) {
			setEncrypting(true);
		}
		if (secretKey == null) {
			System.out.println("Failed to Authenticate");
		}
		if (!decrypting) {
			setDecrypting(true);
			manager.resumeReading();
			try {
				System.out.println("Decrypting Available: " + inputStream.available());
				System.out.println("Original Available: " + connection.getInputStream().available());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setEncrypting(boolean encrypting) {
		if (encrypting) {
			if (this.encrypting) {
				throw new IllegalStateException("Already Encrypting");
			}
			if (secretKey == null) {
				throw new IllegalStateException("Shared key not set");
			}
			System.out.println("[Encryption]: Enabled Encryption of packets.");
			this.outputStream = new DataOutputStream(EncryptionUtils.encryptOutputStream(outputStream, secretKey));
		} else {
			throw new IllegalStateException("You cannot enable Encryption");
		}
		this.encrypting = true;
	}
	
	public void setDecrypting(boolean decrypting) {
		if (decrypting) {
			if (this.decrypting)
				throw new IllegalStateException("Already decrypting");
			if (secretKey == null)
				throw new IllegalStateException("Shared key not set");
			System.out.println("[Encryption]: Enabled Decryption of packets.");
			this.inputStream = new DataInputStream(EncryptionUtils.decryptInputStream(inputStream, secretKey));
		} else {
			throw new IllegalStateException("You cannot disable Decryption");
		}
		this.decrypting = true;
	}
	
	public DataInputStream getInputStream() {
		return inputStream;
	}
	
	public DataOutputStream getOutputStream() {
		return outputStream;
	}
}
