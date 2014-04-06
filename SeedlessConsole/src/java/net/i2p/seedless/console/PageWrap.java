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
 * Wraps HttpServlets, and provides clean namespaces
 */
public class PageWrap extends javax.servlet.http.HttpServlet {

    // Check for Neodatis and Seedless, wait 5 seconds each, error out if either is not running.
    protected void checklibs() throws javax.servlet.ServletException {
        boolean goahead = false;
        int lcount = 0;
        while(lcount < 5) {
            String chk = System.getProperty("NeoDatisRunning", "false");
            if(chk.equalsIgnoreCase("true")) {
                goahead = true;
                break;
            }
            lcount++;
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                break;
            }
        }
        if(!goahead) {
            System.out.println("ERROR: NeoDatis is not running!");
            throw new javax.servlet.ServletException("ERROR: NeoDatis is not running!");
        }

        goahead = false;
        lcount = 0;
        while(lcount < 5) {
            String chk = System.getProperty("SeedlessCoreRunning", "false");
            if(chk.equalsIgnoreCase("true")) {
                goahead = true;
                break;
            }
            lcount++;
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                break;
            }
        }

        if(!goahead) {
            System.out.println("ERROR: SeedlessCore is not running!");
            throw new javax.servlet.ServletException("ERROR: SeedlessCore is not running!");
        }
    }
}
