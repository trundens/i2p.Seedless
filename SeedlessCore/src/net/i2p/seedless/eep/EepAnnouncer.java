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
package net.i2p.seedless.eep;

import net.i2p.seedless.classes.DB;
import net.i2p.seedless.reuse.Announcer;
import net.i2p.seedless.reuse.scrape.Scraper;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Pattern;
import net.i2p.seedless.classes.eep;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class EepAnnouncer implements Runnable {

    private boolean testing = net.i2p.seedless.Version.testing;
    private int launchtime = 120; // 120
    private final int DEFAULT_PROXY = 4488;
    private volatile boolean spin;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private LinkedList<Scraper> scrapers = new LinkedList<Scraper>();
    private Thread announcerThread = null;
    private String dir = "./";
    private String base32 = null;
    private final static String DESC_FILE = "_eepsite_Description.txt";
    private static Pattern LNUMERIC = Pattern.compile("[0-9,]+");
    private String VERSION = "";

    /**
     * Default instance
     */
    public EepAnnouncer() {
        System.out.println("ERROR: EepAnnouncer.war too old.");
    }

    /**
     * Create default instance
     * @param me
     */
    public EepAnnouncer(String me, String dir) {
        System.out.println("ERROR: EepAnnouncer.war too old.");
    }

    public EepAnnouncer(String me, String basedir, String b32, String ver) {
        spin = true;
        proxyaddress = me.split(":")[0];
        proxyport = Integer.parseInt(me.split(":")[1]);
        dir = basedir;
        base32 = b32;
        VERSION = ver;
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
        if(!spin) {
            return;
        }
        DB db = null;
        ODB odb = null;
        int i;
        String weDo = "";
        // Get the Description from dir+"Descrip.txt", if non existant, create a default one.
        File cfg = new File(dir + DESC_FILE);
        FileInputStream fi = null;
        PrintStream fo = null;
        Boolean OK = false;
        Boolean PASS = false;
        String line;
        long timeStamp = 0;
        long newTimeStamp;

        try {
            fi = new FileInputStream(cfg);
            BufferedReader br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
            while(br.ready()) {
                line = br.readLine().trim();
                weDo = weDo.concat(line + "\n");
                OK = true;
            }
        } catch(FileNotFoundException fnfe) {
        } catch(IOException ioe) {
        }
        try {
            fi.close();
        } catch(Exception ex) {
        }
        if(!OK) {
            weDo = "Hidden Eepsite.\n";
            System.out.println("WARNING:\007");
            System.out.println("WARNING:");
            System.out.println("WARNING:");
            System.out.println("WARNING: No Description file " + dir + DESC_FILE + " Please edit the one created to describe your eepsite better, and then restart your router.");
            System.out.println("WARNING:");
            System.out.println("WARNING:");
            System.out.println("WARNING:");
            try {
                fo = new PrintStream(cfg, "UTF-8");
                fo.println("Hidden Eepsite.");
            } catch(FileNotFoundException ex) {
            } catch(IOException ioe) {
            }
            try {
                fo.close();
            } catch(Exception ex) {
            }
        }
        if(weDo.length() > 1024) {
            weDo = weDo.substring(0, 1024);
        }

        PrintStream out = null;
        try {
            out = new PrintStream(System.out, true, "UTF-8");
        } catch(UnsupportedEncodingException ex) {
            out = new PrintStream(System.out);
        }

        System.out.println(launchtime + " seconds to EepAnnouncer launch.");
        try {
            Thread.sleep(1000 * launchtime);
        } catch(InterruptedException ex) {
            return;
        }
        Iterator<Scraper> scrapeList;
        Scraper scrape = null;
        boolean oK;
        Thread s;
        Announcer announcer = null;


        // check if proxy is up?

        try {
            // Setup the Seedless announcerThread.
            announcer = new Announcer("eepsite", weDo, true, proxyaddress, proxyport);
            announcerThread = new Thread(announcer, "Seedless.EepAnnouncer.Announcer");

            while(spin) {
                // Check for updated description file.
                newTimeStamp = cfg.lastModified();
                if(newTimeStamp != timeStamp) {
                    OK = false;
                    weDo = "";
                    try {
                        fi = new FileInputStream(cfg);
                        BufferedReader br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
                        while(br.ready()) {
                            line = br.readLine().trim();
                            weDo = weDo.concat(line + "\n");
                        }
                        // Do a sanity check here so we don't put bad data on the servers.
                        if(weDo.contains(" ") && !(LNUMERIC.matcher(weDo).matches())) {
                            OK = true;
                        } else {
                            System.out.println("WARNING:\007");
                            System.out.println("WARNING:");
                            System.out.println("WARNING:");
                            System.out.println("WARNING: Description file does not have a more than one word or is just a number.");
                            System.out.println("WARNING: Please describe your eepsite properly!");
                            System.out.println("WARNING:");
                            System.out.println("WARNING:");
                            System.out.println("WARNING:");
                        }
                    } catch(FileNotFoundException fnfe) {
                    } catch(IOException ioe) {
                    }
                    try {
                        fi.close();
                    } catch(Exception ex) {
                    }

                    if(OK) {
                        if(weDo.length() > 1024) {
                            weDo = weDo.substring(0, 1024);
                        }
                        timeStamp = newTimeStamp;
                        announcer.changeMeta(weDo);
                        if(!announcer.wasrun()) {
                            // Launch announcer if never started.
                            out.println("Please fasten seatbelts, EepAnnouncer is now launched.");
                            if(testing) {
                                out.println("EepAnnouncer config set to " + weDo);
                            }
                            announcerThread.start();
                            // set here in the DB the eepsite b32 and OK to true
                            db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war EepAnnouncer:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                            if(db == null) {
                                return;
                            }
                            odb = db.getODB();
                            eep zap = new eep();
                            zap.setOK(true);
                            zap.setb32(base32);
                            zap.setVERSION(VERSION);
                            odb.store(zap);
                            odb.commit();
                            odb = null;
                            (new AttachNeoDatis()).DetachNeoDatis(db);
                            db = null;
                        }
                    }
                }

                // Sleep for 1 minute and loop.
                try {
                    Thread.sleep(60 * 1000); // Try again in 1 minute.
                } catch(InterruptedException ex) {
                    return;
                }

            }

        } finally {
            try {
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war EepAnnouncer:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db != null) {
                    odb = db.getODB();
                    Objects<eep> eeps = odb.query(eep.class, W.equal("b32", base32)).objects();
                    if(eeps.hasNext()) {
                        eep zap = eeps.first();
                        zap.setOK(false);
                        zap.setb32(base32);
                        zap.setVERSION(VERSION);
                        odb.store(zap);
                        odb.commit();
                    }
                }
            } catch(Exception ex) {
                // nop
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
                if(announcerThread != null && announcerThread.isAlive()) {
                    announcer.die();
                    announcerThread.interrupt();
                }
                if(!oK) {
                    try {
                        Thread.sleep(1000); // Try again in 1 second.
                    } catch(InterruptedException ex) {
                    }

                }
            }
            System.out.println("SeedlessServer Exited.");
        }
    }
}
