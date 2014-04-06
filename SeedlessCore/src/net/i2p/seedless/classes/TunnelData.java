/*
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                     Version 2, December 2004
 *
 *  Copyright (C) sponge
 *    Planet Earth
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *   0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 * See...
 *
 * 	http://sam.zoy.org/wtfpl/
 * 	and
 * 	http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */
package net.i2p.seedless.classes;

import java.io.Serializable;

/**
 *
 * @author sponge
 */
public class TunnelData implements Serializable {

    public String base32 = null;
    public String inFromI2P = null;
    public String outToI2P = null;
    public boolean up = false;
    public String dir = null;

    public TunnelData(String base32, String fromI2P, String toI2P, boolean up, String dir) {
        super();
        this.base32 = base32;
        this.inFromI2P = fromI2P;
        this.outToI2P = toI2P;
        this.up = up;
        this.dir = dir;
    }

    public TunnelData() {
        super();
    }
}
