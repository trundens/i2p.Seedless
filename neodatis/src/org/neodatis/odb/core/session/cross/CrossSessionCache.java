/**
NeoDatis ODB : Native Object Database (odb.info@neodatis.org)
Copyright (C) 2007 NeoDatis Inc. http://www.neodatis.org

"This file is part of the NeoDatis ODB open source object database".

NeoDatis ODB is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

NeoDatis ODB is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Contributors: 	Marcelo Mayworm - initial API and implementation
 */
package org.neodatis.odb.core.session.cross;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.neodatis.odb.NeoDatisRuntimeException;
import org.neodatis.odb.OID;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.tool.wrappers.map.OdbHashMap;

/**
 * A cache that survives the sessions. It is uses to automatically reconnect object to sessions
 * 
 * <pre>
 * When active, the cross session cache keeps track of all objects and their OID. It contains a map<Object,OID>.
 * When objects are stored or deleted in NeoDatis, if cross session cache is on, NeoDatis check if the object is in the cross session cache
 * If it is, the object is then added to the session cache (this is the reconnection).
 * 
 * When an object is deleted from the database, the object is also removed from the cross session cache. When the deleted is done by OID, it is not 
 * immediately deleted, instead, it is inserted in a map of deleted object (as it would be very expensive to iterate throw all oids to find the right one.
 * 
 * Issues:
 * 	
 * 1) when deleting object using cross session cache : as the cross session cache is static, it is not session dependent, when deleting an object
 * 	from the cross session cache, it will be seen as deleted by all the transactions even before committed. This is bad. 
 *  
 * 2) as cross session cache is static, the following case can happen:
 * 	- create a NeoDatis database test1.neodatis. Create an object o1 and it. delete the database. re-create the database with the same name and store the o1 object.
 * as o1 is the cross session cache, NeoDatis will try to reconnect the object and will fail as the OID won't exist.
 * see for more details 
 * 
 * </pre>
 * @author mayworm,olivier
 * @sharpen.ignore
 *
 */
public class CrossSessionCache implements ICrossSessionCache {

    /**
     * The cache for NeoDatis OID. This cache supports a weak reference and it is
     * sync
     */
    private final Map<Object, OID> objects;
    /** When objects are deleted by oid, the cost is too high to search the object by the oid, so we just keep the deleted oid,
     * and when looking for an object, check if the oid if is the deleted oids, if yes, return null and delete the object
     */
    private final Map<OID, OID> deletedOids;
    /**
     * To keep track of all caches
     */
    private final static Map<String, ICrossSessionCache> instances = new OdbHashMap<String, ICrossSessionCache>();

    /**
     * Protected constructor for factory-based construction
     *
     */
    protected CrossSessionCache() {
        objects = Collections.synchronizedMap(new WeakHashMap<Object, OID>());
        deletedOids = new OdbHashMap<OID, OID>();
    }

