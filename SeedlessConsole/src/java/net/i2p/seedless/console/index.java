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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;
import net.i2p.seedless.classes.TunnelData;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.Server;
import net.i2p.seedless.classes.SeedlessServices;
import net.i2p.seedless.classes.TorrentSearch;
import net.i2p.seedless.classes.eep;
import net.i2p.seedless.reuse.AttachNeoDatis;

/**
 *
 * @author sponge
 */
public class index extends PageWrap {

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        checklibs();
        DB db = null;
        ODB odb = null;
        int i = 0;
        String serviceNames[] = new String[0];
        int serviceNameCounts[] = new int[0];
        int count = 0;
        String SeedlessServerVersion = null;
        Boolean haveSeedlessServer = false;
        Boolean finder = false;
        Boolean monitor = false;
        Boolean cache = false;
        Boolean ready = false;
        String eepvers[] = new String[0];
        Boolean goteep = false;
        Boolean body = false;

        PrintWriter out = response.getWriter();
        response.setContentType("text/html;charset=UTF-8");
        out.print("\n<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
        out.print("\n<html>\n\t");
        try {
            try {
                db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war index:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    return;
                }
                odb = db.getODB();
                Date now = new Date();
                long date = now.getTime(); // in miliseconds
                SeedlessServices service = null;
                try {                // preen database of any old entries... except seedless
                    Objects<SeedlessServices> services = odb.query(SeedlessServices.class, W.le("date", date).and(W.not(W.like("service", "seedless")))).objects();
                    while(services.hasNext()) {
                        service = services.next();
                        odb.delete(service);
                        odb.commit();
                    }
                } catch(Exception ex) {
                }

                try {

                    Objects<Finder> finderserver = odb.query(Finder.class).objects();
                    if(finderserver.hasNext()) {
                        Finder zap = finderserver.first();
                        finder = zap.isRunning();
                    }
                } catch(Exception ex) {
                }

                try {
                    Objects<Tunnel> monitorserver = odb.query(Tunnel.class).objects();
                    if(monitorserver.hasNext()) {
                        Tunnel zap = monitorserver.first();
                        monitor = zap.isRunning();
                    }
                } catch(Exception ex) {
                }

                try {
                    Objects<Cache> cacheserver = odb.query(Cache.class).objects();
                    if(cacheserver.hasNext()) {
                        Cache zap = cacheserver.first();
                        cache = zap.isRunning();
                    }
                } catch(Exception ex) {
                }

                try {
                    Objects<Server> seedlessserver = odb.query(Server.class).objects();
                    if(seedlessserver.hasNext()) {
                        Server got = seedlessserver.first();
                        haveSeedlessServer = got.getServer();
                        SeedlessServerVersion = got.getVERSION();
                    }
                } catch(Exception ex) {
                }

                try {
                    Objects<eep> eeps = odb.query(eep.class).objects();
                    TreeSet<String> clean = new TreeSet();
                    while(eeps.hasNext()) {
                        eep e = eeps.next();
                        try {
                            if(e.getB32().length() != 0 && e.getVERSION().length() != 0) {
                                goteep = true;
                                clean.add(e.VERSION);
                            }
                        } catch(Exception ex) {
                        }
                    }
                    eepvers = clean.toArray(new String[clean.size()]);
                } catch(Exception ex) {
                }

            } catch(Exception ex) {
                // we do not care...
            }
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;

            try {
                db = new DB(this.getClass().getName());
                odb = db.getODB();
                // how many seedless servers
                count = odb.query(SeedlessServices.class, W.equal("service", "seedless")).count().intValue();

                // Get service names and tally counts for each... this is horrible!
                // Neodatis should perform this for us!
                //
                // Not fucking implemented...
                // Values v = odb.queryValues(SeedlessServices.class).field("service").values();
                //

                Objects<SeedlessServices> stuff = odb.query(SeedlessServices.class).objects();
                TreeSet<String> clean = new TreeSet();
                while(stuff.hasNext()) {
                    clean.add(stuff.next().service);
                }
                serviceNames = clean.toArray(new String[clean.size()]);

                serviceNameCounts = new int[serviceNames.length];
                for(i = 0; i < serviceNames.length; i++) {
                    serviceNameCounts[i] = odb.query(SeedlessServices.class, W.equal("service", serviceNames[i])).count().intValue();
                }
            } catch(Exception ex) {
                // we do not care...
            }
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
            ready = finder & monitor & cache & (count > 0);
            String warning = "OK";
            int rft = 600; // 10 minutes.
            if(count < 3) {
                warning = "LOW SERVER COUNT.";
            }
            if(count == 0) {
                warning = "NO SERVICE YET.";
            }
            if(count < 3 || !ready) {
                rft = 60; // 1 minute.
            }

            if(!ready) {
                warning = "Starting up";
                if(monitor) {
                    if(!finder && !cache) {
                        warning = "Starting up, waiting for http proxy";
                    }
                }
            }

            out.print("<head>\n\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n\t\t<meta http-equiv=\"refresh\" content=\"" + rft + "\">\n\t\t<title>SeedlessConsole</title>\n\t</head>\n\t<body>\n\t\t");
            body = true;
            if(ready) {
                out.print("<A HREF=\"/SeedlessConsole/Eepsites\">unpublished eepsites list</A><BR>");
                out.print("<HR>Diagnostics:<BR>");
                out.print("<A HREF=\"/SeedlessConsole/ServiceBrowser\">ServiceBrowser</A><BR>");
                out.print("<A HREF=\"/SeedlessConsole/CacheBrowser\">CacheBrowser</A>");
            } else {
                out.print("Starting up...");
            }
            out.print("\n\t\t<HR>\n\t\t<p><h3>Version Information:</h3></p>\n\t\t<HR>\n\t\t<pre>\n");

            if(goteep) {
                for(i = 0; i < eepvers.length; i++) {
                    out.println("EepAnnouncer version: " + eepvers[i]);
                }
            }
            if(haveSeedlessServer) {
                if(SeedlessServerVersion != null) {
                    out.println("SeedlessServer version: " + SeedlessServerVersion);
                }
            }
            out.println("SeedlessConsole version: " + net.i2p.seedless.console.Version.getVersion());
            out.println("SeedlessCore version: " + net.i2p.seedless.Version.getVersion());
            out.println("Neodatis ODB version: " + org.neodatis.odb.Release.RELEASE_NUMBER + "-" + org.neodatis.odb.Release.RELEASE_BUILD + " (" + org.neodatis.odb.Release.RELEASE_DATE + ") PatchLevel " + org.neodatis.odb.PatchLevel.patchLevel);
            out.print("\t\t</pre>\n\t\t<HR>\n\t\t<p><h3>Status</h3></p>\n\t\t<p>Ready: " + ready);
            out.print("</p>\n\t\t<p>Finder: " + finder);
            out.print("</p>\n\t\t<p>Monitor: " + monitor);
            out.print("</p>\n\t\t<p>Cache: " + cache);
            out.print("</p>\n\t\t<p>Seedless Servers known: " + count);
            out.print("</p>\n\t\t<p>Node Health: " + warning + "</p>");
            if(haveSeedlessServer) {
                out.print("\n\t\t<p>Server is running. ");
                if(serviceNames.length < 3) {
                    if(serviceNames.length < 2 || serviceNames[1].equals("eepsite")) {
                        // Make a massive point to configure the server, and use blink to be super annoying.
                        out.print("</p><p><H1><blink>WARNING: YOU HAVE NOT CONFIGURED YOUR SERVER PROPERLY! PLEASE READ THE DOCUMENTATION!</blink></H1>");
                        out.print("</p><p><H1>This warning will disapear once you have properly configured your <a href='http://sponge.i2p/files/seedless/doc/how-to-seedless.html#server'>server</a>.</H1>");
                    }
                }
            } else {
                if(count > 1 && ready) {
                    out.print("\n\t\t<p>Server is not running yet or not installed.");
                    if(count < 20) {
                        out.print("</p><p><h1>Please consider <a href='http://sponge.i2p/files/seedless/doc/how-to-seedless.html#helping'>helping out</a> by running a server!</h1>");
                    }
                }
            }
            out.print("</p>");

            // Cached serviceNameCounts
            out.print("\n\t\t<HR>\n\t\t<p><h3>Services cached</h3></p>\n\t\t");

            for(i = 0; i < serviceNames.length; i++) {
                out.print("<p>" + serviceNames[i] + ": " + serviceNameCounts[i]);
                if(haveSeedlessServer && serviceNames[i].equalsIgnoreCase("torrent")) {
                    try {
                        db = new DB(this.getClass().getName());
                        odb = db.getODB();
                        Objects<TorrentSearch> ts = odb.query(TorrentSearch.class, W.not(W.equal("code", 200)).and(W.not(W.equal("code", 403))).and(W.not(W.equal("code", 9)))).objects();
                        int notresolved = ts.size();
                        ts = odb.query(TorrentSearch.class).objects();
                        int alltorrents = ts.size();
                        int completed = alltorrents - notresolved;
                        ts = null;
                        out.print(", Total: " + alltorrents + ", Indexed: " + completed  + ", Remaining: " + notresolved);
                    } catch(Exception ex) {
                    } finally {
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                    }
                }
                out.print("</p>\n\t\t");
            }

            // tunnels monitored...
            try {
                db = new DB(this.getClass().getName());
                odb = db.getODB();
                Objects<TunnelData> monitored = odb.query(TunnelData.class).objects();
                if(monitored.hasNext()) {
                    TunnelData tunnel;

                    out.print("<HR>\n\t\t<p><h3>I2PTunnels Monitored</h3></p>\n\t\t<TABLE FRAME=\"box\" BORDER=\"2\">\n\t\t\t<TR><TH>Running?</TH><TH>Base 32</TH><TH>in From I2P</TH><TH>out To I2P</TH></TR>\n\t\t\t");

                    while(monitored.hasNext()) {
                        tunnel = monitored.next();
                        out.print("\n\t\t\t<TR><TD>" + tunnel.up);
                        out.print("</TD><TD>" + tunnel.base32);
                        out.print("</TD><TD>" + tunnel.inFromI2P);
                        out.print("</TD><TD>" + tunnel.outToI2P);
                        out.print("</TD></TR>\n\t\t\t");
                    }

                    out.print("\n\t\t</TABLE>\n");

                }
            } catch(Exception ex) {
                if(!body) {
                    out.print("<body>\n\t");
                }
                out.println("\n<PRE>");
                ex.printStackTrace(out);
                out.println("\n</PRE>");
            }
        } finally {
            if(body) {
                out.print("\n\t</body>\n</html>\n");
            }
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
            out.close();
        }
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Index page";
    }
}
