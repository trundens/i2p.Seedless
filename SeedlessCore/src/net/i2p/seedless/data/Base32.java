package net.i2p.seedless.data;

/*
 * Released into the public domain
 * with no warranty of any kind, either expressed or implied.
 */
/**
 * Encodes and decodes to and from Base32 notation.
 * Ref: RFC 3548
 *
 * Don't bother with '=' padding characters on encode or
 * accept them on decode (i.e. don't require 5-character groups).
 * No whitespace allowed.
 *
 * Decode accepts upper or lower case.
 */
public class Base32 {

    /** The 64 valid Base32 values. */
    private final static char[] ALPHABET = {'a', 'b', 'c', 'd',
                                            'e', 'f', 'g', 'h', 'i', 'j',
                                            'k', 'l', 'm', 'n', 'o', 'p',
                                            'q', 'r', 's', 't', 'u', 'v',
                                            'w', 'x', 'y', 'z',
                                            '2', '3', '4', '5', '6', '7'};
    /**
     * Translates a Base32 value to either its 5-bit reconstruction value
     * or a negative number indicating some other meaning.
     * Allow upper or lower case.
     **/
    private final static byte[] DECODABET = {
        26, 27, 28, 29, 30, 31, -9, -9, // Numbers two through nine
        -9, -9, -9, // Decimal 58 - 60
        -1, // Equals sign at decimal 61
        -9, -9, -9, // Decimal 62 - 64
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, // Letters 'A' through 'M'
        13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'N' through 'Z'
        -9, -9, -9, -9, -9, -9, // Decimal 91 - 96
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, // Letters 'a' through 'm'
        13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'n' through 'z'
        -9, -9, -9, -9, -9 // Decimal 123 - 127
    };
    private final static byte BAD_ENCODING = -9; // Indicates error in encoding

    /** Defeats instantiation. */
    private Base32() { // nop
    }

    public static String encode(String source) {
        return (source != null ? encode(source.getBytes()) : "");
    }

    public static String encode(byte[] source) {
        StringBuilder buf = new StringBuilder((source.length + 7) * 8 / 5);
        encodeBytes(source, buf);
        return buf.toString();
    }
    private final static byte[] emask = {(byte)0x1f,
                                         (byte)0x01, (byte)0x03, (byte)0x07, (byte)0x0f};

    /**
     * Encodes a byte array into Base32 notation.
     *
     * @param source The data to convert
     */
    private static void encodeBytes(byte[] source, StringBuilder out) {
        int usedbits = 0;
        for(int i = 0; i < source.length;) {
            int fivebits;
            if(usedbits < 3) {
                fivebits = (source[i] >> (3 - usedbits)) & 0x1f;
                usedbits += 5;
            } else if(usedbits == 3) {
                fivebits = source[i++] & 0x1f;
                usedbits = 0;
            } else {
                fivebits = (source[i++] << (usedbits - 3)) & 0x1f;
                if(i < source.length) {
                    usedbits -= 3;
                    fivebits |= (source[i] >> (8 - usedbits)) & emask[usedbits];
                }
            }
            out.append(ALPHABET[fivebits]);
        }
    }

    /**
     * Decodes data from Base32 notation and
     * returns it as a string.
     *
     * @param s the string to decode
     * @return The data as a string or null on failure
     */
    public static String decodeToString(String s) {
        byte[] b = decode(s);
        if(b == null) {
            return null;
        }
        return new String(b);
    }

    public static byte[] decode(String s) {
        return decode(s.getBytes());
    }
    private final static byte[] dmask = {(byte)0xf8, (byte)0x7c, (byte)0x3e, (byte)0x1f,
                                         (byte)0x0f, (byte)0x07, (byte)0x03, (byte)0x01};

    /**
     * Decodes Base32 content in byte array format and returns
     * the decoded byte array.
     *
     * @param source The Base32 encoded data
     * @return decoded data
     */
    private static byte[] decode(byte[] source) {
        int len58;
        if(source.length <= 1) {
            len58 = source.length;
        } else {
            len58 = source.length * 5 / 8;
        }
        byte[] outBuff = new byte[len58];
        int outBuffPosn = 0;

        int usedbits = 0;
        for(int i = 0; i < source.length; i++) {
            int fivebits;
            if((source[i] & 0x80) != 0 || source[i] < '2' || source[i] > 'z') {
                fivebits = BAD_ENCODING;
            } else {
                fivebits = DECODABET[source[i] - '2'];
            }

            if(fivebits >= 0) {
                if(usedbits == 0) {
                    outBuff[outBuffPosn] = (byte)((fivebits << 3) & 0xf8);
                    usedbits = 5;
                } else if(usedbits < 3) {
                    outBuff[outBuffPosn] |= (fivebits << (3 - usedbits)) & dmask[usedbits];
                    usedbits += 5;
                } else if(usedbits == 3) {
                    outBuff[outBuffPosn++] |= fivebits;
                    usedbits = 0;
                } else {
                    outBuff[outBuffPosn++] |= (fivebits >> (usedbits - 3)) & dmask[usedbits];
                    byte next = (byte)(fivebits << (11 - usedbits));
                    if(outBuffPosn < len58) {
                        outBuff[outBuffPosn] = next;
                        usedbits -= 3;
                    } else if(next != 0) {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
        return outBuff;
    }
}
