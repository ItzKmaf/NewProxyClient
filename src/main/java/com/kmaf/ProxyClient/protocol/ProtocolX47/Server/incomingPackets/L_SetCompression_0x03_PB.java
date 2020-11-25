package com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.IncomingPacket;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class L_SetCompression_0x03_PB implements IncomingPacket {
	
	private int threshold;
	
	
	@Override
	public void readPacket(GenericPacket packet) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));
		threshold = Util.readVarInt(in);
		in.close();
	}
	
	public int getThreshold() {
		return threshold;
	}
}
