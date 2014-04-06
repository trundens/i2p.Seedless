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
package net.i2p.seedless.classes;

import java.io.Serializable;

/**
 *
 * @author sponge
 */
public class SeedlessServices implements Serializable {

    public String service = null;
    public String metadata = null;
    public String dest = null;
    public long date = 0;
    public boolean us = false;
    public boolean good = true;

    public SeedlessServices(String service, String metadata, String dest, long date, boolean us, boolean good) {
        super();
        this.service = service;
        this.metadata = metadata;
        this.dest = dest;
        this.date = date;
        this.us = us;
        this.good = good;
    }

    public SeedlessServices() {
        super();
    }
    // make a copy

    public SeedlessServices(SeedlessServices ss) {
        super();
        this.service = new String(ss.service);
        this.metadata = new String(ss.metadata);
        this.dest = new String(ss.dest);
        this.date = ss.date;
        this.us = ss.us;
        this.good = ss.good;
    }
}
