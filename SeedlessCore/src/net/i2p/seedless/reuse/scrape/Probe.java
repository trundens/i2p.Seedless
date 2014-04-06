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
package net.i2p.seedless.reuse.scrape;

import net.i2p.seedless.reuse.ProxyRequest;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.SeedlessServices;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Date;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class Probe implements Runnable {

    private String url = null;
    private String weDo = null;
    private String proxyaddress;
    private long expires;
    private int proxyport;
    private String b32addr;
    private boolean success = false;

    /**
     *
     * @param url
     * @param proxyaddress
     * @param proxyport
     * @param expires
     * @param b32addr
     */
    Probe(String url, String proxyaddress, int proxyport, long expires, String b32addr) {
        this.url = url;
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.expires = expires;
        this.b32addr = b32addr;
    }

    public boolean good() {
        return success;
    }

    public void run() {
        Objects<SeedlessServices> services;
        DB db = null;
        ODB odb = null;
        SeedlessServices service = null;
        HttpURLConnection request;
        String line;
        BufferedReader data;
        InputStream in;
        int i;
        String metadata = "";
        String us = "";
        try {
            ProxyRequest proxy = new ProxyRequest();
            request = proxy.doURLRequest(url, weDo, proxyaddress, proxyport);
            if(request != null) {
                try {
                    i = request.getResponseCode();
                    if(i == 200) {
                        // double check for X-Seedless returned header.
                        us = request.getHeaderField("X-Seedless");
                        if(us == null) {
                            i = -200;
                        }
                    }
                    in = request.getInputStream();
                    data = new BufferedReader(new InputStreamReader(in));
                    while((line = data.readLine()) != null) {
                        if(i == 200) {
                            try {
                                if(Integer.parseInt(line.trim().split(" ")[1]) > 120) {
                                    metadata = metadata + "," + line.trim().split(" ")[0];
                                }
                            // Silently ignore any bad data.
                            } catch(NumberFormatException nfe) {
                                // nop
                            }
                        }
                    }
                    if(metadata.length() < 1) {
                        i = 404;
                    }
                    if(!metadata.contains("seedless")) {
                        i = 404;
                    }
                    try {
                        request.disconnect();
                    } catch(Exception ex) {
                        // nop, don't care
                    }
                    try {
                        data.close();
                    } catch(Exception ex) {
                        // nop, don't care
                    }
                    try {
                        in.close();
                    } catch(Exception ex) {
                        // nop, don't care
                    }
                    request = null;
                    data = null;
                    in = null;
                    if(i == 200) {
                        db = (new AttachNeoDatis()).AttachNeodatis(Thread.currentThread().getName() + " Probe:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                        if(db == null) {
                            return;
                        }
                        odb = db.getODB();
                        metadata = metadata.substring(1);
                        // Success! Store the data if it does NOT exist already.
                        Date now = new Date();
                        long date = now.getTime(); // in miliseconds
                        services = odb.query(SeedlessServices.class, W.equal("service", "seedless").and(W.equal("dest", b32addr))).objects();
                        if(!services.hasNext()) {
                            // No entries, so save it...
                            if(us.equals(b32addr)) {
                                service = new SeedlessServices("seedless", metadata, b32addr, date + (expires * 60000l), true, true);
                            } else {
                                service = new SeedlessServices("seedless", metadata, b32addr, date + (expires * 60000l), false, true);
                            }
                            odb.store(service);
                            odb.commit();
                            success = true;
                            System.out.println("Seedless discovered a seedless server " + b32addr + ", providing " + metadata);
                        } else {
                            while(services.hasNext()) {
                                service = services.next();
                            }
                        }
                        odb = null;
                        db.close();
                        db = null;
                    } else {
                        //    System.out.println("No seedless peer at: " + b32addr);
                    }
                } catch(DBException ex) {
                } catch(IOException ex) {
                }
            } else {
                // System.out.println("No connect success for peer: " + b32addr);
            }
        } catch (InterruptedException ie) {
        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
    }
}
