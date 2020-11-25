package com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.IncomingPacket;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class L_LoginSuccess_0x02_PB implements IncomingPacket {
	
	private String uuid;
	private String username;
	
	
	@Override
	public void readPacket(GenericPacket packet) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));
		uuid = Util.readStringAscii(in);
		username = Util.readStringAscii(in);
		in.close();
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getUuid() {
		return uuid;
	}
}
