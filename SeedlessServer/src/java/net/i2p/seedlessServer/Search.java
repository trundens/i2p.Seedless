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
package net.i2p.seedlessServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.DBException;
import net.i2p.seedless.classes.SeedlessException;
import net.i2p.seedless.reuse.AttachNeoDatis;
import net.i2p.seedless.server.SeedlessServerCore;
import org.neodatis.odb.ODB;

/**
 *
 * @author sponge
 */
public class Search extends HttpServlet implements Servlet {

    public Search() {
        super();
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean rss = false;
        try {
            rss = request.getParameter("rss").toLowerCase().contains("true");
        } catch(NullPointerException exx) {
        }
        if(rss) {
            response.setContentType("text/xml;charset=UTF-8");
        } else {
            response.setContentType("text/html;charset=UTF-8");
        }
        PrintWriter out = response.getWriter();

        DB db = null;
        ODB odb = null;
        try {
            db = (new AttachNeoDatis()).AttachNeodatis("Seedless.war Search:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
            if(db != null) {
                odb = db.getODB();
                Map<String, String[]> stuff = request.getParameterMap();
                try {
                    SeedlessServerCore core = new SeedlessServerCore(request.getQueryString(), stuff, out, odb, rss, this.getServletContext().getAttribute("i2p.b32").toString());
                    core.ProcessSearch();
                } catch(SeedlessException ex) {
                    response.sendError(response.SC_NOT_ACCEPTABLE, ex.getMessage());
                }
                odb = null;
                db.close();
                db = null;
            }
        } catch(DBException ex) {
            throw new ServletException(ex);
        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            out.close();
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
        processRequest(request, response);
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
        response.sendError(response.SC_FORBIDDEN);
    }

    /**
     * Handles the HTTP <code>HEAD</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(response.SC_FORBIDDEN);
    }

    /**
     * Handles the HTTP <code>HEAD</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(response.SC_FORBIDDEN);
    }

    /**
     * Handles the HTTP <code>HEAD</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(response.SC_FORBIDDEN);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Seedless";
    }
}
