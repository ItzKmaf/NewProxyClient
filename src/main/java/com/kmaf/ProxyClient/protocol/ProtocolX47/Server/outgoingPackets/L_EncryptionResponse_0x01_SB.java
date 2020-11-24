package com.kmaf.ProxyClient.protocol.ProtocolX47.Server.outgoingPackets;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.OutgoingPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class L_EncryptionResponse_0x01_SB implements OutgoingPacket {
	
	private final byte[] sharedSecret;
	private final byte[] verifyToken;
	
	public L_EncryptionResponse_0x01_SB(byte[] sharedSecret, byte[] verifyToken) {
		this.sharedSecret = sharedSecret;
		this.verifyToken = verifyToken;
	}
	
	@Override
	public GenericPacket generateOutgoingPacket() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteArrayOutputStream);
		Util.writeVarByteArray(sharedSecret, sharedSecret.length, out);
		Util.writeVarByteArray(verifyToken, verifyToken.length, out);
		out.close();
		return new GenericPacket(0x01, byteArrayOutputStream.toByteArray());
	}
}
