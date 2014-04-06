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

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import net.i2p.I2PAppContext;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;
import net.i2p.seedless.data.DestMapper;
import net.i2p.seedless.reuse.scrape.Scraper;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.TunnelData;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.Proxy;
import net.i2p.seedless.classes.ScrapeCheck;
import net.i2p.seedless.reuse.AttachNeoDatis;

/**
 *
 * @author sponge
 */
public class TunnelMonitor implements Runnable {

    private boolean testing = net.i2p.seedless.Version.testing;
    private volatile boolean spin = false;
    private LinkedList<Scraper> scrapers = new LinkedList<Scraper>();
    private String proxyaddress = "127.0.0.1";
    private int proxyport = 4444;

    public TunnelMonitor() {
        spin = true;
    }

    public void run() {
        System.out.println("TunnelMonitor launching.");
        int curServer;
        String dest;
        String ip;
        int port;
        boolean oK = true;
        String dir = null;
        Iterator<Scraper> scrapeList;
        Scraper scrape;
        LinkedList<Scraper> snap;
        ScrapeCheck nuke;
        DB db = null;
        ODB odb = null;
        Tunnel sometunnel;
        try {
            while(System.getProperty("SeedlessCoreRunning", "false").equals("false") && spin) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    return; // Die!!
                }

            }
            if(!spin) {
                return; // Die!!
            }
            try {
                dir = I2PAppContext.getGlobalContext().getAppDir().getCanonicalPath().toString() + java.io.File.separatorChar;
            } catch(IOException ex) {
            }

