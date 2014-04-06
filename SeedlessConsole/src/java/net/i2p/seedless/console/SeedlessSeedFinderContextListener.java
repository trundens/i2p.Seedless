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
package net.i2p.seedless.console;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Web application lifecycle listener.
 * @author sponge
 */
public class SeedlessSeedFinderContextListener implements ServletContextListener {

    private Thread t = null;
    private SeedlessSeedFinder ssf = null;
    public static boolean isOk = false;
    public static boolean isOk2 = false;

    public void contextInitialized(ServletContextEvent sce) {
        int lcount = 0;
        while(lcount < 15) {
            String chk = System.getProperty("SeedlessCoreLoaded", "false");
            if(chk.equalsIgnoreCase("true")) {
                isOk = true;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                break;
            }

        }
        if(!isOk) {
            System.out.println("ERROR: Can't locate SeedlessCore!");
        }

        while(lcount < 16) {
            String chk = System.getProperty("NeoDatisRunning", "false");
            if(chk.equalsIgnoreCase("true")) {
                isOk2 = true;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                break;
            }
        }
        if(!isOk2) {
            System.out.println("ERROR: NeoDatis is not running!");
        }

        I2PTunnelWrapper I2P_Tunnel;
        if(isOk && isOk2) {
            String host = null;
            String port = null;
            I2P_Tunnel = new I2PTunnelWrapper();

            // locate the proxy.
            for(int curServer = 0; curServer < I2P_Tunnel.getTunnelCount(); curServer++) {
                if(!I2P_Tunnel.isClient(curServer)) {
                    continue;
                }
                if(I2P_Tunnel.getInternalType(curServer).equals("httpclient")) {
                    if(I2P_Tunnel.startAutomatically(curServer)) {
                        host = I2P_Tunnel.getClientInterface(curServer);
                        port = I2P_Tunnel.getClientPort(curServer);
                        break; // break loop on first available... not the best, but...
                    }
                }
            }
            if(host != null) {
                if(host.length() == 0) {
                    host = null;
                }
            }
            if(port != null) {
                if(port.length() == 0) {
                    port = null;
                }
            }
            if(host == null && port == null) {
                // could be no eepproxy, so try for a httpbidirserver
                for(int curServer = 0; curServer < I2P_Tunnel.getTunnelCount(); curServer++) {
                    if(!I2P_Tunnel.isClient(curServer)) {
                        continue;
                    }
                    if(I2P_Tunnel.getInternalType(curServer).equals("httpbidirserver")) {
                        if(I2P_Tunnel.startAutomatically(curServer)) {
                            host = I2P_Tunnel.getClientInterface(curServer);
                            port = I2P_Tunnel.getClientPort(curServer);
                            break; // break loop on first available... not the best, but...
                        }
                    }
                }
            }
            if(host != null) {
                if(host.length() == 0) {
                    host = null;
                }
            }
            if(port != null) {
                if(port.length() == 0) {
                    port = null;
                }
            }
            if(host == null && port == null) {
                ssf = new SeedlessSeedFinder();
            }
            if(host == null && port != null) {
                ssf = new SeedlessSeedFinder(Integer.parseInt(port));
            } else if(host != null && port == null) {
                ssf = new SeedlessSeedFinder(host);
            } else if(host != null && port != null) {
                ssf = new SeedlessSeedFinder(host, Integer.parseInt(port));
            }
            t = new Thread(ssf, "SeedlessSeedFinder");
            t.setDaemon(true);
            t.start();
            System.out.println("SeedlessSeedFinder server context initialized");
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        int loops = 0;
        if(t != null) {
            ssf.die();
            try {
                Thread.sleep(200);
            } catch(InterruptedException ex) {
            }
            while(t.isAlive() && loops < 60) {
                t.interrupt();
                try {
                    t.join(1000);
                } catch(InterruptedException ex) {
                    break;
                }
                loops++;
            }
        }
        if(loops < 60) {
            System.out.println("SeedlessSeedFinder server context destroyed");
        } else {
            System.out.println("SeedlessSeedFinder server context could not stop threads.");
        }
    }
}