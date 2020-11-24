package com.kmaf.ProxyClient.protocol.ProtocolX47.Server;

import com.kmaf.ProxyClient.protocol.GenericPacket;

import java.io.IOException;

public interface OutgoingPacket {
	
	GenericPacket generateOutgoingPacket() throws IOException;
	
}
