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
public class Server implements Serializable {

    public boolean server = false;
    public String VERSION = null;
    public String b32 = null;

    public Server() {
        super();
    }

    public boolean getServer() {
        return server;
    }

    public void setServer(boolean value) {
        server = value;
    }

    public String getVERSION() {
        return VERSION;
    }

    public void setVERSION(String value) {
        VERSION = value;
    }

    public String getB32() {
        return b32;
    }

    public void setB32(String value) {
        b32 = value;
    }
}
