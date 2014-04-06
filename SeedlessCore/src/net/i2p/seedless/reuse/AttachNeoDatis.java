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
package net.i2p.seedless.reuse;

import net.i2p.seedless.classes.DB;
import net.i2p.seedless.classes.DBException;

/**
 *
 * @author sponge
 */
public class AttachNeoDatis {

    /**
     * Try 10 times to get a connection to NeoDatis
     * @param app The webapplication
     * @return null on failure, DB on success
     */
    public DB AttachNeodatis(String app) {
        DB db = null;
        int openattempts = 0;
        while(db == null) {
            try {
                db = new DB(this.getClass().getName());
            } catch(DBException ex) {
                // Owch! try again in 5 seconds.
                db = null;
                openattempts++;
                if(openattempts > 10) {
                    System.out.println(app + " ERROR: No neodatis connection after 10 attempts! FAILING!");
                    break;
                }
                try {
                    Thread.sleep(6000);
                } catch(InterruptedException ex1) {
                    break; // Die!
                }
            }
        }
        return db;
    }

    /**
     * Close the database.
     * @param db The DB to close
     */
    public void DetachNeoDatis(DB db) {
        try {
            if(db != null && !db.isClosed()) {
                db.close();
            }
        } catch(Exception ex) {
        }
        db = null;
    }
}