    /**
     * Gets the unique instance for the cache for the identification
     */
    public static ICrossSessionCache getInstance(String baseIdentification) {
        ICrossSessionCache cache = instances.get(baseIdentification);
        if(cache == null) {
            synchronized(instances) {
                cache = new CrossSessionCache();
                instances.put(baseIdentification, cache);
            }
        }
        return cache;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.neodatis.odb.core.transaction.ICacheObjectConnected#addObject(java
     * .lang.Object, org.neodatis.odb.OID)
     */
    public void addObject(Object o, OID oid) {
        if(o == null) {
            //DLogger.info(String.format("Added null object in cross cache session"));
            return;
        // throw new
        // ODBRuntimeException(NeoDatisError.CACHE_NULL_OBJECT.addParameter(object));
        }
        try {
            //DLogger.info(String.format("Adding object %s of type %s with oid=%s and hc=%s",o.toString(),o.getClass().getName(),oid.toString(),System.identityHashCode(o)));
            synchronized(objects) {
                objects.put(o, oid);
            }

        } catch(NullPointerException e) {
            ///DLogger.error(OdbString.exceptionToString(e, true));
            // FIXME URL in HashMap What should we do?
            // In some case, the object can throw exception when added to the
            // cache
            // because Map.put, end up calling the equals method that can throw
            // exception
            // This is the case of URL that has a transient attribute handler
            // that is used in the URL.equals method
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neodatis.odb.core.transaction.ICacheObjectConnected#clear()
     */
    public void clear() {
        synchronized(objects) {
            objects.clear();
            deletedOids.clear();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.neodatis.odb.core.transaction.ICacheObjectConnected#existObject(java
     * .lang.Object)
     */
    public boolean existObject(Object object) {
        synchronized(objects) {
            OID oid = objects.get(object);
            if(oid == null) {
                return false;
            }
            // Then check if oid is in the deleted oid list
            if(deletedOids.containsKey(oid)) {
                // The object has been marked as deleted
                // removes it from the cache
                objects.remove(object);
                deletedOids.remove(oid);
                return false;
            }
            return true;
        }
    }

    public boolean slowExistObject(Object object) {
        synchronized(objects) {
            Iterator iterator = objects.keySet().iterator();

            int i = 1;
            while(iterator.hasNext()) {
                Object o = iterator.next();
                boolean b = object == o;
                //DLogger.info(String.format("slowExist.Checking if object %s(%d) matched object %s(%d) : ==? %b",o.toString(),System.identityHashCode(o),object.toString(), System.identityHashCode(object),b));
                if(b) {
                    return b;
                }
                i++;
            }
            return false;
        }
    }
    /*
     * (non-Javadoc)
     *
     * @see
     * org.neodatis.odb.core.transaction.ICacheObjectConnected#getOid(java.lang
     * .Object, boolean)
     */

    public OID getOid(Object object) {
        synchronized(objects) {
            if(object == null) {
                throw new NeoDatisRuntimeException(NeoDatisError.CACHE_NULL_OBJECT.addParameter(object));
            }

            OID oid = objects.get(object);

            if(oid != null) {
                //DLogger.info(String.format("Object %s exist in cross session cache : OID = %s",  object.toString(), String.valueOf(oid)));
                if(deletedOids.containsKey(oid)) {
                    //DLogger.info(String.format("Object %s exist in cross session cache but is marked as deleted", object.toString()));
                    // The object has been marked as deleted
                    // removes it from the cache
                    objects.remove(object);
                    deletedOids.remove(oid);
                    return null;
                }
                return oid;
            }
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.neodatis.odb.core.transaction.ICacheObjectConnected#isEmpty()
     */
    public boolean isEmpty() {
        synchronized(objects) {
            return objects.isEmpty();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.neodatis.odb.core.transaction.ICacheObjectConnected#removeObject(
     * java.lang.Object)
     */
    public void removeObject(Object object) {
        synchronized(objects) {
            if(object == null) {
                throw new NeoDatisRuntimeException(NeoDatisError.CACHE_NULL_OBJECT.addParameter(" while removing object from the cache"));
            }
            OID oid = objects.remove(object);
            if(oid != null) {
                // Add the oid to deleted oid
                // see junit org.neodatis.odb.test.fromusers.gyowanny_queiroz.TestBigDecimal.test13
                deletedOids.put(oid, oid);
            }
        }
    }

    public void removeOid(OID oid) {
        deletedOids.put(oid, oid);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.neodatis.odb.core.transaction.ICacheObjectConnected#size()
     */
    public int size() {
        synchronized(objects) {
            return objects.size();
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        synchronized(objects) {
            StringBuffer b = new StringBuffer(String.format("Cross session cache with %d objects", objects.size()));
            Iterator iterator = objects.keySet().iterator();
            int i = 1;
            while(iterator.hasNext()) {
                Object o = iterator.next();
                b.append(String.format("\n%d:%s %s hc=%d", i, o.toString(), o.getClass().getSimpleName(), System.identityHashCode(o)));
                i++;
            }
            return b.toString();
        }
    }

    public static void clearAll() {
        synchronized(instances) {
            Iterator<String> names = instances.keySet().iterator();
            while(names.hasNext()) {
                String name = names.next();
                ICrossSessionCache cache = instances.get(name);
                cache.clear();
            }
            instances.clear();
        }
    }
}
