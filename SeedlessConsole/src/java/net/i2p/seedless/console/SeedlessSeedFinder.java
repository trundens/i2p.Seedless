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

import net.i2p.seedless.data.DestMapper;
import net.i2p.seedless.reuse.scrape.Scraper;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.TunnelData;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.Server;
import net.i2p.seedless.classes.Proxy;
import net.i2p.seedless.classes.SeedlessServices;
import java.io.IOException;
import java.util.Date;
import net.i2p.I2PAppContext;
import net.i2p.seedless.classes.eep;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
//import net.i2p.i2ptunnel.web.IndexBean;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class SeedlessSeedFinder implements Runnable {

    private boolean testing = net.i2p.seedless.Version.testing;
    private int launchtime = 600; // 600
    private final int DEFAULT_PROXY = 4444;
    private volatile boolean spin = false;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private long expires = 12 * 60; // 12 hours

    /**
     * Default instance
     */
    public SeedlessSeedFinder() {
        spin = true;
        System.out.println("WARNING! SeedlessSeedFinder is using defaults:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Create default instance except proxyport
     * @param proxyport
     */
    public SeedlessSeedFinder(int proxyport) {
        spin = true;
        this.proxyport = proxyport;
        System.out.println("WARNING! SeedlessSeedFinder is using default host:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Create default instance except proxyaddress
     * @param proxyaddress
     */
    public SeedlessSeedFinder(String proxyaddress) {
        spin = true;
        this.proxyaddress = proxyaddress;
        System.out.println("WARNING! SeedlessSeedFinder is using default port:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Create instance
     * @param proxyaddress
     * @param proxyport
     */
    public SeedlessSeedFinder(String proxyaddress, int proxyport) {
        spin = true;
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        System.out.println("SeedlessSeedFinder is using discovered setting of:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Kill it.
     */
    public void die() {
        spin = false;
    }

    /**
     * This starts seedless
     */
    public void run() {
        Scraper scraper = null;
        SeedlessCache seedlessCache = null;
        TunnelMonitor tunnelMonitor = null;
        Thread scraperThread = null;
        Thread seedlessCacheThread = null;
        Thread tunnelMonitorThread = null;
        boolean oK = false;
        String dir = null;
        String dbdir = null;
        DB db = null;
        ODB odb = null;
        Finder finder = new Finder();
        try {
            Objects<Proxy> pro;
            Proxy prox;
            if(System.getProperty("SeedlessCoreRunning", "no").equals("no")) {
                // Never ran, (re)set the database.
                db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war SeelessSeedFinder:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    return;
                }
                odb = db.getODB();
                try {
                    dir = I2PAppContext.getGlobalContext().getAppDir().getCanonicalPath().toString() + java.io.File.separatorChar;
                } catch(IOException ex) {
                }
                try {
                    dbdir = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath().toString() + java.io.File.separatorChar;
                } catch(IOException ex) {
                }
                I2PTunnelWrapper indexBean = null;

                // Set "server" resource to false, this will be reset if a server is actually running,
                // then scan all configs in I2P and publish the information.

                DestMapper dm = new DestMapper();
                {
                    Server zap = new Server();
                    zap.setServer(false);
                    try {
                        Objects<Server> server = odb.query(Server.class).objects();
                        if(server.hasNext()) {
                            while(server.hasNext()) {
                                Server k = server.next();
                                odb.delete(k);
                            }
                            odb.commit();
                        // odb.deleteAll(server);
                        }
                    } catch(Exception npe) {
                        // Owch! This is a workarround... a horrible one at that...
                        System.out.println("Database corruption? Zapping the database and making a new one. If the Tunnel monitor does not start, stop i2p, delete the Seedlessdb and Seedlessdb.lg files, and start i2p.");
                        System.out.println("Database corruption can be caused by not waiting long enough on shutdown.");
                        System.out.println("Try an increase of the value in wrapper.config for the setting wrapper.jvm_exit.timeout");
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                        org.neodatis.tool.IOUtil.deleteFile(dbdir + "Seedlessdb");
                        org.neodatis.tool.IOUtil.deleteFile(dbdir + "Seedlessdb.lg");
                        try {
                            // Sleep for a bit to cause concern :-)
                            Thread.sleep(5000);
                        } catch(InterruptedException ex) {
                            return; // die
                        }

                        db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war SeedlessSeedFinder:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                        if(db == null) {
                            return;
                        }
                        odb = db.getODB();
                    }
                    odb.store(zap);
                    odb.commit();
                }
                {
                    Tunnel zap = new Tunnel();
                    zap.setState(false);
                    Objects<Tunnel> server = odb.query(Tunnel.class).objects();
                    if(server.hasNext()) {
                        while(server.hasNext()) {
                            Tunnel k = server.next();
                            odb.delete(k);
                        }
                    // odb.deleteAll(server);
                    }
                    odb.store(zap);
                    odb.commit();
                }
                {
                    Cache zap = new Cache();
                    zap.setState(false);
                    Objects<Cache> server = odb.query(Cache.class).objects();
                    if(server.hasNext()) {
                        while(server.hasNext()) {
                            Cache k = server.next();
                            odb.delete(k);
                        }
                    //odb.deleteAll(server);
                    }
                    odb.store(zap);
                    odb.commit();
                }
                {
                    eep zap = new eep();
                    zap.setOK(false);
                    zap.setb32(null);
                    Objects<eep> server = odb.query(eep.class).objects();
                    if(server.hasNext()) {
                        while(server.hasNext()) {
                            eep k = server.next();
                            odb.delete(k);
                        }
                    //odb.deleteAll(server);
                    }
                    odb.store(zap);
                    odb.commit();
                }

                {
                    finder.setState(false);
                    Objects<Finder> server = odb.query(Finder.class).objects();
                    if(server.hasNext()) {
                        while(server.hasNext()) {
                            Finder k = server.next();
                            odb.delete(k);
                        }
                    // odb.deleteAll(server);
                    }
                    odb.store(finder);
                    odb.commit();
                }

                {
                    Objects<TunnelData> tunnels = odb.query(TunnelData.class).objects();
                    if(tunnels.hasNext()) {
                        while(tunnels.hasNext()) {
                            TunnelData k = tunnels.next();
                            odb.delete(k);
                        }
                    // odb.deleteAll(tunnels);
                    }
                }
                indexBean = new I2PTunnelWrapper();

                for(int curServer = 0; curServer < indexBean.getTunnelCount(); curServer++) {
                    if(indexBean.isClient(curServer)) {
                        continue;
                    }
                    if(indexBean.getInternalType(curServer).equals("httpbidirserver")) {
                        TunnelData tunnel = new TunnelData(dm.b32(indexBean.getDestinationBase64(curServer)), indexBean.getServerTarget(curServer), indexBean.getClientInterface(curServer) + ":" + indexBean.getClientPort(curServer), (indexBean.getTunnelStatus(curServer) == I2PTunnelWrapper.RUNNING), dir + indexBean.getPrivateKeyFile(curServer));
                        odb.store(tunnel);
                        odb.commit();
                    }
                }
                // store proxy ip:port
                pro = odb.query(Proxy.class).objects();
                while(pro.hasNext()) {
                    Proxy k = pro.next();
                    odb.delete(k);
                }
                odb.commit();
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                try {
                    // Sleep for a bit...
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    return;
                }
            }
            // launch tunnel monitor thread.
            tunnelMonitor = new TunnelMonitor();
            tunnelMonitorThread = new Thread(tunnelMonitor, "Seedless.TunnelMonitor");
            tunnelMonitorThread.setDaemon(true);
            tunnelMonitorThread.start();
            System.setProperty("SeedlessCoreRunning", "true"); // Actually, TunnelMonitor, but enough is now set up to work.

            boolean waiting = true;
            System.out.println("Waiting to launch SeedlessSeedFinder.");
            {
                int loops = 0;
                while(waiting && spin) {
                    // Check if the proxy is up, when it is, continue.
                    try {
                        db = new DB(this.getClass().getName());
                    } catch(DBException ex) {
                        System.out.println("SeedlessSeedFinder could not connect to database. Exiting!");
                        return;
                    }
                    odb = db.getODB();
                    pro = odb.query(Proxy.class, W.equal("up", true)).objects();
                    if(pro.hasNext()) {
                        prox = pro.first();
                        proxyaddress = prox.IP;
                        proxyport = prox.PORT;
                        waiting = false;
                        System.out.println("Hurray! A proxy resource came up.");
                    }
                    odb = null;
                    (new AttachNeoDatis()).DetachNeoDatis(db);
                    db = null;

                    // check in 10 seconds.
                    if(waiting) {
                        try {
                            Thread.sleep(10000);
                        } catch(InterruptedException ex) {
                            return;
                        }
                        loops++;
                        if(loops > 24) {
                            // 2.5 minute warning
                            System.out.println("No proxy yet, is a http proxy running?");
                        }
                    }
                }
            }
            System.out.println("Waiting one minute for tunnel to build.");
            // Wait 1 minute
            try {
                Thread.sleep(60000);
            } catch(InterruptedException ex) {
                return;
            }


            System.out.println("Please fasten seatbelts, SeedlessSeedFinder is now launched.");
            //
            // We should now be ready to run anything else.
            //
            System.setProperty("SeedlessReady", "true");
            try {
                db = new DB(this.getClass().getName());
            } catch(DBException ex) {
                System.out.println("SeedlessSeedFinder could not connect to database. Exiting!");
                return;
            }
            odb = db.getODB();
            {
                Objects<Finder> server = odb.query(Finder.class).objects();
                finder = server.first();
                finder.running = true;
                odb.store(finder);
                odb.commit();
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
            }
            if(spin) {


                // Launch cache
                seedlessCache = new SeedlessCache(proxyaddress, proxyport);
                seedlessCacheThread = new Thread(seedlessCache, "Seedless.SeedlessCache");
                seedlessCacheThread.setDaemon(true);
                seedlessCacheThread.start();

                // Launch seedless scraper to locate seedless servers.
                scraper = new Scraper(true, "seedless", " ", expires, true, proxyaddress, proxyport);
                scraperThread = new Thread(scraper, "seedless server scraper");
                scraperThread.setDaemon(true);
                scraperThread.start();


            }
            // Monitor the scraper
            while(spin) {
                if(!scraper.isAlive()) {
                    Date now = new Date();
                    long date = now.getTime(); // in miliseconds
                    SeedlessServices service = null;
                    try {
                        db = new DB(this.getClass().getName());
                        odb = db.getODB();
                        // Preen database of any old entries... ONLY seedless ones!
                        Objects<SeedlessServices> services = odb.query(SeedlessServices.class, W.le("date", date).and(W.like("service", "seedless"))).objects();
                        while(services.hasNext()) {
                            service = services.next();
                            odb.delete(service);
                            odb.commit();
                        }
                    } catch(DBException ex) {
                        System.out.println("SeedlessSeedFinder ERROR" + ex);
                    }
                    odb = null;
                    (new AttachNeoDatis()).DetachNeoDatis(db);
                    db = null;
                    scraper.revive();
                }

                // Sleep for 1 minute and loop.
                try {
                    Thread.sleep(60 * 1000); // Check again in 1 minute.
                } catch(InterruptedException ex) {
                    return; // die
                }
            }

        } finally {
            // Perhaps I need to reverse the shutdown order here?
            System.setProperty("SeedlessReady", "false");
            System.out.println("SeedlessSeedFinder Trying to Exit.... if it does not exit within 60 seconds, please report on http://trac.i2p2.i2p/ticket/566");
            try {
                if(db == null) {
                    try {
                        db = new DB(this.getClass().getName());
                    } catch(DBException ex) {
                    }
                    odb = db.getODB();
                }
                if(db != null) {
                    Objects<Finder> server = odb.query(Finder.class).objects();
                    finder = server.first();
                    finder.running = false;
                    odb.store(finder);
                    odb.commit();
                }
            } catch(Exception ex) {
            }
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
            System.out.println("SeedlessSeedFinder Now trying to kill child threads...");
            oK = false;
            while(!oK) {
                oK = true;
                if(seedlessCacheThread != null) {
                    if(seedlessCacheThread.isAlive()) {
                        oK = false;
                        seedlessCache.die();
                        seedlessCacheThread.interrupt();
                    }
                }
                if(scraperThread != null) {
                    if(!scraperThread.isAlive()) {
                        oK = false;
                        scraper.die();
                        scraperThread.interrupt();
                    }
                }
                if(tunnelMonitorThread != null) {
                    if(!tunnelMonitorThread.isAlive()) {
                        oK = false;
                        tunnelMonitor.die();
                        tunnelMonitorThread.interrupt();
                    }
                }
                if(!oK) {
                    try {
                        Thread.sleep(1000); // Try again in 1 second.
                    } catch(InterruptedException ex) {
                    }

                }
            }
            System.out.println("SeedlessSeedFinder Exited.");
            System.setProperty("SeedlessCoreRunning", "false");
        }
    }
}

