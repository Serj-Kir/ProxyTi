package proxyti.protocol;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The very first packet on every modern Minecraft connection. It is always sent
 * before compression/encryption are negotiated, which is what makes a
 * transparent proxy possible: routing only needs to read this one packet.
 */
public final class Handshake {
    public int protocolVersion;
    public String host;
    public int port;
    public int nextState;

    public static final int STATE_STATUS = 1;
    public static final int STATE_LOGIN = 2;
    public static final int STATE_TRANSFER = 3;

    public static Handshake read(InputStream in) throws IOException {
        byte[] body = Protocol.readFrame(in);
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(body));
        int id = Protocol.readVarInt(data);
        if (id != 0x00) {
            throw new IOException("Expected Handshake (0x00) but got 0x" + Integer.toHexString(id));
        }
        Handshake handshake = new Handshake();
        handshake.protocolVersion = Protocol.readVarInt(data);
        handshake.host = Protocol.readString(data, 255);
        handshake.port = data.readUnsignedShort();
        handshake.nextState = Protocol.readVarInt(data);
        return handshake;
    }

    public byte[] encode(int nextState) throws IOException {
        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream(64);
        Protocol.writeVarInt(body, 0x00);
        Protocol.writeVarInt(body, protocolVersion);
        Protocol.writeString(body, host);
        body.write((port >> 8) & 0xFF);
        body.write(port & 0xFF);
        Protocol.writeVarInt(body, nextState);
        return body.toByteArray();
    }

    public boolean isStatus() {
        return nextState == STATE_STATUS;
    }

    public boolean isLogin() {
        return nextState == STATE_LOGIN || nextState == STATE_TRANSFER;
    }

    @Override
    public String toString() {
        return "Handshake{protocol=" + protocolVersion + ", host='" + host + "', port=" + port
                + ", nextState=" + nextState + "}";
    }
}