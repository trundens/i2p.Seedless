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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.ScrapeCheck;
import net.i2p.seedless.classes.SeedlessServices;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class ServiceBrowser extends PageWrap {

    private boolean havehead = false;
    private boolean serverReady = false;
    private String ticks = "\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588\u2593\u2592\u2591\u2680\u2681\u2682\u2683\u2684\u2685 ";

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
        setstat();
        if(serverReady) {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();

            try {
                query(out, "");
                out.println("</body></html>");
            } finally {
                out.close();
            }
        } else {
            response.sendRedirect("/SeedlessConsole/");
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        checklibs();
        setstat();
        if(serverReady) {
            response.setContentType("text/html;charset=UTF-8");
            int srl = 0;
            int lrl = 0;
            int sl = 0;
            int rl = 0;
            String[] svcreq = request.getParameterValues("service");
            String[] lookreq = request.getParameterValues("look");
            String[] searching = request.getParameterValues("searching");
            String[] ready = request.getParameterValues("ready");
            String[] tickIndexs = request.getParameterValues("tickIndex");
            int tickIndex = 0;
            if(tickIndexs != null) {
                tickIndex = Integer.parseInt(tickIndexs[0]);
                tickIndex++;
                if(tickIndex >= (ticks.length() - 1)) {
                    tickIndex = 0;
                }
            }
            PrintWriter out = response.getWriter();
            DB db = null;
            db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war ServiceBrowser:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
            if(db == null) {
                return;
            }
            ODB odb = db.getODB();
            try {
                if(svcreq != null) {
                    srl = svcreq[0].length(); // Catch badness.
                }
                if(lookreq != null) {
                    lrl = lookreq.length; // zero length string OK here
                }
                if(searching != null) {
                    sl = searching[0].length();
                }
                if(ready != null) {
                    rl = ready[0].length();
                }
                if((srl == 0 || lrl == 0) && sl == 0 && rl == 0) {
                    odb = null;
                    try {
                        if(db != null && !db.isClosed()) {
                            db.close();
                        }
                    } catch(Exception ex) {
                    }
                    db = null;
                    doGet(request, response);
                    return;
                } else {
                    if(rl == 0) {
                        if(sl == 0) {
                            // New request, but does it contain an old request that is processing?
                            String svc = svcreq[0];
                            String look = lookreq[0];
                            String scraper = svc + look;

                            Objects<ScrapeCheck> scraping = odb.query(ScrapeCheck.class, W.equal("id", scraper)).objects();
                            if(scraping.isEmpty()) {
                                // New request
                                ScrapeCheck add = new ScrapeCheck();
                                add.find = look;
                                add.service = svc;
                                add.id = scraper;
                                add.looking = true;
                                add.started = false;
                                Date now = new Date();
                                add.time = now.getTime() + (10 * 60000l); // 10 minutes

                                odb.store(add);
                                odb.commit();
                            }   // and fall thru as an old request.
                            // Old request
                            onLoad(out, "searching", scraper, tickIndex);

                        } else {
                            Objects<ScrapeCheck> scraping = odb.query(ScrapeCheck.class, W.equal("id", searching[0])).objects();
                            if(scraping.isEmpty()) {
                                // error
                                odb = null;
                                (new AttachNeoDatis()).DetachNeoDatis(db);
                                db = null;
                                doGet(request, response);
                                return;
                            } else {
                                ScrapeCheck scrape = scraping.first();
                                if(scrape.looking) {
                                    // still processing
                                    onLoad(out, "searching", searching[0], tickIndex);
                                } else {
                                    // Ready.
                                    onLoad(out, "ready", searching[0], tickIndex);
                                }

                            }
                        }
                    } else {
                        // Stuff found
                        Objects<ScrapeCheck> scraping = odb.query(ScrapeCheck.class, W.equal("id", ready[0])).objects();
                        if(scraping.isEmpty()) {
                            // error
                            odb = null;
                            (new AttachNeoDatis()).DetachNeoDatis(db);
                            db = null;
                            doGet(request, response);
                            return;
                        } else {
                            head(out);
                            out.println("<h1>ServiceBrowser Results</h1>");
                            // query database, then clean out our request.
                            ScrapeCheck scrape = scraping.first();
                            Objects<SeedlessServices> info = odb.query(SeedlessServices.class, W.equal("service", scrape.service).and(W.like("metadata", scrape.find))).objects();
                            if(info.isEmpty()) {
                                // No results
                                odb = null;
                                (new AttachNeoDatis()).DetachNeoDatis(db);
                                db = null;
                                query(out, "<P>No results for '" + scrape.service + ":" + scrape.find + "'</P><HR>");
                            } else {
                                SeedlessServices service = null;
                                // Show results
                                boolean UB = false;
                                if(scrape.service.equals("eepsite")) {
                                    UB = true;
                                }
                                out.println("<TABLE FRAME='box' BORDER='2'><TR><TH>Address</TH><TH>Metadata</TH></TR>");
                                while(info.hasNext()) {
                                    service = info.next();
                                    if(UB) {
                                        out.println("<TR><TD><A HREF='http://" + service.dest + "/'>" + service.dest + "</A></TD><TD>" + stringToHTMLString(service.metadata) + "</TD></TR>");
                                    } else {
                                        out.println("<TR><TD>" + service.dest + "</TD><TD>" + stringToHTMLString(service.metadata) + "</TD></TR>");
                                    }
                                }
                                out.print("</TABLE>");
                            }
                        }
                    }
                    out.println("</body></html>");
                }
            } finally {
                odb = null;
                (new AttachNeoDatis()).DetachNeoDatis(db);
                db = null;
                out.close();
            }
        } else {
            response.sendRedirect("/SeedlessConsole/");
        }

    }

    private static String stringToHTMLString(String string) {
        StringBuffer sb = new StringBuffer(string.length());
        // true if last char was blank
        boolean lastWasBlankChar = false;
        int len = string.length();
        char c;

        for(int i = 0; i < len; i++) {
            c = string.charAt(i);
            if(c == ' ') {
                // blank gets extra work,
                // this solves the problem you get if you replace all
                // blanks with &nbsp;, if you do that you lose
                // word breaking
                if(lastWasBlankChar) {
                    lastWasBlankChar = false;
                    sb.append("&nbsp;");
                } else {
                    lastWasBlankChar = true;
                    sb.append(' ');
                }
            } else {
                lastWasBlankChar = false;
                //
                // HTML Special Chars
                if(c == '"') {
                    sb.append("&quot;");
                } else if(c == '&') {
                    sb.append("&amp;");
                } else if(c == '<') {
                    sb.append("&lt;");
                } else if(c == '>') {
                    sb.append("&gt;");
                } else if(c == '\n') // Handle Newline
                {
                    sb.append("<br />");
                } else {
                    int ci = 0xffff & c;
                    if(ci < 160) // nothing special only 7 Bit
                    {
                        sb.append(c);
                    } else {
                        // Not 7 Bit use the unicode system
                        sb.append("&#");
                        sb.append(new Integer(ci).toString());
                        sb.append(';');
                    }
                }
            }
        }
        return sb.toString();
    }

    private void head(PrintWriter out) {
        out.println("<html><head><title>Seedless ServiceBrowser</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body>");
        havehead = true;
    }

    private void head(PrintWriter out, int dly, String name) {
        out.println("<html><head><title>Seedless ServiceBrowser</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
        out.println("<script type='text/javascript'>");
        out.println("function doSomething()");
        out.println("{");
        out.println("var t=setTimeout('" + name + ".submit()'," + dly * 1000 + ");");
        out.println("}");
        out.println("</script>");

        out.println("</head>");
        out.println("<body onload='doSomething()'>");
        havehead = true;
    }

    private void onLoad(PrintWriter out, String form, String value, int tickIndex) {
        head(out, 10, form);
        out.println("<h1>ServiceBrowser</h1><p>Searching " + ticks.substring(tickIndex, tickIndex + 1) + "</p><P>This page should refresh automatically.");
        out.println("<FORM METHOD='POST' ACTION='/SeedlessConsole/ServiceBrowser' name='" + form + "'><INPUT TYPE='hidden' NAME='" + form + "' VALUE='" + value + "'><INPUT TYPE='hidden' NAME='tickIndex' VALUE='" + tickIndex + "'><P>Click <INPUT TYPE='submit' VALUE='this'> if the page does not refresh.</P></FORM>");
    }

    private void query(PrintWriter out, String message) {
        ODB odb = null;
        Objects<SeedlessServices> services;
        SeedlessServices service;
        String serv;
        DB db = null;
        db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war ServiceBrowser:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
        if(db == null) {
            return;
        }
        odb = db.getODB();
        Date now = new Date();
        long date = now.getTime(); // in miliseconds
        // preen database of any old entries... except seedless
        services = odb.query(SeedlessServices.class, W.le("date", date).and(W.not(W.like("service", "seedless")))).objects();
        while(services.hasNext()) {
            service = services.next();
            odb.delete(service);
            odb.commit();
        }
        try {
            if(!havehead) {
                out.println("<html><head><title>Seedless ServiceBrowser</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
            }
            // get a listing of services from the seedless server entries, and weed out seedless
            // build list
            services = odb.query(SeedlessServices.class, W.equal("service", "seedless")).objects();
            if(services.hasNext()) {
                if(!havehead) {
                    out.println("</head><body>");
                }
                out.println("<h1>Browser Query</h1>");
                out.println("<FORM METHOD='POST' ACTION='/SeedlessConsole/ServiceBrowser' accept-charset='UTF-8'>");
                Set<String> ss = new TreeSet();
                out.print("<P><select name='service'>");
                out.print("<OPTION value=''>Choose Something</OPTION>");
                while(services.hasNext()) {
                    service = services.next();
                    ss.addAll(Arrays.asList(service.metadata.split(",")));
                }
                Iterator<String> it = ss.iterator();
                while(it.hasNext()) {
                    serv = it.next();
                    if(!(serv.length() > 20 || serv.length() == 0 || serv.contains("seedless"))) {
                        out.print("<OPTION value='" + serv + "'>" + serv + "</OPTION>");
                    }
                }
                out.println("</select></P>");
                out.println("<P>Search for: <INPUT TYPE='text' NAME='look' VALUE='' SIZE=44 MAXLENGTH=50></P>");
                out.println("<P><INPUT TYPE='submit' VALUE='Submit' ></P>");
                out.println("</FORM>");
                if(!(message.length() == 0)) {
                    out.println("<HR>" + message);
                }
            } else {
                if(!havehead) {
                    out.println("<meta http-equiv='refresh' content='60'>");
                    out.println("</head><body>");
                }
                out.println("No Service yet. Please wait.");
            }

        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
    }

    private void setstat() {
        DB db = null;
        ODB odb = null;
        boolean finder = false;
        boolean monitor = false;
        boolean cache = false;
        havehead = false;
        try {
            try {
                db = new DB(this.getClass().getName());
                odb = db.getODB();
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
                serverReady = finder & monitor & cache;
            } catch(Exception ex) {
            }
        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Service Browser";
    }
}
