package proxyti.protocol;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Primitive Minecraft protocol codecs: VarInts, strings, UUIDs and the length
 * prefixed packet framing. Everything here works on plain streams so it can be
 * layered on top of raw sockets, ciphers or compression.
 */
public final class Protocol {
    private Protocol() {
    }

    public static int readVarInt(InputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            int raw = in.read();
            if (raw < 0) {
                throw new EOFException("Unexpected end of stream while reading VarInt");
            }
            value |= (raw & 0x7F) << position;
            position += 7;
            if (position >= 35) {
                throw new IOException("VarInt is too big");
            }
            if ((raw & 0x80) == 0) {
                return value;
            }
        }
    }

    public static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    public static int varIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    public static String readString(InputStream in, int maxChars) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > maxChars * 4 + 3) {
            throw new IOException("String length out of bounds: " + length);
        }
        byte[] bytes = new byte[length];
        readFully(in, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeString(OutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    public static long readLong(InputStream in) throws IOException {
        byte[] b = new byte[8];
        readFully(in, b);
        long v = 0;
        for (byte value : b) {
            v = (v << 8) | (value & 0xFF);
        }
        return v;
    }

    public static void writeLong(OutputStream out, long value) throws IOException {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        out.write(b);
    }

    public static UUID readUuid(InputStream in) throws IOException {
        byte[] b = new byte[16];
        readFully(in, b);
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (b[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (b[i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }

    public static void writeUuid(OutputStream out, UUID uuid) throws IOException {
        writeLong(out, uuid.getMostSignificantBits());
        writeLong(out, uuid.getLeastSignificantBits());
    }

    /** Reads one uncompressed length-prefixed frame and returns its body. */
    public static byte[] readFrame(InputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > 16 * 1024 * 1024) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] body = new byte[length];
        readFully(in, body);
        return body;
    }

    /** Writes one uncompressed length-prefixed frame. */
    public static void writeFrame(OutputStream out, byte[] body) throws IOException {
        writeVarInt(out, body.length);
        out.write(body);
        out.flush();
    }

    public static byte[] frame(byte[] body) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(body.length + 5);
        writeFrame(buffer, body);
        return buffer.toByteArray();
    }

    public static void readFully(InputStream in, byte[] target) throws IOException {
        int offset = 0;
        while (offset < target.length) {
            int read = in.read(target, offset, target.length - offset);
            if (read < 0) {
                throw new EOFException("Unexpected end of stream");
            }
            offset += read;
        }
    }
}