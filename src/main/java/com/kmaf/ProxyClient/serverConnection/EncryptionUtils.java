package com.kmaf.ProxyClient.serverConnection;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class EncryptionUtils {
	
	public static SecretKey generateSecretKey() {
		CipherKeyGenerator generator = new CipherKeyGenerator();
		generator.init(new KeyGenerationParameters(new SecureRandom(), 128));
		return new SecretKeySpec(generator.generateKey(), "AES");
	}
	
	public static byte[] encrypt(String string, PublicKey publicKey, SecretKey secretKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return hash(string.getBytes(StandardCharsets.US_ASCII), secretKey.getEncoded(), publicKey.getEncoded());
	}
	
	private static byte[] hash(byte[]... data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		for (byte[] section : data)
			digest.update(section);
		return digest.digest();
	}
	
	public static PublicKey generatePublicKey(byte[] encodedKey) throws GeneralSecurityException {
		X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return factory.generatePublic(spec);
	}
	
	public static byte[] cipher(int method, Key key, byte[] data) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance(key.getAlgorithm());
		cipher.init(method, key);
		return cipher.doFinal(data);
	}
	
	public static OutputStream encryptOutputStream(OutputStream outputStream, SecretKey key) {
		return new CipherOutputStream(outputStream, createBlockCipher(key, true));
	}
	
	public static InputStream decryptInputStream(InputStream inputStream, SecretKey key) {
		return new CipherInputStream(inputStream, createBlockCipher(key, false));
	}
	
	public static BufferedBlockCipher createBlockCipher(SecretKey key, boolean out) {
		BufferedBlockCipher blockCipher = new BufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 8));
		blockCipher.init(out, new ParametersWithIV(new KeyParameter(key.getEncoded()), key.getEncoded(), 0, 16));
		return blockCipher;
	}
}
