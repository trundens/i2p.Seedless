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
package net.i2p.seedless.reuse.scrape;

import net.i2p.seedless.data.Base64;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.SeedlessServices;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import net.i2p.I2PAppContext;
import net.i2p.seedless.classes.Proxy;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class Scraper implements Runnable {

    private boolean testing = net.i2p.seedless.Version.testing;
    private String metadata = "";
    private String type = null;
    private final int DEFAULT_PROXY = 4488;
    private static final String PROBESTRING = "/Seedless/index.jsp";
    private static final String QUERYSTRING = "/Seedless/seedless";
    private volatile boolean spin;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private boolean server;
    private long expires;
    // private Thread eepWalker = null;
    private boolean once;
    private boolean failed;
    private long started = 0;
    private int quick = 0;
    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private String ID = null;
    // private boolean walked = false;

    /**
     *
     * @param once
     * @param type
     * @param metadata
     * @param expires
     * @param proxyaddress
     * @param proxyport
     */
    public Scraper(boolean once, String type, String metadata, long expires, String proxyaddress, int proxyport) {
        SetupScraper(once, type, metadata, expires, false, proxyaddress, proxyport, null);
    }

    /**
     *
     * @param once
     * @param type
     * @param metadata
     * @param expires
     * @param proxyaddress
     * @param proxyport
     */
    public Scraper(boolean once, String type, String metadata, long expires, String proxyaddress, int proxyport, String ID) {
        SetupScraper(once, type, metadata, expires, false, proxyaddress, proxyport, ID);
    }

    /**
     *
     * @param type
     * @param metadata
     * @param expires
     * @param server
     * @param proxyaddress
     * @param proxyport
     */
    public Scraper(String type, String metadata, long expires, boolean server, String proxyaddress, int proxyport) {
        SetupScraper(false, type, metadata, expires, server, proxyaddress, proxyport, null);
    }

    /**
     *
     * @param once
     * @param type
     * @param metadata
     * @param expires
     * @param server
     * @param proxyaddress
     * @param proxyport
     */
    public Scraper(boolean once, String type, String metadata, long expires, boolean server, String proxyaddress, int proxyport) {
        SetupScraper(once, type, metadata, expires, server, proxyaddress, proxyport, null);
    }

    /**
     * Kill it.
     */
    public void die() {
        spin = false;
        isAlive.set(false);
    }

    public boolean isAlive() {
        return isAlive.get();
    }

    public void revive() {
        if(!isAlive.get()) {
            isAlive.set(true);
            synchronized(isAlive) {
                isAlive.notify();
            }
        }
    }

    /**
     *
     * @return true or false
     */
    public boolean failed() {
        return failed;
    }

    /**
     *
     * @return the service type
     */
    public String kind() {
        return type;
    }

    public String myID() {
        return ID;
    }

    /**
     *
     * @return thread of this instance
     */
    public Thread myThread() {
        return Thread.currentThread();
    }

    /**
     *
     * @return when the thread was started
     */
    public long startTime() {
        return this.started;
    }

    /**
     *
     * @param once
     * @param type
     * @param metadata
     * @param expires
     * @param server
     * @param proxyaddress
     * @param proxyport
     * @param ID
     */
    private void SetupScraper(boolean once, String type, String metadata, long expires, boolean server, String proxyaddress, int proxyport, String ID) {
        spin = true;
        this.type = type;
        this.metadata = metadata;
        this.expires = expires;
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.server = server;
        this.once = once;
        this.failed = false;
        this.ID = ID;
    }

    private void snooze() {
        if(testing) {
            System.out.println("Scrape snooze " + type);
        }

        isAlive.set(false);
        synchronized(isAlive) {
            while(!isAlive.get()) {
                try {
                    isAlive.wait();
                } catch(InterruptedException ex) {
                    if(!spin) {
                        return;
                    }
                }
            }
        }
        if(testing) {
            System.out.println("Scrape wake " + type);
        }
        // Update proxy data, blocking 'till a proxy is ready.
        DB db = null;
        ODB odb = null;
        try {
            boolean good = false;
            while(!good) {
                Proxy prox = null;
                Objects<Proxy> proxies;
                db = (new AttachNeoDatis()).AttachNeodatis(Thread.currentThread().getName() + " Scraper:" + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    return;
                }
                odb = db.getODB();
                proxies = odb.query(Proxy.class, W.equal("up", true)).objects();
                if(proxies.hasNext()) {
                    prox = proxies.first();
                    proxyaddress = prox.IP;
                    proxyport = prox.PORT;
                    good = true;
                } else {
                    try {
                        Thread.sleep(6000);
                    } catch(InterruptedException ex1) {
                        return; // Die!
                    }
                }
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
            }
        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
    }

    /**
     *
     */
    public void run() {
        isAlive.set(true);
        Long timeNow;

        Long elapsedTime;

        Objects<SeedlessServices> services;
        SeedlessServices service;

        //Collection<SeedlessServices> kill;

        String url;

        long weKnow = 0;
        String weWant;

        String b32addr;

        String sep = java.io.File.separator;
        String cd = "." + sep;
        try {
            cd = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + sep;
        } catch(IOException ex) {
        }

        /*
        EepWalker walk = new EepWalker(proxyaddress, proxyport, expires);
        eepWalker = new Thread(walk, "Seedless.EepWalker");
        eepWalker.setDaemon(true);
         */
        started = (new Date()).getTime();
        long toGo;
        LinkedList<SeedlessServices> listing = null;
        failed = false;
        File cfg = new File(cd + "quickseeds.txt");
        PrintStream fo = null;
        DB db = null;
        ODB odb = null;
        boolean again = false;
        int attempts = 0;
        SeedlessServices k;

        try {
            while(spin) {
                db = (new AttachNeoDatis()).AttachNeodatis(Thread.currentThread().getName() + " Scraper:" + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    return;
                }
                odb = db.getODB();
                if(testing) {
                    System.out.println("Scrape started for " + type + " " + metadata);
                }

                timeNow = (new Date()).getTime(); // in miliseconds
                services = odb.query(SeedlessServices.class, W.equal("service", "seedless").and(W.like("metadata", type))).objects();
                /*
                 * Set if we need to quickscan or not...
                 */
                weKnow = services.size();

                if(services.hasNext()) {
                    listing = new LinkedList<SeedlessServices>();
                    while(services.hasNext()) {
                        listing.add(services.next());
                    }
                    services = null;
                }
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                if(weKnow > 0) {
                    quick = 0;
                    try {
                        weWant = "locate " + Base64.encode(type + " " + metadata, "UTF-8");
                    } catch(UnsupportedEncodingException ex) {
                        weWant = "locate " + Base64.encode(type + " " + metadata);
                    }
                    if(testing) {
                        System.out.println("We have peers for " + type + " " + metadata);
                    }
                    /*
                     * Scramble these, and try to scrape 3 at most, as we do
                     * advertize to everyone we know anyway.
                     *
                     * This will atleast pick up new information that did not
                     * make it to us, and help prevent fragmenting.
                     *
                     */
                    toGo = Math.min(3, weKnow);
                    Collections.shuffle(listing, new Random());
                    Iterator<SeedlessServices> seedlessPeers = listing.iterator();

                    while(seedlessPeers.hasNext() && toGo > 0) {
                        attempts = 0;
                        service = seedlessPeers.next();
                        do {
                            attempts += 1;
                            again = false;
                            url = "http://" + service.dest + QUERYSTRING;
                            Scrape s = new Scrape(url, proxyaddress, proxyport, expires, service.dest, type, weWant, service.good);
                            if(testing) {
                                System.out.println("Scraping " + url);
                            }

                            if(testing && once && server) {
                                System.out.println(attempts + ": Scraping " + url + " for " + type);
                            }

                            Thread t = new Thread(s, "Seedless.Scrape-" + url);
                            t.setDaemon(true);
                            t.start();
                            while(t.isAlive()) {
                                try {
                                    Thread.sleep(10 * 1000); // check again in 10 seconds.
                                } catch(InterruptedException ex) {
                                    if(!spin) {
                                        return;
                                    }
                                }
                            }
                            // Check status of the scrape.
                            if(s.wasOK()) {
                                if(testing && once && server) {
                                    System.out.println(attempts + ": Scrape " + url + " for " + type + " Success.");
                                }
                                if(!service.good) {
                                    service.good = true;
                                    // save verified (*as best as we could) service.
                                    try {
                                        db = new DB(this.getClass().getName());
                                    } catch(DBException ex) {
                                        return;
                                    }
                                    odb = db.getODB();
                                    services = odb.query(SeedlessServices.class, W.equal("service", "seedless").and(W.equal("dest", service.dest)).and(W.equal("metadata", service.metadata))).objects();
                                    k = services.next();
                                    k.good = true;
                                    odb.store(k);
                                    odb.commit();
                                    odb = null;
                                    (new AttachNeoDatis()).DetachNeoDatis(db);
                                    db = null;
                                }
                                toGo--;
                            } else {
                                if(testing && once && server) {
                                    System.out.println(attempts + ": Scrape " + url + " for " + type + " Fail.");
                                }
                                if(s.KillMe()) {
                                    // This cleans out dead records.
                                    try {
                                        db = new DB(this.getClass().getName());
                                    } catch(DBException ex) {
                                        return;
                                    }
                                    odb = db.getODB();
                                    services = odb.query(SeedlessServices.class, W.equal("service", "seedless").and(W.equal("dest", service.dest)).and(W.like("metadata", type))).objects();
                                    if(services.hasNext()) {
                                        // load and delete manually, deleteAll fails
                                        while(services.hasNext()) {
                                            k = services.next();
                                            odb.delete(k);
                                        }
                                    //odb.deleteAll(services);
                                    }
                                    odb.commit();
                                    odb = null;
                                    (new AttachNeoDatis()).DetachNeoDatis(db);
                                    db = null;
                                } else {
                                    if(s.error() == 504 && attempts < 2) {
                                        again = true; // retry on a failed route
                                    }
                                }
                            }
                        } while(again && spin);
                    }
                    if(once && server) {
                        // Write out known good seeds
                        try {
                            db = new DB(this.getClass().getName());
                        } catch(DBException ex) {
                            return;
                        }
                        odb = db.getODB();
                        services = odb.query(SeedlessServices.class, W.equal("service", "seedless")).objects();
                        if(services.hasNext()) {
                            // be sure we have > 5, to avoid not having enough of them
                            int count = 0;
                            while(services.hasNext()) {
                                services.next();
                                count++;
                            }
                            if(count > 5) {
                                services = odb.query(SeedlessServices.class, W.equal("service", "seedless")).objects();
                                try {
                                    fo = new PrintStream(cfg, "UTF-8");
                                    while(services.hasNext()) {
                                        fo.println(services.next().dest);
                                    }
                                    fo.close();
                                } catch(UnsupportedEncodingException ex1) {
                                } catch(FileNotFoundException ex1) {
                                }
                            }

                        }
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                    }
                    // sleep for an hour.
                    elapsedTime = (new Date()).getTime() - timeNow; // in miliseconds
                    if(elapsedTime > 3000000l) {
                        // We had a very huge list. What to do???
                        // perhaps print a warning??
                        // sleep for 10 minutes....
                        elapsedTime = 3000000l;
                    }
                    if(ID == null) {
                        if(testing) {
                            System.out.println("Scraper Sleeping  " + (3600000l - elapsedTime) / 60000 + "minutes for " + type);
                        }
                        try {
                            Thread.sleep(3600000l - elapsedTime); // Try again in an hour.
                        } catch(InterruptedException ex) {
                            if(!spin) {
                                return;
                            }
                        }
                    }
                    if(once) {
                        snooze();
                        if(!spin) {
                            return;
                        }
                    }
                } else {
                    // We know nobody providing information :-(
                    if(testing || server) {
                        System.out.println("We have no peers for " + type + " " + metadata);
                    }

                    if(server) {
                        //if(!eepWalker.isAlive() && quick < 3) {
                        if(quick < 3) {
                            System.out.println("Attempting a quick seed in 3 minutes.");
                            try {
                                Thread.sleep(1000 * 60 * 3);
                            } catch(InterruptedException ex) {
                                return;
                            }

                            quick++;
                            // used only by seedless it'sself...
                            // try to do some quick seeds first.
                            BufferedReader br = null;
                            FileInputStream fi = null;

                            BufferedWriter bw = null;
                            String line;
                            boolean more = true;
                            try {
                                fi = new FileInputStream(cfg);
                            } catch(FileNotFoundException ex) {
                                try {
                                    fo = new PrintStream(cfg, "UTF-8");
                                    fo.println("# No idea who these are/don't care who, seen them long enough.");
                                    fo.println("t5qds7twb7eyyvp2fchhbn74tgdzv774rlru5guit5jkn4a6do7a.b32.i2p");
                                    fo.println("wrrwzdgsppwl2g2bdohhajz3dh45ui6u3y7yuop5ivvfzxtwnipa.b32.i2p");
                                    fo.println("qw5e5pfd64feb44j477lebekfguti73ffafy7amq7o3ib6vrgtka.b32.i2p");
                                    fo.println("zy37tq6ynucp3ufoyeegswqjaeofmj57cpm5ecd7nbanh2h6f2ja.b32.i2p");
                                    fo.println("ps7go5iana5y4bnvdap4n25lbi53uxl7bsjyhbic5f42mmsvk6xa.b32.i2p");
                                    fo.println("# Known");
                                    fo.println("# guest123");
                                    fo.println("3i2rcjcis3fmy2ylj356qko2eaj5dx5pxlsqc6wqyeirod5uzwzq.b32.i2p");
                                    fo.println("# vKarl");
                                    fo.println("jyeth4lg2b4lkz3jqelhejpx5b6gy2icc3u7bqesmn4i4aze4efa.b32.i2p");
                                    fo.println("# i2peek-a-boo");
                                    fo.println("qgv64klyy4tgk4ranaznet5sjgi7ccsrawtjx3j5tvekvvfl67aa.b32.i2p");
                                    fo.println("# sponge");
                                    fo.println("o5hu7phy7udffuhts6w5wn5mw3sepwe3hyvw6kthti33wa2xn5tq.b32.i2p");
                                    fo.println("vnmf4apo3mpxitki2nzqjx24cq4ykuiptpdmihgyecomcql5ttaa.b32.i2p");
                                    fo.println("eomdwleo4rs3n4xfmeja6sbdaauqj75hlfchxkzl4jmqpgziyexa.b32.i2p");
                                    fo.close();
                                    fi = new FileInputStream(cfg);
                                } catch(UnsupportedEncodingException ex1) {
                                } catch(FileNotFoundException ex1) {
                                }

                            }
                            if(fi != null) {
                                try {
                                    br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
                                    while(br.ready() && more) {
                                        line = br.readLine().trim().split(";")[0].trim().split("#")[0].trim();
                                        if((!(line.length() == 0)) && line.endsWith(".i2p")) {
                                            url = "http://" + line + PROBESTRING;
                                            Probe p = new Probe(url, proxyaddress, proxyport, expires, line);
                                            Thread t = new Thread(p, "Seedless.Probe-" + url);
                                            t.setDaemon(true);
                                            t.start();
                                            while(t.isAlive()) {
                                                try {
                                                    Thread.sleep(10 * 1000);
                                                } catch(InterruptedException ex) {
                                                    if(!spin) {
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch(UnsupportedEncodingException ex) {
                                } catch(IOException ex) {
                                } finally {
                                    try {
                                        fi.close();
                                    } catch(Exception ex) {
                                    }
                                }
                            } else {
                                System.out.println("Seedless: ERROR could not create or open quickseeds.txt!");
                                /*
                                System.out.println("Seedless: Will revert to long search on hosts.txt");
                                System.out.println("Seedless: this may take SEVERAL HOURS.");
                                 */
                                System.out.println("Please check permissions in your directory!");
                                String username;
                                username = System.getProperty("user.name");
                                System.out.println("Seedless: If the following user and path does not look correct, please bug report.");
                                System.out.println("Seedless: User: " + username + " Path: " + cd);
                            }
                        /*
                         * TEMP REPLACEMENT 'TILL NEW IDEA IS FOUND
                         */
                        } else if(once && quick == 3) {
                            failed = true;
                            quick = 0;
                            snooze();
                            if(!spin) {
                                return;
                            }
                        } else if(quick == 3) {
                            quick = 0;
                        // loop and just keep attempting connections
                        }
                    /*
                     *
                     * HOSTS.TXT IS NO LONGER USABLE! THIS CODE NEEDS A REPLACEMENT!
                     *
                    } else if(!eepWalker.isAlive() && quick < 3) {
                    quick++;
                    if(walked) {
                    //walk = new EepWalker(proxyaddress, proxyport, expires);
                    eepWalker = new Thread(walk, "Seedless.EepWalker");
                    eepWalker.setDaemon(true);
                    }
                    eepWalker.start();
                    walked = true;
                    try {
                    Thread.sleep(300 * 1000); // Try again in 5 minutes.
                    } catch(InterruptedException ex) {
                    if(!spin) {
                    return;
                    }
                    }
                    } else if(eepWalker.isAlive()) {
                    try {
                    Thread.sleep(600 * 1000); // Try again in 10 minutes.
                    } catch(InterruptedException ex) {
                    if(!spin) {
                    return;
                    }
                    }

                    } else if(once && !eepWalker.isAlive() && quick == 3) {
                    failed = true;
                    quick = 0;
                    snooze();
                    if(!spin) {
                    return;
                    }
                    }
                     */

                    } else { // else not a Seedless server :-D
                        // In all fail cases, sleep for 10 minutes, and try again.
                        try {
                            Thread.sleep(600 * 1000); // Try again in 10 minutes.
                        } catch(InterruptedException ex) {
                            if(!spin) {
                                return;
                            }
                        }
                        if(once) {
                            failed = true;
                            snooze();
                            if(!spin) {
                                return;
                            }
                        }
                    }
                }
            }

        } finally {
            spin = false;
            isAlive.set(false);
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
            if(once && server) {
                System.out.println("SeedlessSeedFinder seedless server scraper Exited.");
            }
        }
    }
}
