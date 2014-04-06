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
import java.util.Iterator;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.SeedlessServices;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class CacheBrowser extends PageWrap {

    private boolean havehead = false;
    private boolean serverReady = false;

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
        setstat();
        if(serverReady) {
            response.setContentType("text/html;charset=UTF-8");
            int srl = 0;
            int lrl = 0;
            String[] svcreq = request.getParameterValues("service");
            String[] lookreq = request.getParameterValues("look");
            PrintWriter out = response.getWriter();
            DB db = null;
            db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war CacheBrowser:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
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
                if((srl == 0 || lrl == 0)) {
                    odb = null;
                    (new AttachNeoDatis()).DetachNeoDatis(db);
                    db = null;
                    doGet(request, response);
                } else {
                    head(out);
                    out.println("<h1>CacheBrowser Results</h1>");
                    // query database, then clean out our request.
                    Objects<SeedlessServices> info = odb.query(SeedlessServices.class, W.equal("service", svcreq[0]).and(W.like("metadata", lookreq[0]))).objects();
                    if(info.isEmpty()) {
                        // No results
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                        query(out, "<P>No results for '" + svcreq[0] + ":" + lookreq[0] + "'</P><HR>");
                    } else {
                        SeedlessServices service = null;
                        // Show results
                        boolean UB = false;
                        if(svcreq[0].equals("eepsite")) {
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
                        out.println("</table>");
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
        if(!havehead) {
            out.println("<html><head><title>Seedless CacheBrowser</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body>");
        }
        havehead = true;
    }

    private void query(PrintWriter out, String message) {
        ODB odb = null;
        Objects<SeedlessServices> services;
        SeedlessServices service;
        String serv;
        DB db = null;
        db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war CacheBrowser:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
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
                out.println("<html><head><title>Seedless CacheBrowser</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
            }
            // get a listing of services from the seedless services entries
            // build list
            services = odb.query(SeedlessServices.class, W.equal("service", "seedless")).objects();
            if(services.hasNext()) {
                if(!havehead) {
                    out.println("</head><body>");
                }
                // Not fucking implemented here either!
                //   Set<String> ss = new TreeSet(odb.queryValues(SeedlessServices.class).field("service").values());
                Objects<SeedlessServices> stuff = odb.query(SeedlessServices.class).objects();
                TreeSet<String> ss = new TreeSet();
                while(stuff.hasNext()) {
                    ss.add(stuff.next().service);
                }

                Iterator<String> it = ss.iterator();
                // Remove anything evil.
                while(it.hasNext()) {
                    serv = it.next();
                    if(serv.length() > 20 || serv.length() == 0) {
                        ss.remove(serv);
                    }
                }
                it = ss.iterator();
                out.println("<h1>Browser Query</h1>");
                out.println("<FORM METHOD='POST' ACTION='/SeedlessConsole/CacheBrowser' accept-charset='UTF-8'>");

                out.print("<P><select name='service'>");
                out.print("<OPTION value=''>Choose Something</OPTION>");
                while(it.hasNext()) {
                    serv = it.next();
                    out.print("<OPTION value='" + serv + "'>" + serv + "</OPTION>");
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
        } catch(Exception ex) {
            out.println("\n<PRE>");
            ex.printStackTrace(out);
            out.println("\n</PRE>");

        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
    }

    private void setstat() throws ServletException {
        checklibs();
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
            try {
                if(db != null && !db.isClosed()) {
                    db.close();
                }
            } catch(Exception ex) {
            }
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
