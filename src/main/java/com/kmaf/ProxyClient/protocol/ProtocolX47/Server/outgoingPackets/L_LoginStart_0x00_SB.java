package com.kmaf.ProxyClient.protocol.ProtocolX47.Server.outgoingPackets;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.OutgoingPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class L_LoginStart_0x00_SB implements OutgoingPacket {
	
	private final String username;
	
	public L_LoginStart_0x00_SB(String username) {
		this.username = username;
	}
	
	@Override
	public GenericPacket generateOutgoingPacket() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteArrayOutputStream);
		Util.writeString(username, out);
		out.close();
		return new GenericPacket(0x00, byteArrayOutputStream.toByteArray());
	}
}
