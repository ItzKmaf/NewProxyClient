package com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.IncomingPacket;
import com.kmaf.ProxyClient.serverConnection.EncryptionHandler;
import com.kmaf.ProxyClient.serverConnection.ServerConnectionManager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class L_EncryptionRequest_0x01_PB implements IncomingPacket {
	
	private final ServerConnectionManager manager;
	private final EncryptionHandler handler;
	
	private String serverID;
	private byte[] publicKeyByte;
	private byte[] verifyTokenByte;
	
	public L_EncryptionRequest_0x01_PB(ServerConnectionManager manager, EncryptionHandler handler) {
		this.manager = manager;
		this.handler = handler;
	}
	
	@Override
	public void readPacket(GenericPacket packet) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));
		serverID = Util.readStringAscii(in);
		int publicKeyLength = Util.readVarInt(in);
		publicKeyByte = Util.readVarByteArray(in, publicKeyLength);
		int verifyTokenLength = Util.readVarInt(in);
		verifyTokenByte = Util.readVarByteArray(in, verifyTokenLength);
		in.close();
	}
	
	public String getServerID() {
		return serverID;
	}
	
	public byte[] getPublicKeyByte() {
		return publicKeyByte;
	}
	
	public byte[] getVerifyTokenByte() {
		return verifyTokenByte;
	}
}


