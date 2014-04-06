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
package net.i2p.seedless.classes;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neodatis.odb.NeoDatis;
import org.neodatis.odb.NeoDatisRuntimeException;
import org.neodatis.odb.ODB;

/**
 * Allows one to not worry about closing the database.
 *
 * @author sponge
 */
public class DB {

    private ODB odb = null;
    // private static final int NEODATIS_SERVER_PORT = net.i2p.seedless.Version.NEODATIS_SERVER_PORT;
    private String me = null;

    public DB(String me) throws DBException {
        this.me = new String(me);
        try {
            odb = NeoDatis.openClient(net.i2p.seedless.Version.getNEODATIS_SERVER(), net.i2p.seedless.Version.getNEODATIS_SERVER_PORT(), "Seedlessdb");
        } catch(NeoDatisRuntimeException NDRE) {
            throw new DBException(NDRE);
        }
        try {
            odb.getConfig().setDatabaseCharacterEncoding("UTF-8");
        } catch(UnsupportedEncodingException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ODB getODB() {
        return odb;
    }

    public void close() throws DBException {
        try {
            if(!isClosed()) {
                odb.close();
            }
        } catch(NeoDatisRuntimeException NDRE) {
            throw new DBException(NDRE);
        }
    }

    public boolean isClosed() {
        return odb.isClosed();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if(!odb.isClosed()) {
                System.out.println("Cleaned orphaned ODB instance. Bug in " + me + ".");
                odb.close();
            }
        } finally {
            super.finalize();
        }
    }
}
