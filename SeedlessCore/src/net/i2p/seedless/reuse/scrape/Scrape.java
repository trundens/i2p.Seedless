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
import net.i2p.seedless.data.Base64;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.SeedlessServices;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class Scrape implements Runnable {

    private String url = null;
    private String weDo = null;
    private String proxyaddress;
    private long expires;
    private int proxyport;
    private String b32addr;
    private String what;
    private boolean success = false;
    private boolean cool;
    private boolean testing = net.i2p.seedless.Version.testing;
    private static Pattern LALPHANUMERIC = Pattern.compile("[A-Za-z0-9,_-]+");
    private static Pattern LNUMERIC = Pattern.compile("[0-9,]+");
    private static Pattern LHEX = Pattern.compile("[a-f0-9]+");
    private boolean killme = false;
    private int errno = 504;

    Scrape(String url, String proxyaddress, int proxyport, long expires, String b32addr, String what, String weDo, boolean cool) {
        this.url = url;
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.expires = expires;
        this.b32addr = b32addr;
        this.what = what;
        this.weDo = weDo;
        this.cool = cool;
    }

    public boolean wasOK() {
        return success;
    }

    public boolean KillMe() {
        return killme;
    }

    public int error() {
        return errno;
    }

    public void run() {
        Objects<SeedlessServices> services;
        SeedlessServices service = null;
        SeedlessServices killService = null;
        HttpURLConnection request = null;
        String line;
        BufferedReader data = null;
        InputStream in = null;
        String metadata = "";
        String us = "";
        List<String> metadatas = new ArrayList<String>();
        String newMetadata = null;
        String newb32 = null;
        Long newDate = null;
        DB db = null;
        ODB odb = null;
        int tix = 0;

        try {
            ProxyRequest proxy = new ProxyRequest();
            if(cool) {
                errno = 200;
            } else {
                // Check as best we can that the server is a real verified server.
                // If it isn't kill it here.
                errno = 504;
                String turl = url.substring(0, url.indexOf("/Seedless/")) + "/Seedless/index.jsp";
                request = proxy.doURLRequest(turl, null, proxyaddress, proxyport);
                if(request != null) {
                    errno = request.getResponseCode();
                    if(errno == 200) {
                        // double check for X-Seedless returned header.
                        us = request.getHeaderField("X-Seedless");
                        if(us == null) {
                            errno = 404;
                        }
                    }
                }
            }
            if(errno != 200) {
                try {
                    request.disconnect();
                } catch(Exception ex) {
                    // nop, don't care
                }
                request = null;
                // Should also keep possibly a failed attempt counter?
                // Transient errors are a bitch!
                if((errno > 400 && errno < 500)) {
                    // Delete this record, it's dead.
                    // System.out.print("Got Error, >" + turl + "< ");
                    // System.out.println(errno);
                    killme = true;
                }
            // 504 should do a retry
            } else {
                proxy = new ProxyRequest();
                request = proxy.doURLRequest(url, weDo, proxyaddress, proxyport);
            }
            if(request != null) {
                try {
                    errno = request.getResponseCode();
                    if(errno == 200) {
                        // double check for X-Seedless returned header.
                        us = request.getHeaderField("X-Seedless");
                        if(us == null) {
                            errno = -200;
                        }
                    // to-do debug switch
                    } else {
                        if(testing) {
                            if(errno == 406) {
                                System.out.println("Scrape on " + url + " Failed: " + weDo + " " + request.getResponseMessage());
                            }
                        }
                    }
                    in = request.getInputStream();
                    data = new BufferedReader(new InputStreamReader(in));
                    while((line = data.readLine()) != null) {
                        if(errno == 200) {
                            metadatas.add(line);
                        }
                    }
                } catch(IOException ex) {
                    request = null;
                }
                if(request != null) {
                    try {
                        request.disconnect();
                    } catch(Exception ex) {
                    }
                }
                request = null;
                if(data != null);
                try {
                    data.close();
                } catch(Exception ex) {
                }
                data = null;
                if(in != null) {
                    try {
                        in.close();
                    } catch(Exception ex) {
                    }
                }
                in = null;

                // How many records is too many records??
                // lets's try 200.
                if(errno == 200) {
                    // to-do debug switch
                    if(testing) {
                        if(metadatas.size() > 200) {
                            System.out.println("Scrape seedless '" + weDo + "' WARNING: many records from " + url);
                        }
                    }
                    Iterator it = metadatas.iterator();
                    Date now = new Date();
                    long date = now.getTime(); // in miliseconds

                    while(it.hasNext()) {
                        boolean vfy = true;
                        try {
                            metadata = Base64.decodeToString((String)it.next(), "UTF-8");
                        } catch(UnsupportedEncodingException ex) {
                            metadata = null;
                        }
                        newMetadata = null;
                        if(metadata != null) {
                            try {
                                newb32 = metadata.split(" ", 3)[0];
                                newDate = Long.parseLong(metadata.split(" ", 3)[1]);
                                newMetadata = metadata.split(" ", 3)[2];
                                // check for valid seedless / torrent data
                                //test
                                if(what.equals("seedless")) {
                                    vfy = false;
                                    if(newMetadata.contains("\n") || newMetadata.contains(" ") || newMetadata.endsWith(",") || newMetadata.startsWith(",")) {
                                        // to-do debug switch
                                        if(testing) {
                                            System.out.println("Scrape seedless ',\\n ,' WARNING: Bad data from " + url);
                                            System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                        }
                                        newMetadata = null;
                                    } else if(LNUMERIC.matcher(newMetadata).matches()) {
                                        // to-do debug switch
                                        if(testing) {
                                            System.out.println("Scrape seedless LNUMERIC WARNING: Bad data from " + url);
                                            System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                        }
                                        newMetadata = null;
                                    } else if(!LALPHANUMERIC.matcher(newMetadata).matches()) {
                                        // to-do debug switch
                                        if(testing) {
                                            System.out.println("Scrape seedless LALPHANUMERIC WARNING: Bad data from " + url);
                                            System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                        }
                                        newMetadata = null;
                                    } else {
                                        String[] ml = newMetadata.split(",");
                                        for(errno = 0; errno < ml.length; errno++) {
                                            int l = ml[errno].length();
                                            if(l > 20 || l < 1 || ml[errno].contains(" ")) {
                                                // to-do debug switch
                                                if(testing) {
                                                    System.out.println("Scrape seedless length WARNING: Bad data from " + url);
                                                    System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                                }
                                                newMetadata = null;
                                            }
                                        }
                                    }
                                }
                                // test
                                if(what.equals("eepsite")) {
                                    if(!newMetadata.contains(" ")) {
                                        // to-do debug switch
                                        if(testing) {
                                            System.out.println("Scrape eepsite no space WARNING: Bad data from " + url);
                                            System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                        }
                                        newMetadata = null;
                                    }
                                    if(LNUMERIC.matcher(newMetadata).matches()) {
                                        // to-do debug switch
                                        if(testing) {
                                            System.out.println("Scrape eepsite LNUMERIC WARNING: Bad data from " + url);
                                            System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                        }
                                        newMetadata = null;
                                    }
                                }
                                // perfect
                                if(what.equals("torrent")) {
                                    String[] ml = newMetadata.split("\n");
                                    if(ml.length != 3) {
                                        newMetadata = null;
                                    } else {
                                        if(ml[0].length() != 40) {
                                            newMetadata = null;
                                        } else if(ml[1].length() != 28) {
                                            newMetadata = null;
                                        } else if(!(ml[2].equals("seed") || ml[2].equals("leech"))) {
                                            newMetadata = null;
                                        } else if(ml[0].length() != 40 || !LHEX.matcher(ml[0]).matches()) {
                                            newMetadata = null;
                                        }
                                    }
                                }

                            } catch(Exception ex) {
                                // caused by bad data!
                                // to-do debug switch
                                if(testing) {
                                    System.out.println("Scrape WARNING: Bad data from " + url);
                                    System.out.println("Scrape WARNING: Bad data -->" + metadata + "<--");
                                }
                                newMetadata = null;
                            }
                        }
                        if(newMetadata != null) {
                            // Store the data if it is newer
                            db = (new AttachNeoDatis()).AttachNeodatis(Thread.currentThread().getName() + " Scrape:" + Thread.currentThread().getStackTrace()[1].getLineNumber());
                            if(db == null) {
                                return;
                            }
                            odb = db.getODB();
                            services = odb.query(SeedlessServices.class, W.equal("service", what).and(W.equal("dest", newb32)).and(W.equal("metadata", newMetadata))).objects();
                            if(!services.hasNext()) {
                                // No entries, so save it...
                                if(tix < 50) {
                                    // ...But only 50 new ones at a time!
                                    tix++;
                                    if(us.equals(newb32)) {
                                        service = new SeedlessServices(what, newMetadata, newb32, date + (newDate * 60000l), true, vfy);
                                    } else {
                                        service = new SeedlessServices(what, newMetadata, newb32, date + (newDate * 60000l), false, vfy);
                                    }
                                    odb.store(service);
                                    odb.commit();
                                    if(testing) {
                                        System.out.println("Seedless  " + b32addr + ", provided " + metadata);
                                    }
                                }
                            } else {
                                // One entry.
                                service = services.next();
                                while(services.hasNext()) {
                                    // delete anything else!
                                    killService = services.next();
                                    odb.delete(killService);
                                }
                                if(service.date < date + (newDate * 60000l) || (us.equals(newb32) && (!service.us))) {
                                    // Newer, or self without knowing self so update it.
                                    // odb.delete(service);
                                    service.date = date + (newDate * 60000l);
                                    if(us.equals(b32addr)) {
                                        service.us = true;
                                    } else {
                                        service.us = false;
                                    }
                                    odb.store(service);
                                    odb.commit();
                                    if(testing) {
                                        System.out.println("Seedless  " + b32addr + ", updated " + metadata);
                                    }
                                }

                            }
                            odb = null;
                            (new AttachNeoDatis()).DetachNeoDatis(db);
                            db = null;

                        }
                    }
                    if(tix >= 50) {
                        System.out.println("Scrape seedless '" + weDo + "' WARNING: many new records from " + url);
                        System.out.println("Only saved 50.");
                    }

                    success = true;

                } else {
                    //    System.out.println("No seedless peer at: " + b32addr);
                }
            } else {
                // System.out.println("No connect success for peer: " + b32addr);
            }
        } catch(InterruptedException ie) {
        } catch(IOException ex) {
            // nop
        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
            if(request != null) {
                try {
                    request.disconnect();
                } catch(Exception ex) {
                }
            }
            request = null;
            if(data != null);
            try {
                data.close();
            } catch(Exception ex) {
            }
            data = null;
            if(in != null) {
                try {
                    in.close();
                } catch(Exception ex) {
                }
            }
            in = null;
        }
    }
}
