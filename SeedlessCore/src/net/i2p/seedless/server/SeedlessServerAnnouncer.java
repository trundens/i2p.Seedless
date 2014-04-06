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
package net.i2p.seedless.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import net.i2p.seedless.reuse.Announcer;
import net.i2p.seedless.data.SetupServer;
import net.i2p.seedless.reuse.scrape.Scraper;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.Server;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import net.i2p.I2PAppContext;
import net.i2p.seedless.classes.SeedlessServices;
import net.i2p.seedless.classes.TorrentSearch;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class SeedlessServerAnnouncer implements Runnable {

    private boolean testing = net.i2p.seedless.Version.testing;
    private SetupServer config = null;
    private String accepted[];
    private long expires[]; // in minutes
    private int delaylaunch = 60; // wait 60 seconds to update the server field.
    private final int DEFAULT_PROXY = 4488;
    private volatile boolean spin;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private LinkedList<Scraper> scrapers = new LinkedList<Scraper>();
    private Thread announcerThread = null;
    //private Random generator = new Random();
    private String VERSION = null;
    private int major = 0;
    private int minor = 0;
    private int release = 0;
    private String base32 = null;
    private final static String CONFIG_FILE = "SeedlessServer.Version";

    public SeedlessServerAnnouncer() {
        System.out.println("ERROR: Seedless.war too old.");
    }

    public SeedlessServerAnnouncer(String me) {
        System.out.println("ERROR: Seedless.war too old.");
    }

    public SeedlessServerAnnouncer(String me, String V) {
        System.out.println("ERROR: Seedless.war too old.");
    }

    /**
     * Create default instance
     * @param me proxy address:port
     * @parm V version
     * @parm b32 base32 address
     *
     */
    public SeedlessServerAnnouncer(String me, String V, String b32) {
        spin = true;
        proxyaddress = me.split(":")[0];
        proxyport = Integer.parseInt(me.split(":")[1]);
        VERSION = V;
        base32 = b32;
        major = Integer.parseInt(VERSION.split("\\.")[0], 16);
        minor = Integer.parseInt(VERSION.split("\\.")[1], 16);
        release = Integer.parseInt(VERSION.split("\\.")[2], 16);
    }

    /**
     * Kill it.
     */
    public void die() {
        spin = false;
    }

    /**
     * Verify config for server.
     */
    private void VerifyConfig() {
        boolean didsomething = false;
        int lastmajor = -1;
        int lastminor = -1;
        int lastrelease = -1;
        BufferedReader br = null;
        File cfg = null;
        // Check if we ever verified before, if not set need.
        String sep = java.io.File.separator;
        String cd = "." + sep;
        try {
            cd = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + sep;
            cfg = new File(cd + CONFIG_FILE);
            FileInputStream fi = new FileInputStream(cfg);
            br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
            String line = br.readLine().trim();
            br.close();
            lastmajor = Integer.parseInt(line.split("\\.")[0], 16);
            lastminor = Integer.parseInt(line.split("\\.")[1], 16);
            lastrelease = Integer.parseInt(line.split("\\.")[2], 16);
        } catch(FileNotFoundException fnfe) {
        } catch(IOException ex) {
        }

        SetupServer cacheconfig = new SetupServer();
        if(cacheconfig.getAccepted().length == 1 && lastmajor < 0 && lastminor < 0 && lastrelease < 0) {
            // Add in all default services, because the end-user did not do that.
            cacheconfig.addEntry("seedless", 720);
            cacheconfig.addEntry("eepsite", 720);
            cacheconfig.addEntry("torrent", 240);
            cacheconfig.addEntry("i2p-bote", 360);
            cacheconfig.addEntry("i2p-messenger", 1400);
            cacheconfig.addEntry("tahoe", 720);
            System.out.println("INFO: Seedless.war: Since you skipped a step, I set up the cache for you.");
        }

        // FUTURE: If version is below, add new official services, and mark didsomething
        /*
        if(lastmajor < 1) {
        didsomething = true;
        }
        if(lastminor < 2) {
        didsomething = true;
        }
        if(lastrelease < ...) {
        didsomething = true;
        }
         *
         */
        // Save new version number
        if(lastmajor != major || lastminor != minor || lastrelease != release) {
            try {
                try {
                    PrintStream fo = new PrintStream(cfg, "UTF-8");
                    fo.println(VERSION);
                    fo.close();
                } catch(FileNotFoundException ex) {
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     *
     */
    public void run() {
        if(!spin) {
            return;
        }
        int i;
        String weDo = "";
        DB db = null;
        ODB odb = null;
        Iterator<Scraper> scrapeList;
        Scraper scrape = null;
        boolean oK;
        Thread s;
        Announcer announcer = null;
        boolean finder = false;
        boolean monitor = false;
        boolean cache = false;
        boolean ready = false;
        Thread indexer = null;
        boolean didsomething = false;
        boolean report = true;
        boolean doreports = false;
        int checktime = 60 * 1000;
        int lastindex = 0;

        /* Before launching, check if this is the first time ever.
         * If it is, push in all services as the defaults if the user never did it.
         * If there is a new official service, add it.
         * This is to help operatiors keep up to date, and help the community have resources.
         */
        VerifyConfig();
        try {
            System.out.println("SeedlessServer waiting " + delaylaunch + " seconds for resources.");
            try {
                Thread.sleep(1000 * delaylaunch);
            } catch(InterruptedException ex) {
                return;
            }
            // Wait for services to be up.
            System.out.println("SeedlessServer waiting for resources to startup.");
            while(!ready) {
                db = (new AttachNeoDatis()).AttachNeodatis("Seedless.war SeedlessServerAnnouncer:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    return;
                }
                odb = db.getODB();
                try {
                    Objects<Finder> finderserver = odb.query(Finder.class).objects();
                    if(finderserver.hasNext()) {
                        Finder zap = finderserver.first();
                        finder = zap.isRunning();
                    }
                    Objects<Tunnel> monitorserver = odb.query(Tunnel.class).objects();
                    if(monitorserver.hasNext()) {
                        Tunnel zap = monitorserver.first();
                        monitor = zap.isRunning();
                    }
                    Objects<Cache> cacheserver = odb.query(Cache.class).objects();
                    if(cacheserver.hasNext()) {
                        Cache zap = cacheserver.first();
                        cache = zap.isRunning();
                    }
                    ready = finder & monitor & cache;
                } catch(Exception ex) {
                    // don't care...
                } finally {
                    odb = null;
                    (new AttachNeoDatis()).DetachNeoDatis(db);
                    db = null;
                }
                if(!ready) {
                    try {
                        Thread.sleep(6000);
                    } catch(InterruptedException ex1) {
                        return; // Die!
                    }
                }
            }

            System.out.println("SeedlessServer: resources are ready.");
            // Set "server" resource to true.
            db = (new AttachNeoDatis()).AttachNeodatis("Seedless.war SeedlessServerAnnouncer" + Thread.currentThread().getStackTrace()[1].getLineNumber());
            if(db == null) {
                return;
            }
            odb = db.getODB();
            try {
                Server zap = new Server();
                Objects<Server> server = odb.query(Server.class).objects();
                if(server.hasNext()) {
                    zap = server.first();
                }
                zap.server = true;
                zap.VERSION = VERSION;
                zap.b32 = base32;
                odb.store(zap);
            } catch(Exception ex) {
                // don't care...
            } finally {
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
            }

            // check if proxy is up?
            System.out.println("Please fasten seatbelts, SeedlessServer is now launched.");
            config = new SetupServer();
            accepted = config.getAccepted();
            expires = config.getExpires();

            weDo = "";
            for(i = 0; i < accepted.length; i++) {
                weDo = weDo + ",";
                weDo = weDo + accepted[i];
            }
            weDo = weDo.substring(1);
            if(testing) {
                System.out.println("SeedlessServer config set to " + weDo);
            }

            // Launch the Seedless announcerThread.
            announcer = new Announcer("seedless", weDo, true, proxyaddress, proxyport);
            announcerThread = new Thread(announcer, "Seedless.ServerAnnouncer");
            announcerThread.start();
            // Next, we need to launch multiple scrapers, and monitor them.
            // We do that in the loop below.
            boolean change;

            while(spin) {
                didsomething = false;
                change = false;
                config = new SetupServer();
                accepted = config.getAccepted();
                expires = config.getExpires();

                // Anything dead?
                scrapeList = scrapers.iterator();
                while(scrapeList.hasNext()) {
                    scrape = scrapeList.next();
                    if(!scrape.isAlive()) {
                        oK = false;
                        // Thread is done, Type still in our config?
                        for(i = 0; i < accepted.length; i++) {
                            if(scrape.kind().equals(accepted[i])) {
                                oK = true;
                            }
                        }
                        if(oK) {
                            scrape.revive(); // Start the thread again.
                        } else {
                            scrape.die();
                            scrape.revive();
                            if(testing) {
                                System.out.println("Deleting " + scrape.kind() + ".");
                            }
                            scrapers.remove(scrape); // Remove this from the list.
                            change = true;
                        }
                    }
                }

                // anything *NEW*
                for(i = 0; i < accepted.length; i++) {
                    scrapeList = scrapers.iterator();
                    oK = false;
                    while(scrapeList.hasNext()) {
                        scrape = scrapeList.next();
                        if(scrape.kind().equals(accepted[i])) {
                            oK = true;
                        }
                    }
                    if(!oK && !accepted[i].equals("seedless")) {
                        // Start new thread!
                        // Only ran ONCE, and will die off on thier own.
                        if(testing) {
                            System.out.println("NEW Scrape for " + accepted[i] + " starting.");
                        }
                        scrape = new Scraper(true, accepted[i], " ", expires[i], proxyaddress, proxyport);
                        s = new Thread(scrape, "Seedless.Scraper-" + accepted[i]);
                        s.setDaemon(true);
                        s.start();
                        scrapers.add(scrape);
                        change = true;
                    }
                }
                if(change) {
                    didsomething = true;
                    weDo = "";
                    for(i = 0; i < accepted.length; i++) {
                        weDo = weDo + ",";
                        weDo = weDo + accepted[i];
                    }
                    weDo = weDo.substring(1);
                    announcer.changeMeta(weDo);

                    if(testing) {
                        System.out.println("SeedlessServer changed config to " + weDo);
                    }

                }
                // Check for any new torrent hashes

                if(announcer.getMeta().contains("torrent")) {
                    //System.out.println("Collecting new torrents.");
                    String request = null;
                    try {
                        db = new DB(this.getClass().getName());
                        odb = db.getODB();
                        SortedSet<String> got = new TreeSet();
                        SortedSet<String> want = new TreeSet();
                        Objects<TorrentSearch> ts = odb.query(TorrentSearch.class, W.isNotNull("maggot")).objects();
                        while(ts.hasNext()) {
                            TorrentSearch x = ts.next();
                            //System.out.println("got " + x.maggot.subSequence(9, 49).toString());
                            got.add(x.maggot.subSequence(9, 49).toString());
                        }
                        Objects<SeedlessServices> services = odb.query(SeedlessServices.class, W.equal("service", "torrent")).objects();
                        while(services.hasNext()) {
                            SeedlessServices ss = services.next();
                            String poop = ss.metadata.split("\n")[0];
                            if((!got.contains(poop)) && (!want.contains(poop))) {
                                //System.out.println("want Adding " + poop);
                                want.add(poop);
                            }
                        }
                        // Add all new
                        while(!want.isEmpty()) {
                            String m = want.first();
                            request = "maggot://" + m + ":????????????????????????????????????????";
                            //System.out.println("adding new search " + request);
                            TorrentSearch nts = new TorrentSearch();
                            nts.maggot = request;
                            odb.store(nts);
                            want.remove(m);
                            didsomething = true;
                        }

                        if(indexer != null) {
                            if(!indexer.isAlive()) {
                                //System.out.println("indexer finished.");
                                didsomething = true;
                                indexer = null;
                            }
                        }
                        if(indexer == null) {
                            // Start a search for anything that is not 200(resolved) 403(not shared) or has had 10 attempts
                            ts = odb.query(TorrentSearch.class, W.not(W.equal("code", 200)).and(W.not(W.equal("code", 403))).and(W.not(W.equal("code", 9)))).objects();
                            if(ts.hasNext()) {
                                report = true;
                                // randomly pick one...
                                int m = ts.size();
                                if(m >= lastindex) {
                                    lastindex = 0;
                                }
                                if(doreports || testing) {
                                    if(m == 1) {
                                        System.out.println("There is 1 torrent left to index.");
                                    } else {
                                        System.out.println("There are " + m + " torrents left to index.");
                                    }
                                }
                                //int q = generator.nextInt(m);
                                int q = lastindex;
                                lastindex += 1;
                                while(q > 0) {
                                    q--;
                                    ts.next();
                                }
                                request = ts.next().maggot;
                                //System.out.println("Starting new indexer: " + request + " " + proxyaddress + ":" + proxyport);

                                TorrentIndexer ti = new TorrentIndexer(proxyaddress, proxyport, request);
                                indexer = new Thread(ti);
                                indexer.start();
                                didsomething = true;
                            } else if(report && (doreports || testing)) {
                                report = false;
                                System.out.println("There are no torrents left to index.");
                            }
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                    }
                //} else {
                //System.out.println("NOT collecting new torrents, getmeta" + announcer.getMeta() + " check is " + announcer.getMeta().contains("torrent"));
                }

                if(checktime < 60 * 1000) {
                    checktime *= 2;
                    if(checktime > 60 * 1000) {
                        checktime = 60 * 1000;
                    }
                }
                if(didsomething) {
                    checktime = 1000;
                }

                // Idle for a bit, depending on how fast things happen.
                try {
                    Thread.sleep(checktime);
                } catch(InterruptedException ex) {
                    return;
                }
            }

        } finally {
            try {
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;

                try {
                    db = new DB(this.getClass().getName());
                } catch(DBException ex) {
                }
                odb = db.getODB();
                if(db != null) {
                    try {
                        Server zap = new Server();
                        Objects<Server> server = odb.query(Server.class).objects();
                        if(server.hasNext()) {
                            zap = server.first();
                        }
                        zap.server = true;
                        zap.b32 = "NOT RUNNING";
                        odb.store(zap);
                    } catch(Exception ex) {
                        // don't care...
                    } finally {
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                    }
                }
            } catch(Exception ex) {
            }
            odb = null;
            oK = false;
            while(!oK) {
                oK = true;
                scrapeList = scrapers.iterator();
                while(scrapeList.hasNext()) {
                    scrape = scrapeList.next();
                    s = scrape.myThread();
                    if(!s.isAlive()) {
                        oK = false;
                        scrape.die();
                        s.interrupt();
                    }
                }
                if(announcerThread != null && announcerThread.isAlive()) {
                    announcer.die();
                    announcerThread.interrupt();
                    oK = false;
                }
                if(indexer != null && indexer.isAlive()) {
                    indexer.interrupt();
                    oK = false;
                }
                if(!oK) {
                    try {
                        Thread.sleep(100); // Try again in .1 seconds.
                    } catch(InterruptedException ex) {
                    }

                }
            }
            System.out.println("SeedlessServer Exited.");
        }
    }
}
