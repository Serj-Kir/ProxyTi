package proxyti.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A decoded Minecraft packet: the numeric id plus the raw payload that followed
 * it. Kept deliberately dumb so it can be forwarded verbatim.
 */
public final class Packet {
    public final int id;
    public final byte[] payload;

    public Packet(int id, byte[] payload) {
        this.id = id;
        this.payload = payload;
    }

    /** Splits a frame body (id varint + payload) into a Packet. */
    public static Packet decode(byte[] body) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(body);
        int id = Protocol.readVarInt(in);
        byte[] payload = new byte[in.available()];
        Protocol.readFully(in, payload);
        return new Packet(id, payload);
    }

    /** Re-encodes this packet into a frame body (id varint + payload). */
    public byte[] encode() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(payload.length + 5);
        Protocol.writeVarInt(out, id);
        out.write(payload);
        return out.toByteArray();
    }

    @Override
    public String toString() {
        return "Packet{id=0x" + Integer.toHexString(id) + ", payload=" + payload.length + " bytes}";
    }
}