            // We're ready to monitor httpclient and httpbidir tunnels
            System.out.println("TunnelMonitor launched.");
            try {
                db = new DB(this.getClass().getName());
            } catch(DBException ex) {
                System.out.println("TunnelMonitor could not connect to database. Exiting @ "  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                return;
            }
            odb = db.getODB();
            Objects<Tunnel> server = odb.query(Tunnel.class).objects();
            sometunnel = server.first();
            sometunnel.running = true;
            odb.store(sometunnel);
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;

            // This is the longest loop in seedless to hold open an odb instance.
            while(spin) {
                db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war TunnelMonitor:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    return;
                }
                odb = db.getODB();
                I2PTunnelWrapper allI2PTunnels = null;

                // Scan all configs in I2P and publish the information.
                try {
                    Proxy prox = null;
                    Objects<Proxy> proxies;
                    TunnelData tunnel = null;
                    Objects<TunnelData> tunnels;
                    DestMapper dm = new DestMapper();

                    allI2PTunnels = new I2PTunnelWrapper();

                    for(curServer = 0; curServer < allI2PTunnels.getTunnelCount(); curServer++) {
                        String foo = allI2PTunnels.getDestinationBase64(curServer);
                        if(foo.equals("")) {
                            // debugging for user/kytv
                            if(false) {
                                System.out.println("httpbidirserver tunnel#" + curServer + "b64 is null, skipping.");
                            }
                        } else {
                            if(allI2PTunnels.isClient(curServer)) {
                                if(allI2PTunnels.getInternalType(curServer).equals("httpclient")) {
                                    ip = allI2PTunnels.getClientInterface(curServer);
                                    port = Integer.decode(allI2PTunnels.getClientPort(curServer));
                                    int bstat = allI2PTunnels.getTunnelStatus(curServer);
                                    boolean testup = (bstat == I2PTunnelWrapper.RUNNING) || (bstat == I2PTunnelWrapper.STANDBY);
                                    proxies = odb.query(Proxy.class, W.equal("IP", ip).and(W.equal("PORT", port))).objects();
                                    if(proxies.isEmpty()) {
                                        // Add new tunnel
                                        System.out.println("Proxy discovered at " + ip + ":" + port);
                                        prox = new Proxy();
                                        prox.IP = ip;
                                        prox.PORT = port;
                                        prox.up = testup;
                                        odb.store(prox);
                                        odb.commit();
                                    } else {
                                        // Do we need to update?
                                        prox = proxies.first();
                                        boolean testfail = !((testup == prox.up));
                                        if(testfail) {
                                            // Update tunnel information.
                                            prox.up = testup;
                                            odb.store(prox);
                                            odb.commit();
                                        }
                                    }
                                }
                                continue;
                            } else if(allI2PTunnels.getInternalType(curServer).equals("httpbidirserver")) {
                                dest = dm.b32(allI2PTunnels.getDestinationBase64(curServer));
                                boolean testup = (allI2PTunnels.getTunnelStatus(curServer) == I2PTunnelWrapper.RUNNING);
                                ip = allI2PTunnels.getClientInterface(curServer);
                                port = Integer.decode(allI2PTunnels.getClientPort(curServer));
                                tunnels = odb.query(TunnelData.class, W.equal("base32", dest)).objects();
                                proxies = odb.query(Proxy.class, W.equal("IP", ip).and(W.equal("PORT", port))).objects();
                                // copied from above...
                                if(proxies.isEmpty()) {
                                    // Add new tunnel
                                    System.out.println("Proxy discovered at " + ip + ":" + port);
                                    prox = new Proxy();
                                    prox.IP = ip;
                                    prox.PORT = port;
                                    prox.up = testup;
                                    odb.store(prox);
                                    odb.commit();
                                } else {
                                    // Do we need to update?
                                    prox = proxies.first();
                                    boolean testfail = !((testup == prox.up));
                                    if(testfail) {
                                        // Update tunnel information.
                                        prox.up = testup;
                                        odb.store(prox);
                                        odb.commit();
                                    }
                                }
                                if(tunnels.isEmpty()) {
                                    // Add new tunnel
                                    tunnel = new TunnelData(dest, allI2PTunnels.getServerTarget(curServer), allI2PTunnels.getClientInterface(curServer) + ":" + allI2PTunnels.getClientPort(curServer), (allI2PTunnels.getTunnelStatus(curServer) == I2PTunnelWrapper.RUNNING), dir + java.io.File.separatorChar + allI2PTunnels.getPrivateKeyFile(curServer));
                                    odb.store(tunnel);
                                    odb.commit();
                                } else {
                                    // Do we need to update?
                                    tunnel = tunnels.first();
                                    String testServer = allI2PTunnels.getServerTarget(curServer);
                                    String testClient = allI2PTunnels.getClientInterface(curServer) + ":" + allI2PTunnels.getClientPort(curServer);
                                    boolean testfail = !((testup == tunnel.up) && (testServer.equals(tunnel.inFromI2P)) && (testClient.equals(tunnel.outToI2P)));
                                    if(testfail) {
                                        // Update tunnel information.
                                        tunnel.up = testup;
                                        tunnel.inFromI2P = testServer;
                                        tunnel.outToI2P = testClient;
                                        odb.store(tunnel);
                                        odb.commit();
                                    }
                                }
                            }
                        }
                    }
                    // Check for removed servers
                    tunnels = odb.query(TunnelData.class).objects();
                    while(tunnels.hasNext()) {
                        oK = false;
                        tunnel = tunnels.next();
                        String testB32 = tunnel.base32;
                        for(curServer = 0; curServer < allI2PTunnels.getTunnelCount(); curServer++) {
                            String foo = allI2PTunnels.getDestinationBase64(curServer);
                            if(!foo.equals("")) {
                                dest = dm.b32(allI2PTunnels.getDestinationBase64(curServer));
                                if(dest.equals(testB32)) {
                                    oK = true;
                                }
                            }
                        }
                        if(!oK) {
                            odb.delete(tunnel);
                            odb.commit();
                        }
                    }
                    // Check for removed http proxies
                    proxies = odb.query(Proxy.class).objects();
                    while(proxies.hasNext()) {
                        oK = false;
                        prox = proxies.next();
                        port = prox.PORT;
                        ip = prox.IP;
                        for(curServer = 0; curServer < allI2PTunnels.getTunnelCount(); curServer++) {
                            String foo = allI2PTunnels.getDestinationBase64(curServer);
                            if(!foo.equals("")) {
                                ip = allI2PTunnels.getClientInterface(curServer);
                                port = Integer.decode(allI2PTunnels.getClientPort(curServer));
                                if(port == prox.PORT && ip.equals(prox.IP)) {
                                    oK = true;
                                }
                            }
                        }
                        if(!oK) {
                            odb.delete(prox);
                            odb.commit();
                        }
                    }
                } catch(Exception ex) {
                }


                {
                    // Kill all old scrape requests.
                    Date now = new Date();
                    long date = now.getTime();
                    Objects<ScrapeCheck> kill = odb.query(ScrapeCheck.class, W.le("time", date)).objects();

                    while(kill.hasNext()) {
                        nuke = kill.next();
                        boolean crush = true;
                        scrapeList = scrapers.iterator();
                        while(scrapeList.hasNext()) {
                            scrape = scrapeList.next();
                            if(nuke.id.equals(scrape.myID())) {
                                crush = false;
                            }
                        }
                        if(crush) {
                            odb.delete(nuke);
                            odb.commit();
                        }
                    }

                    Objects<ScrapeCheck> scraping;
                    snap = new LinkedList<Scraper>();
                    snap.addAll(scrapers);
                    scrapeList = snap.iterator();
                    while(scrapeList.hasNext()) {
                        scrape = scrapeList.next();
                        String ID = scrape.myID();
                        ScrapeCheck me = null;
                        scraping = odb.query(ScrapeCheck.class, W.equal("id", ID)).objects();
                        if(scraping.hasNext()) {
                            me = scraping.next();
                        }
                        if(!scrape.isAlive()) {
                            if(testing) {
                                System.out.println("ID:" + ID + " is done.");
                            }
                            scrape.die();
                            scrape.revive();
                            if(me != null) {
                                me.looking = false;
                                odb.store(me);
                                odb.commit();
                            }
                            scrapers.remove(scrape);
                        } else if(me != null) {

                            me.time = date + (10 * 60000l);
                            odb.store(me);
                            odb.commit();
                        }
                    }
                    snap = null;
                    // check for any scrape requests
                    // Do this only when things are ready!
                    scraping = odb.query(ScrapeCheck.class, W.equal("started", false)).objects();
                    // get a working proxy to use
                    Objects<Proxy> proxies = odb.query(Proxy.class, W.equal("up", true)).objects();
                    if(proxies.hasNext() && scraping.hasNext()) {
                        Proxy prox = proxies.first();
                        proxyaddress = prox.IP;
                        proxyport = prox.PORT;
                        while(scraping.hasNext()) {
                            // launch a scraper
                            ScrapeCheck me = scraping.next();
                            //  proxy address/port is dynamic
                            scrape = new Scraper(true, me.service, me.find, 12 * 60, proxyaddress, proxyport, me.id);
                            Thread s = new Thread(scrape, "monitor_scrape");
                            s.setDaemon(true);
                            s.start();
                            scrapers.add(scrape);
                            me.started = true;
                            odb.store(me);
                            odb.commit();
                            if(testing) {
                                System.out.println("ID:" + me.id + " Started.");
                            }
                        }
                    }
                }
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                try {
                    // *whew* now sleep for 5 seconds
                    Thread.sleep(1000 * 5);
                } catch(InterruptedException ex) {
                }

            }
        } finally {
            try {
                if(db == null) {
                    try {
                        db = new DB(this.getClass().getName());
                    } catch(DBException ex) {
                    }
                    odb = db.getODB();
                }

                if(db != null) {
                    Objects<Tunnel> server = odb.query(Tunnel.class).objects();
                    sometunnel = server.first();
                    sometunnel.running = false;
                    odb.store(sometunnel);
                    odb.commit();
                    odb = null;
                }

            } catch(Exception ex) {
            }
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
            System.out.println("TunnelMonitor Exited.");
        }

    }

    void die() {
        spin = false;
    }
}
