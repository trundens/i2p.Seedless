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
package net.i2p.seedlessServer;

import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.server.SeedlessServerAnnouncer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import net.i2p.seedless.classes.TunnelData;
import net.i2p.seedless.reuse.SeedlessContextListenerCore;

/**
 * Web application lifecycle listener.
 * @author sponge
 */
public class SeedlessServerContextListener implements ServletContextListener {

    private Thread t = null;
    private SeedlessServerAnnouncer pop = null;
    private spinner spin = null;

    public void contextInitialized(ServletContextEvent sce) {
        String me = null;
        if(sce.getServletContext().getInitParameter("i2p.b32") != null) {
            sce.getServletContext().setAttribute("i2p.b32", sce.getServletContext().getInitParameter("i2p.b32").toString());
        }
        if(sce.getServletContext().getAttribute("i2p.b32") != null) {
            me = sce.getServletContext().getAttribute("i2p.b32").toString();
        }
        if(me == null) {
            System.out.println("Seedless.war ERROR: i2p.b32 is not set, check jetty config.");
            return;
        }

        spin = new spinner(me);
        t = new Thread(spin, "Seedless.spinner");
        t.start();


    }

    private class spinner implements Runnable {

        private boolean spinit = true;
        private String me;

        public spinner(String mee) {
            me = mee;
        }

        public void die() {
            spinit = false;
        }

        public void run() {
            boolean goahead = false;
            while(spinit) {
                String chk = System.getProperty("SeedlessReady", "false");
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

            if(goahead) {
                int loops = 0;
                while(spinit) {
                    try {
                        loops++;
                        TunnelData tunnel = (new SeedlessContextListenerCore()).testB32NeoDatis(me, "Seedless.war");
                        if(tunnel != null) {
                            pop = new SeedlessServerAnnouncer(tunnel.outToI2P, net.i2p.seedlessServer.Version.VERSION, me);
                            t = new Thread(pop, "Seedless.ServerAnnouncer");
                            t.setDaemon(true);
                            t.start();
                            net.i2p.seedlessServer.Version.main(null);
                            System.out.println("Seedless.war: context initialized for " + me);
                            break;
                        } else {
                            try {
                                Thread.sleep(1000);
                                if(loops == 600) {
                                    // 10 minutes, warn!
                                    System.out.println("Seedless.war ERROR: eepsite tunnel" + me + " is not found yet! Does it exist?");
                                    loops = 0;
                                }
                            } catch(InterruptedException ex) {
                                break;
                            }
                        }
                    } catch(DBException ex) {
                        // ignore, for now :-)
                    }
                }
            }
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        if(t != null) {
            if(spin != null) {
                spin.die();
            }
            if(pop != null) {
                pop.die();
                try {
                    Thread.sleep(200);
                } catch(InterruptedException ex) {
                }
                (new SeedlessContextListenerCore()).zap(t, "Seedless.war");
            }
        }
    }
}
