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
package net.i2p.seedless.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.SeedlessServices;
import net.i2p.seedless.classes.TorrentSearch;
import net.i2p.seedless.data.Base64;
import net.i2p.seedless.reuse.AttachNeoDatis;
import net.i2p.seedless.reuse.ProxyRequest;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class TorrentIndexer implements Runnable {

    private String proxyaddress = null;
    private int proxyport = 0;
    private String request = null;
    private String rhost = null;

    /**
     *
     * @param proxyaddress address of the proxy to use
     * @param proxyport port of the proxy to use
     * @param request maggot URI request
     */
    TorrentIndexer(String proxyaddress, int proxyport, String request) {
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.request = request;
    }

    /**
     *
     * @param proxyaddress address of the proxy to use
     * @param proxyport port of the proxy to use
     * @param request maggot URI request
     * @param rhost host to immediately query
     */
    TorrentIndexer(String proxyaddress, int proxyport, String request, String rhost) {
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.request = request;
        this.rhost = rhost;
    }

    /**
     * Try to index the torrent.
     */
    public void run() {
        DB db = null;
        ODB odb = null;
        boolean resolved = false;
        String hash = request.substring(9, 49);
        LinkedList<String> dests = new LinkedList<String>();
        Iterator<String> scrapeList;
        int tries = 2;
        HttpURLConnection huc;
        InputStream in;
        BufferedReader data;
        TorrentSearch ts = null;
        TorrentSearch nts = null;
        String cltype;
        //System.out.println("Index on " + request);

        try {
            try {
                db = new DB(this.getClass().getName());
                odb = db.getODB();
                Objects<TorrentSearch> tos = odb.query(TorrentSearch.class, W.equal("maggot", request)).objects();
                if(tos.hasNext()) {
                    ts = tos.next();
                    nts = new TorrentSearch(ts);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                return;
            } finally {
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
            }


            while(!resolved && tries > 0) {
                tries--;
                dests = new LinkedList<String>();


                if (rhost == null) {
                    // get a list of peers
                    try {
                        db = new DB(this.getClass().getName());
                        odb = db.getODB();
                        Objects<SeedlessServices> services = odb.query(SeedlessServices.class, W.equal("service", "torrent").and(W.like("metadata", hash))).objects();
                        while(services.hasNext()) {
                            SeedlessServices ss = services.next();
                            if(hash.equals(ss.metadata.split("\n")[0])) {
                                dests.add(ss.dest);
                            }
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                    }
                    if(dests.isEmpty()) {
                        //System.out.println(request + " No dests, killing.");
                        try {
                            db = new DB(this.getClass().getName());
                            odb = db.getODB();
                            Objects<TorrentSearch> tos = odb.query(TorrentSearch.class, W.equal("maggot", request)).objects();
                            while(tos.hasNext()) {
                                TorrentSearch foo = tos.next();
                                odb.delete(foo);
                            }
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            odb = null;
                            (new AttachNeoDatis()).DetachNeoDatis(db);
                            db = null;
                        }

                        return;
                    }
                } else {
                    dests.add(rhost);
                }

                scrapeList = dests.iterator();
                while(scrapeList.hasNext() && !resolved) {
                    String host = scrapeList.next();
                    String URL = "http://" + host + request.substring(8);
                    //System.out.println("Requesting " + URL);
                    ProxyRequest proxy = new ProxyRequest();
                    try {
                        huc = proxy.doURLRequest(URL, null, proxyaddress, proxyport);
                    } catch(InterruptedException ex) {
                        return;
                    }
                    int rc = 0;
                    if(huc != null) {
                        try {
                            rc = huc.getResponseCode();
                            nts.code = rc;
                            if(rc == 200) {
                                String stuff = null;
                                in = huc.getInputStream();
                                data = new BufferedReader(new InputStreamReader(in));
                                nts.maggot = Base64.decodeToString(data.readLine().trim(), "UTF-8");
                                nts.filename = URLDecoder.decode(Base64.decodeToString(data.readLine().trim(), "UTF-8"), "UTF-8");
                                nts.filedata = Base64.decodeToString(data.readLine().trim(), "UTF-8");
                                nts.comment = Base64.decodeToString(data.readLine().trim(), "UTF-8");
                                nts.date = (new Date()).getTime();
                                // then each category, these can occur in any order!
                                do {
                                    stuff = data.readLine();
                                    if(stuff != null) {
                                        String[] what = stuff.trim().split(" ");
                                        if(what.length == 2) {
                                            String claz = what[0].trim();
                                            if(claz.startsWith("cat_")) {
                                                try {
                                                    Field fld = nts.getClass().getDeclaredField(claz);
                                                    cltype = fld.getType().toString();
                                                    // java.lang.String
                                                    // long
                                                    // int
                                                    //System.out.println("field " + claz + " is type :" + fld.getType().toString());
                                                    // boolean isset = false;
                                                    String cldat = Base64.decodeToString(what[1].trim(), "UTF-8");
                                                    int theint = 0;
                                                    long thelong = 0;
                                                    boolean isint = false;
                                                    boolean islong = false;

                                                    // nts->claz = cldat
                                                    // Sloppy, but should work!
                                                    if(cltype.equals("int")) {
                                                        try {
                                                            theint = asafeint(cldat);
                                                         isint = true;
                                                        } catch(NumberFormatException nfe) {
                                                        }
                                                    }

                                                    else if (cltype.equals("long")) {
                                                        try {
                                                            thelong = asafelong(cldat);
                                                            islong = true;
                                                        } catch(NumberFormatException nfe) {
                                                        }
                                                    }
                                                    // else it's java.lang.String

                                                    if(islong) {
                                                        try {
                                                            fld.setLong(nts, thelong);
                                                            //isset = true;
                                                        } catch(IllegalArgumentException ex) {
                                                        } catch(IllegalAccessException ex) {
                                                        } catch(SecurityException ex) {
                                                        }
                                                    } else if(isint) {
                                                        try {
                                                            fld.setInt(nts, theint);
                                                            //isset = true;
                                                        } catch(IllegalArgumentException ex) {
                                                        } catch(IllegalAccessException ex) {
                                                        } catch(SecurityException ex) {
                                                        }
                                                    } else {

                                                    // if(!isset) {
                                                        try {
                                                            fld.set(nts, cldat);
                                                        } catch(IllegalArgumentException ex) {
                                                        } catch(IllegalAccessException ex) {
                                                        } catch(SecurityException ex) {
                                                        }
                                                    }
                                                } catch(NoSuchFieldException ex) {
                                                } catch(SecurityException ex) {
                                                }
                                            }
                                        }
                                    }
                                } while(stuff != null);

                                ts = new TorrentSearch(nts);
                                try {
                                    huc.disconnect();
                                } catch(Exception ex) {
                                }
                                huc = null;
                                try {
                                    data.close();
                                } catch(Exception ex) {
                                }
                                data = null;
                                try {
                                    in.close();
                                } catch(Exception ex) {
                                }
                                in = null;
                                resolved = true;
                            //System.out.println(request + " Resolved.");

                            } else if(rc == 403) {
                                //System.out.println(request + " error " + rc);
                                resolved = true;
                                ts.code = rc;
                                try {
                                    huc.disconnect();
                                } catch(Exception ex) {
                                }
                                huc = null;
                            } else {
                                try {
                                    huc.disconnect();
                                } catch(Exception ex) {
                                }
                            }

                        } catch(NullPointerException npe) {
                            if(huc != null) {
                                try {
                                    huc.disconnect();
                                } catch(Exception ex) {
                                }
                                huc = null;
                            }
                        } catch(IOException ioe) {
                            if(huc != null) {
                                try {
                                    huc.disconnect();
                                } catch(Exception ex) {
                                }
                                huc = null;
                            }
                        }
                    } else {
                        //System.out.println(request + " No connection, " + tries);
                    }
                }
            }
            // Update record.
            try {
                if(ts.code < 100) {
                    ts.code++;
                }
                //System.out.println("Storing " + ts.maggot);
                db = new DB(this.getClass().getName());
                odb = db.getODB();
                Objects<TorrentSearch> tos = odb.query(TorrentSearch.class, W.equal("maggot", request)).objects();
                while(tos.hasNext()) {
                    TorrentSearch foo = tos.next();
                    odb.delete(foo);
                }
                odb.store(ts);
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("Dumping info for debugging:");
                System.out.println("cat_ISBN:" + ts.cat_ISBN);
                System.out.println("cat_album:" + ts.cat_album);
                System.out.println("cat_artist:" + ts.cat_artist);
                System.out.println("cat_author:" + ts.cat_author);
                System.out.println("cat_bitrate:" + ts.cat_bitrate);
                System.out.println("cat_codec:" + ts.cat_codec);
                System.out.println("cat_composer:" + ts.cat_composer);
                System.out.println("cat_flavor:" + ts.cat_flavor);
                System.out.println("cat_format:" + ts.cat_format);
                System.out.println("cat_genre:" + ts.cat_genre);
                System.out.println("cat_languages:" + ts.cat_languages);
                System.out.println("cat_length:" + ts.cat_length);
                System.out.println("cat_main:" + ts.cat_main);
                System.out.println("cat_os:" + ts.cat_os);
                System.out.println("cat_patch_crack:" + ts.cat_patch_crack);
                System.out.println("cat_publisher:" + ts.cat_publisher);
                System.out.println("cat_reference_link:" + ts.cat_reference_link);
                System.out.println("cat_release_date:" + ts.cat_release_date);
                System.out.println("cat_ripper:" + ts.cat_ripper);
                System.out.println("cat_series_name:" + ts.cat_series_name);
                System.out.println("cat_subtitle_languages:" + ts.cat_subtitle_languages);
                System.out.println("cat_theme:" + ts.cat_theme);
                System.out.println("cat_title:" + ts.cat_title);
                System.out.println("comment:" + ts.comment);
                System.out.println("filedata:" + ts.filedata);
                System.out.println("filename:" + ts.filename);
                System.out.println("maggot:" + ts.maggot);
            } finally {
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
     * @param what
     * @return long if string parses
     */
    private long asafelong(String what) throws NumberFormatException {
        if(what != null) {
            long bar = Long.parseLong(what.trim());
            return bar;
        }
        throw new NumberFormatException();
    }

    /**
     *
     * @param what
     * @return int if string parses
     */
    private int asafeint(String what) throws NumberFormatException {
        if(what != null) {
            int bar = Integer.parseInt(what.trim());
            return bar;
        }
        throw new NumberFormatException();
    }
}
