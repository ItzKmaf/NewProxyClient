package com.kmaf.ProxyClient.protocol.ProtocolX47.Server.outgoingPackets;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.OutgoingPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HS_Handshake_0x00_SB implements OutgoingPacket {
	
	private final String serverDomain;
	private final int serverPort;
	
	public HS_Handshake_0x00_SB(String serverDomain, int serverPort) {
		this.serverDomain = serverDomain;
		this.serverPort = serverPort;
	}
	
	@Override
	public GenericPacket generateOutgoingPacket() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteArrayOutputStream);
		Util.writeVarInt(47, out);
		Util.writeString(serverDomain, out);
		out.writeShort(serverPort);
		Util.writeVarInt(2, out);
		out.close();
		return new GenericPacket(0x00, byteArrayOutputStream.toByteArray());
	}
}
