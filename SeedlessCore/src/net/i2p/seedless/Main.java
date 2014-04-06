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
package net.i2p.seedless;

/**
 *
 * @author sponge
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException {
        // check for Neodatis
        boolean goahead = false;
        int lcount = 0;
        while(lcount < 15) {
            String chk = System.getProperty("NeoDatisLoaded", "false");
            if(chk.equalsIgnoreCase("true")) {
                goahead = true;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                break;
            }
        }
        if(!goahead) {
            throw new ClassNotFoundException("Can't find NeoDatis Plugin");
        }
            System.out.println("SeedlessCore version: " + net.i2p.seedless.Version.getVersion());
    }
}
