/**
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                    Version 2, December 2004
 *
 * Copyright (C) sponge
 *   Planet Earth
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 * See...
 *
 *	http://sam.zoy.org/wtfpl/
 *	and
 *	http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */
package net.i2p.seedless.data;

import gnu.crypto.hash.Sha256Standalone;

/**
 *
 * @author sponge
 */
public class DestMapper {

    void DestMapper() {
    }

    /**
     *
     * @param dest Base64 encoded destination key.
     * @return Base32 url
     */
    public String b32(String dest) {
        String rv = "";
        try {
            rv = Base32.encode(calculateHash(Base64.decode(dest))) + ".b32.i2p";
        } catch(NullPointerException npe) {
        }
        return rv;
    }

    /**
     *
     * @param source
     * @return Sha256 hash
     * @throws java.lang.NullPointerException
     */
    private byte[] calculateHash(byte[] source) throws NullPointerException {
        byte rv[] = null;
        Sha256Standalone digest = new Sha256Standalone();
        digest.update(source, 0, source.length);
        rv = digest.digest();
        digest = null;
        return rv;
    }
}
