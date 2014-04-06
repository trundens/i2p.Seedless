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
package net.i2p.seedless.server;

import net.i2p.seedless.data.SetupServices;

/**
 *
 * @author sponge
 */
public class ServerInfo {

    private SetupServices services = new SetupServices();

    public ServerInfo() {
    }

    public String dump() {
        String acpt[] = services.getAccepted();
        long exp[] = services.getExpires();
        StringBuilder everything = new StringBuilder();
        for(int i = 0; i < exp.length; i++) {
            everything.append(acpt[i]).append(" ").append(exp[i]).append("\n");
        }
        return everything.toString();
    }
}
