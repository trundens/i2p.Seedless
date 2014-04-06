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

import net.i2p.data.Destination;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.i2p.I2PAppContext;
import net.i2p.seedless.data.Base64;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.ScrapeCheck;
import net.i2p.seedless.classes.SeedlessServices;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.Server;
import net.i2p.seedless.reuse.AttachNeoDatis;

/**
 *
 * @author sponge
 */
public class Service extends PageWrap {

    private static final String SCAN = "scan ";
    private static final int SCAN_LEN = SCAN.length();
    private static final String CHECK = "check ";
    private static final int CHECK_LEN = CHECK.length();
    private static final String STAT = "stat ";
    private static final String LOCATE = "locate ";
    private static final int LOCATE_LEN = LOCATE.length();
    private static final String BASE64 = "base64 ";
    private static final int BASE64_LEN = BASE64.length();

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        DB db = null;
        ODB odb = null;
        String info = null;
        boolean scan = false;
        boolean check = false;
        boolean locate = false;
        boolean convert = false;
        boolean stat = false;
        String data[] = null;

        try {
            db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war Service:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
            if(db == null) {
                return;
            }
            odb = db.getODB();
            String STag = request.getHeader("X-Seedless");
            Date now = new Date();
            long date = now.getTime(); // in miliseconds
            SeedlessServices service = null;
            // preen database of any old entries... except seedless
            Objects<SeedlessServices> services = odb.query(SeedlessServices.class, W.le("date", date).and(W.not(W.like("service", "seedless")))).objects();
            while(services.hasNext()) {
                service = services.next();
                odb.delete(service);
                odb.commit();
            }
            if(STag != null) {
                STag = STag.trim();
                try {

                    if(STag.startsWith(SCAN)) {
                        // Request to scan the network for a database object from anywhere.
                        info = Base64.decodeToString(STag.substring(SCAN_LEN), "UTF-8");
                        scan = true;
                    } else if(STag.startsWith(STAT)) {
                        stat = true;
                    } else if(STag.startsWith(CHECK)) {
                        // Request to check status of the scan request.
                        info = Base64.decodeToString(STag.substring(CHECK_LEN), "UTF-8");
                        check = true;
                    } else if(STag.startsWith(LOCATE)) {
                        // Request to locate a database object.
                        info = Base64.decodeToString(STag.substring(LOCATE_LEN), "UTF-8");
                        locate = true;

                    } else if(STag.startsWith(BASE64)) {
                        // Request to locate a database object.
                        info = STag.substring(BASE64_LEN);
                        convert = true;

                    }
                    if(info != null) {
                        data = info.split(" ", 2);
                    }
                } catch(Exception ex) {
                }
            }
            if(stat) {
                // return seedless status
                boolean finder = false;
                boolean monitor = false;
                boolean cache = false;
                boolean haveSeedlessServer = false;
                Boolean ready = false;
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
                Objects<Server> seedlessserver = odb.query(Server.class).objects();
                if(seedlessserver.hasNext()) {
                    haveSeedlessServer = odb.query(SeedlessServices.class, W.equal("service", "seedless")).count().intValue() > 1;
                }

                // And finally, health, which must be > 1 server.

                ready = finder & monitor & cache & haveSeedlessServer;
                response.setHeader("X-Seedless", ready.toString());
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;


            } else if(convert && info.endsWith(".i2p")) {
                // Convert a b32.i2p or named address to the long dest format,
                // and return in X-Seedless.
                // DO NOT set X-Seedless if we can't find it!
                // Close db NOW!!! This can take a while on b32.i2p addresses!!!
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                try {
                    Destination dest = I2PAppContext.getGlobalContext().namingService().lookup(info);
                    String line = dest.toBase64();
                    response.setHeader("X-Seedless", line);
                } catch(NullPointerException npe) {
                    // Could not find the destination!
                    // Perhaps we can set an error code here too.
                    // response.SC_GONE (410) would be an excellent choice.
                    //      * 404 would NOT be a good choice, The actual page is accessable
                    //      * The request was acceptable
                    //      * There is no alternate URI
                }

            } else {
                if(data == null || info == null || data.length != 2) {
                    response.sendError(response.SC_NOT_ACCEPTABLE, "Malformed request.");
                } else {
                    String svc = data[0];
                    String metadata = data[1];
                    String scraper = svc + metadata;
                    if(metadata.length() > 1024) {
                        response.sendError(response.SC_NOT_ACCEPTABLE, "Metadata to long.");
                    } else if(svc.length() > 20) {
                        response.sendError(response.SC_NOT_ACCEPTABLE, "Service name to long.");
                    } else if(scan) {
                        doScan(odb, out, svc, metadata, scraper);
                    } else if(check) {
                        doCheck(odb, out, svc, metadata, scraper);
                    } else if(locate) {
                        doLocate(odb, out, svc, metadata);
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
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Service API to Seedless";
    }

    // Scan the network for something
    private void doScan(ODB odb, PrintWriter out, String svc, String metadata, String scraper) {
        Objects<ScrapeCheck> scraping = odb.query(ScrapeCheck.class, W.equal("id", scraper)).objects();
        if(scraping.isEmpty()) {
            // New request
            ScrapeCheck add = new ScrapeCheck();
            add.find = metadata;
            add.service = svc;
            add.id = scraper;
            add.looking = true;
            add.started = false;
            Date now = new Date();
            add.time = now.getTime() + (10 * 60000l); // 10 minutes
            odb.store(add);
            odb.commit();
        }
    }

    // Check progress of scan
    private void doCheck(ODB odb, PrintWriter out, String svc, String metadata, String scraper) {
        Objects<ScrapeCheck> scraping = odb.query(ScrapeCheck.class, W.equal("id", scraper)).objects();
        if(scraping.isEmpty()) {
            // Error
            out.println("NO SCANNER");
        } else {
            ScrapeCheck scrape = scraping.first();
            if(scrape.looking) {
                // Searching...
                out.println("SCANNING");
            } else {
                // Ready.
                out.println("READY");
            }
        }
    }

    // Query the cache.
    private void doLocate(ODB odb, PrintWriter out, String svc, String metadata) {
        //LinkedList<SeedlessServices> xservices = new LinkedList<SeedlessServices>();
        //Objects<SeedlessServices> services;
        SeedlessServices service;
        metadata = metadata.trim();
        try {
            Objects<SeedlessServices> info = odb.query(SeedlessServices.class, W.equal("service", svc).and(W.like("metadata", metadata)).and(W.equal("good", true))).objects();
            while(info.hasNext()) {
                service = info.next();
                out.println(Base64.encode(service.dest + " " + service.metadata, "UTF-8"));
            }
        } catch(Exception e) {
            e.printStackTrace(out);
        }

    }
}
