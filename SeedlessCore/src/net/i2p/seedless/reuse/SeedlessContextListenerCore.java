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
import net.i2p.seedless.classes.TunnelData;
import org.neodatis.odb.NeoDatisConfig;
import org.neodatis.odb.NeoDatisGlobalConfig;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class SeedlessContextListenerCore {

    /**
     * Test b32 address and test NeoDatis
     * @param me
     * @return null on failure or TunnelData object on success.
     */
    public TunnelData testB32NeoDatis(String me, String app) throws DBException {
        ODB odb = null;
        DB db = null;
        Objects<TunnelData> tunnels;
        TunnelData tunnel = null;

        try {
            out:
            {
                if(me == null) {
                    System.out.println(app + " ERROR: i2p.b32 address was not found, please see the documentation.");
                    break out;
                } else {
                    try {
                        NeoDatisConfig X = NeoDatisGlobalConfig.get();
                    } catch(NoClassDefFoundError e) {
                        System.out.println(app + " ERROR:Can't locate NeoDatis ODB, did you forget to install or upgrade the Neodatis plugin?");
                        break out;
                    }
                }
                // if we get to here, the b32 address is good, and neodatis jar is accessable.
                db = (new AttachNeoDatis()).AttachNeodatis(Thread.currentThread().getName() + " SeedlessContextListenerCore:"  + Thread.currentThread().getStackTrace()[1].getLineNumber());
                if(db == null) {
                    break out;
                }
                odb = db.getODB();
                tunnels = odb.query(TunnelData.class, W.equal("base32", me)).objects();
                if(tunnels.hasNext()) {
                    tunnel = tunnels.next();
                    odb = null;
                    db.close();
                    db = null;
                //}
                //else {
                //    System.out.println(app + " ERROR: context NOT initialized, no tunnel found for B32 address " + me);
                }
            }
        } catch(DBException ex) {
            tunnel = null;
            throw new DBException(ex);
            // System.out.println(app + " ERROR: context NOT initialized, DBException.");
        } finally {
            odb = null;
            (new AttachNeoDatis()).DetachNeoDatis(db);
            db = null;
        }
        return tunnel;
    }

    public void zap(Thread t, String app) {
        int loops = 0;
        while(t.isAlive() && loops < 60) {
            t.interrupt();
            try {
                t.join(1000);
            } catch(InterruptedException ex) {
            }
            loops++;
        }
        if(loops < 60) {
            System.out.println(app + " context destroyed, thread stopped. loops=" + loops);
        } else {
            System.out.println(app + " ERROR: context could not stop threads.");
        }
    }
}
