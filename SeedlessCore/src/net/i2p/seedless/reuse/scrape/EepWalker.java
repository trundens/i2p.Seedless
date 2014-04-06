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
 * /
package net.i2p.seedless.reuse.scrape;

import net.i2p.seedless.data.DestMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import net.i2p.I2PAppContext;
import net.i2p.client.naming.NamingService;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.Proxy;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 *
 * /
public class EepWalker implements Runnable {

    private final int DEFAULT_PROXY = 4488;
    private static final String PROBESTRING = "/Seedless/index.jsp";
    private volatile boolean spin;
    private String proxyaddress = "127.0.0.1";
    private int proxyport = DEFAULT_PROXY;
    private long when;

    /**
     *
     * @param proxyaddress
     * @param proxyport
     * @param when
     *
     * /
    public EepWalker(String proxyaddress, int proxyport, long when) {
        this.proxyaddress = proxyaddress;
        this.proxyport = proxyport;
        this.when = when;
    }

    public void run() {
        int skip = 0;
        NamingService root = I2PAppContext.getGlobalContext().namingService();
        int max = root.size();

        Properties foo = new Properties();
        // Key "skip": skip that many entries
        // Key "limit": max number to return
        foo.setProperty("skip", "" + skip); // this one gets incremented
        foo.setProperty("limit", "1");
        // search the "router" book, as it has everything.
        // net.i2p.client.naming.NamingService.createInstance(net.i2p.I2PAppContext.getCurrentContext()).getNames(foo);
        // filter=none begin=x end=y
        root.getBase64Entries(foo);
        File cfg;
        try {
            cfg = new File(I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath().toString() + java.io.File.separatorChar + "hosts.txt");
        } catch(IOException ex) {
            cfg = new File("hosts.txt");
        }
        BufferedReader br = null;
        FileInputStream fi = null;
        String line;
        DestMapper dm = new DestMapper();
        String url;
        String b32addr;

        try {
            System.out.println("WARNING! No service, brute force seeding Seedless. This may take several hours depending on how many hosts are known by your router.");
            fi = new FileInputStream(cfg);
            br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
            while(br.ready()) {
                line = br.readLine().trim();
                if(!line.isEmpty() && !line.startsWith(";") && !line.startsWith("#") && line.contains("=")) {
                    url = "http://" + line.split("=")[0] + PROBESTRING;
                    b32addr = dm.b32(line.split("=")[1]);
                    if(b32addr.equals("")) {
                        System.out.println("WARNING: Seedless found a corrupt entry in hosts.txt, please remove from your addressbook " + line.split("=")[0]);
                    } else {
                        DB db = null;
                        ODB odb = null;
                        try {
                            boolean good = false;
                            while(!good) {
                                Proxy prox = null;
                                Objects<Proxy> proxies;
                                int openattemps = 0;
                                while(db == null) {
                                    try {
                                        db = new DB(this.getClass().getName());
                                    } catch(DBException ex) {
                                        // Owch! try again in 5 seconds.
                                        db = null;
                                        openattemps++;
                                        if(openattemps > 10) {
                                            System.out.println(this.getClass().getName() + " No neodatis connection after 10 attempts! FAILING!");
                                            return;
                                        }
                                        try {
                                            Thread.sleep(6000);
                                        } catch(InterruptedException ex1) {
                                            return; // Die!
                                        }
                                    }
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

                        Probe p = new Probe(url, proxyaddress, proxyport, when, b32addr);
                        Thread t = new Thread(p, "Seedless.Probe" + url);
                        t.setDaemon(true);
                        t.start();
                        try {
                            Thread.sleep(10 * 1000); // launch the next one in 10 seconds.
                        } catch(InterruptedException ex) {
                            if(!spin) {
                                return;
                            }
                        }
                    }
                }
            }
            dm = null;
            br.close();
            br = null;
            fi.close();
            fi = null;
        } catch(UnsupportedEncodingException ueex) {
        } catch(FileNotFoundException fnfex) {
        } catch(IOException ioex) {
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch(IOException ex) {
                }
            }
            if(fi != null) {
                try {
                    fi.close();
                } catch(IOException ex) {
                }
            }
            dm = null;
            System.out.println("host scan completed.");
        }
    }
}
*/
