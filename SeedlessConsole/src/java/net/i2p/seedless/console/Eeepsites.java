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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.i2p.seedless.classes.Cache;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.Finder;
import net.i2p.seedless.classes.Tunnel;
import net.i2p.seedless.classes.SeedlessServices;
import net.i2p.seedless.classes.Server;
import net.i2p.seedless.reuse.AttachNeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class Eeepsites extends PageWrap {

    private boolean serverReady = false;
    private Boolean haveSeedlessServer = false;

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
                query(out);
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
        response.sendRedirect("/SeedlessConsole/");
    // TO-DO: page for large listings.
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

    private void query(PrintWriter out) {
        ODB odb = null;
        Objects<SeedlessServices> services;
        SeedlessServices service;
        String serv;
        DB db = null;
        db = (new AttachNeoDatis()).AttachNeodatis("SeedlessConsole.war Eeepsites:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
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
            out.println("<html><head><title>Seedless eepsite list.</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
            // get a listing of services from the seedless services entries
            // build list
            services = odb.query(SeedlessServices.class, W.equal("service", "seedless")).objects();
            if(services.hasNext()) {
                boolean goteep = false;
                // Not fucking implemented here either!
                //   Set<String> ss = new TreeSet(odb.queryValues(SeedlessServices.class).field("service").values());
                Objects<SeedlessServices> stuff = odb.query(SeedlessServices.class).objects();
                //TreeSet<String> ss = new TreeSet();
                while(stuff.hasNext()) {
                    if(stuff.next().service.equals("eepsite")) {
                        goteep = true;
                        break;
                    }
                }

                Objects<SeedlessServices> info = odb.query(SeedlessServices.class, W.equal("service", "eepsite")).objects();

                if(goteep) {
                    if(info.isEmpty()) {
                        // No results
                        odb = null;
                        (new AttachNeoDatis()).DetachNeoDatis(db);
                        db = null;
                        if(haveSeedlessServer) {
                            out.println("</head><body>");
                            out.println("<P>None known yet.</P><HR>");
                        } else {
                            // Redirect to the service browser url, as a faux post operation.
                            out.print("<script type='text/javascript'>\nfunction doSomething()\n{\nvar t=setTimeout('searching.submit()',10000);\n}\n</script>\n</head><body onload='doSomething()'><FORM METHOD='POST' ACTION='/SeedlessConsole/ServiceBrowser' name='searching'><INPUT TYPE='hidden' name='service' VALUE='eepsite'><INPUT TYPE='hidden' NAME='look' VALUE=''><P>Click <INPUT TYPE='submit' VALUE='this'> if the page does not refresh.</P></FORM></body></html>");
                        }
                    } else {
                        service = null;
                        // Show results
                        out.println("<TABLE FRAME='box' BORDER='2'><TR><TH>Address</TH><TH>Metadata</TH></TR>");
                        while(info.hasNext()) {
                            service = info.next();
                            out.println("<TR><TD><A HREF='http://" + service.dest + "/'>" + service.dest + "</A></TD><TD>" + stringToHTMLString(service.metadata) + "</TD></TR>");
                        }
                        out.println("</table>");
                    }
                    out.println("</body></html>");
                } // else ... hummm...
            } else {
                out.println("<meta http-equiv='refresh' content='60'>");
                out.println("</head><body>");
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
                if(serverReady) {
                    Objects<Server> seedlessserver = odb.query(Server.class).objects();
                    if(seedlessserver.hasNext()) {
                        Server got = seedlessserver.first();
                        haveSeedlessServer = got.getServer();
                    }
                }
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
        return "Eepsite Browser";
    }
}
