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

import net.i2p.seedless.data.SetupServices;
import net.i2p.seedless.reuse.scrape.Scraper;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Cache;
import java.util.Iterator;
import java.util.LinkedList;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;

/**
 *
 * @author sponge
 */
public class SeedlessCache implements Runnable {

    private boolean testing = net.i2p.seedless.Version.testing;
    private SetupServices config = null;
    private String accepted[];
    private long expires[]; // in minutes
    private final int DEFAULT_PROXY = 4444;
    private volatile boolean spin;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private LinkedList<Scraper> scrapers = new LinkedList<Scraper>();

    /**
     * Default instance
     */
    public SeedlessCache() {
        spin = true;
        System.out.println("WARNING! SeedlessCache is using defaults:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Create default instance except proxyport
     * @param proxyport
     */
    public SeedlessCache(int proxyport) {
        spin = true;
        this.proxyport = proxyport;
        System.out.println("WARNING! SeedlessCache is using default host:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Create default instance except proxyaddress
     * @param proxyaddress
     */
    public SeedlessCache(String proxyaddress) {
        spin = true;
        this.proxyaddress = proxyaddress;
        System.out.println("WARNING! SeedlessCache is using default port:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Create instance
     * @param proxyaddress
     * @param proxyport
     */
    public SeedlessCache(String proxyaddress, int proxyport) {
        spin = true;
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        System.out.println("SeedlessCache is using discovered setting of:" + proxyaddress + ":" + proxyport);
    }

    /**
     * Kill it.
     */
    public void die() {
        spin = false;
    }

    /**
     *
     */
    public void run() {
        int i;
        String weDo = "";

        Iterator<Scraper> scrapeList;
        Scraper scrape = null;
        boolean oK;
        Thread s;
        DB db = null;
        ODB odb = null;
        Cache cache;
        // check if proxy is up?

        try {
            System.out.println("Please fasten seatbelts, SeedlessCache is now launched.");
            try {
                db = new DB(this.getClass().getName());
            } catch(DBException ex) {
                System.out.println("SeedlessCache could not connect to database. Exiting!");
                return;
            }
            odb = db.getODB();
            Objects<Cache> server = odb.query(Cache.class).objects();
            cache = server.first();
            cache.running = true;
            odb.store(cache);
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;

            config = new SetupServices();
            accepted = config.getAccepted();
            expires = config.getExpires();


            weDo = "";
            for(i = 0; i < accepted.length; i++) {
                weDo = weDo + ",";
                weDo = weDo + accepted[i];
            }
            weDo = weDo.substring(1);
            if(testing) {
                System.out.println("SeedlessCache config set to " + weDo);
            }


            while(spin) {
                config = new SetupServices();
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
                        s = new Thread(scrape, "cache_scrape-" + accepted[i]);
                        s.setDaemon(true);
                        s.start();
                        scrapers.add(scrape);
                    }
                }

                // Sleep for 1 minute and loop.
                try {
                    Thread.sleep(60 * 1000); // Try again in 10 minutes.
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
                    Objects<Cache> server = odb.query(Cache.class).objects();
                    cache = server.first();
                    cache.running = false;
                    odb.store(cache);
                    odb.commit();
                    odb = null;
                }
            } catch(Exception ex) {
            }
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;

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
                if(!oK) {
                    try {
                        Thread.sleep(1000); // Try again in 1 second.
                    } catch(InterruptedException ex) {
                    }

                }
            }
            System.out.println("SeedlessCache Exited.");
        }
    }
}
