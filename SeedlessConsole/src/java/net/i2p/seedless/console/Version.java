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
package net.i2p.seedless.console;

/**
 *
 * @author sponge
 */
public class Version {

    private final static String VERSION = "00.01.0C";

    public static void main(String args[]) {
        System.out.println("SeedlessConsole version: " + VERSION);
        System.out.println("SeedlessCore version: " + net.i2p.seedless.Version.getVersion());
        System.out.println("Neodatis ODB version: " + net.i2p.seedless.Version.getNEODATIS_SERVERver());

    }

    static String getVersion() {
        return VERSION;
    }
}
