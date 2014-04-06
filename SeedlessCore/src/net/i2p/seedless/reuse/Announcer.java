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
package net.i2p.seedless.reuse;

import net.i2p.seedless.data.Base64;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.SeedlessServices;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 * Announce for specified type, if we can.
 *
 * @author sponge
 */
public class Announcer implements Runnable {

    private String metadata = " ";
    private String type = null;
    private final int DEFAULT_PROXY = 4488;
    private static final String QUERYSTRING = "/Seedless/seedless";
    private volatile boolean spin;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private boolean server;
    private boolean testing = net.i2p.seedless.Version.testing;
    private Lock lock = new ReentrantLock();
    private boolean shortcircuit = false;
    private static final Object objMutex = new Object();
    private boolean started = false;

    /**
     *
     * @param type
     * @param metadata
     * @param proxyaddress
     * @param proxyport
     */
    public Announcer(String type, String metadata, String proxyaddress, int proxyport) {
        SetupAnnouncer(type, metadata, false, proxyaddress, proxyport);
    }

    /**
     *
     * @param type
     * @param metadata
     * @param server
     * @param proxyaddress
     * @param proxyport
     */
    public Announcer(String type, String metadata, boolean server, String proxyaddress, int proxyport) {
        SetupAnnouncer(type, metadata, server, proxyaddress, proxyport);
    }

    /**
     * Kill it
     */
    public void die() {
        spin = false;
    }

    /**
     * Change the metadata, and announce
     * @param metadata
     */
    public void changeMeta(String metadata) {
        try {
            lock.lock();
            this.metadata = metadata;
            synchronized(objMutex) {
                shortcircuit = true;
                objMutex.notify();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @return metadata
     */
    public String getMeta() {
        try {
            lock.lock();
            return this.metadata;
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param type
     * @param metadata
     * @param server
     * @param proxyaddress
     * @param proxyport
     */
    private void SetupAnnouncer(String type, String metadata, boolean server, String proxyaddress, int proxyport) {
        spin = true;
        this.type = type;
        this.metadata = metadata;
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.server = server;
    }

    /**
     *
     * @return Time elapsed
     */
    private synchronized long once() {
        Long timeNow;
        Long elapsedTime = -2l;
        Objects<SeedlessServices> services;
        LinkedList<SeedlessServices> servicesC;
        SeedlessServices service;
        String url;
        HttpURLConnection request;
        BufferedReader data = null;
        InputStream in = null;
        boolean didsomething = false;
        DB db = null;
        ODB odb;
        lock.lock();
        try {
            // Reset if changed, we are OK to do this here as we are locked.
            shortcircuit = false;
            db = (new AttachNeoDatis()).AttachNeodatis(Thread.currentThread().getName() +" Announcer:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
            if(db == null) {
                return -1;
            }
            odb = db.getODB();
            if(testing) {
                System.out.println("Announce started for " + type + " " + metadata);
            }
            // will have to add more configs...
            timeNow = (new Date()).getTime(); // in miliseconds
            didsomething = false;
            //
            // Announce to everybody we know who accepts this type of announcement
            // with disreguard for the expiration, in case it's been days since we've been ran.
            //
            services = odb.query(SeedlessServices.class, W.equal("service", "seedless").and(W.like("metadata", type))).objects();
            if(services.hasNext()) {
                servicesC = new LinkedList<SeedlessServices>();
                while(services.hasNext()) {
                    service = new SeedlessServices(services.next());
                    servicesC.add(service);
                }
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                int successful = Math.min(10, servicesC.size());
                Collections.shuffle(servicesC, new Random());
                Iterator<SeedlessServices> servicesX = servicesC.iterator();
                while(servicesX.hasNext() && successful > 0) {
                    service = servicesX.next();
                    url = "http://" + service.dest + QUERYSTRING;
                    ProxyRequest proxy = new ProxyRequest();
                    try {
                        request = proxy.doURLRequest(url, "announce " + Base64.encode(type + " " + metadata, "UTF-8"), proxyaddress, proxyport);
                    } catch(UnsupportedEncodingException ex) {
                        request = proxy.doURLRequest(url, "announce " + Base64.encode(type + " " + metadata), proxyaddress, proxyport);
                    }
                    if(request != null) {
                        try {
                            if(request.getResponseCode() == 200) {
                                successful--;
                                didsomething = true;
                                in = request.getInputStream();
                                data = new BufferedReader(new InputStreamReader(in));
                                while(data.readLine() != null) {
                                    // nop, don't care, shouldn't get anything anyway
                                }
                                try {
                                    request.disconnect();
                                } catch(Exception ex) {
                                    // nop, don't care
                                }
                                request = null;
                                try {
                                    data.close();
                                } catch(Exception ex) {
                                    // nop, don't care
                                }
                                data = null;
                                try {
                                    in.close();
                                } catch(Exception ex) {
                                    // nop, don't care
                                }
                                in = null;
                            }
                        } catch(IOException ioe) {
                            if(request != null) {
                                try {
                                    request.disconnect();
                                } catch(Exception ex) {
                                    // nop, don't care
                                }
                                request = null;

                            }
                            if(request != null) {

                                try {
                                    data.close();
                                } catch(Exception ex) {
                                    // nop, don't care
                                }
                                data = null;
                            }
                            if(request != null) {
                                try {
                                    in.close();
                                } catch(Exception ex) {
                                    // nop, don't care
                                }
                                in = null;
                            }
                        }

                    }

                }
                elapsedTime = (new Date()).getTime() - timeNow; // in miliseconds
                if(elapsedTime > 3000000l) {
                    // We had a very huge time. What to do???
                    // perhaps print a warning??
                    // sleep for 10 minutes....
                    elapsedTime = 3000000l;
                }
                if(!didsomething) {
                    // There is a possiblity our list is old, and we need to reseed.
                    // Let's try again in 10 minutes.
                    System.out.println("No contacts made, for " + type);
                    elapsedTime = 3000000l;
                }
                if(testing) {
                    System.out.println("Announcer Sleeping  " + (3600000l - elapsedTime) / 60000 + " minutes for " + type);
                }

            } else {
                try {
                    if(db != null && !db.isClosed()) {
                        db.close();
                    }
                } catch(Exception ex) {
                }
                db = null;
                // no service yet...
                System.out.println("No service for " + type + " Announcer yet, Sleeping 10 minutes.");
                elapsedTime = 600l * 1000l;
            }
        } catch (InterruptedException ie) {
        } finally {
            lock.unlock();
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
        return elapsedTime;
    }

    public boolean wasrun() {
        return started;
    }

    /**
     * Guess :-)
     */
    public void run() {
        Long elapsedTime;
        Long timeLeft;
        started = true;
        while(spin) {
            elapsedTime = once();
            if(elapsedTime < 0) {
                return;
            }

            timeLeft = 3600000l - elapsedTime;
            // This is an interruptable sleep via notify().
            // Would be better to tick down timeLeft, but this should do nicely.
            if(timeLeft > 0 && spin && !shortcircuit) {
                synchronized(objMutex) {
                    try {
                        objMutex.wait(timeLeft);
                    } catch(InterruptedException ex) {
                        if(!spin) return;
                    }
                }
            }
        }
    }
}
