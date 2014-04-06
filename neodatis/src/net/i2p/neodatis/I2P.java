/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.i2p.neodatis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.neodatis.odb.NeoDatisConfig;
import org.neodatis.odb.NeoDatisGlobalConfig;
import net.i2p.I2PAppContext;
import org.neodatis.odb.NeoDatis;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBServer;

/**
 *
 * @author Sponge
 */
public class I2P {

    private final static int NEODATIS_SERVER_PORT = 65534;
    private final static String NEODATIS_SERVER = "127.0.0.1";
    private static Object server = null;

    public static String getNEODATIS_SERVER() {
        return NEODATIS_SERVER;
    }

    public static int getNEODATIS_SERVER_PORT() {
        return NEODATIS_SERVER_PORT;
    }

    public static String getNEODATIS_SERVERver() {
        return org.neodatis.odb.Release.RELEASE_NUMBER + "-" + org.neodatis.odb.Release.RELEASE_BUILD + " (" + org.neodatis.odb.Release.RELEASE_DATE + ")" + "PatchLevel " + org.neodatis.odb.PatchLevel.patchLevel;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        boolean real = true;
        try {
            if(args[0].equalsIgnoreCase("start")) {
                try {
                    // Settle...
                    Thread.sleep(1000);
                } catch(InterruptedException ex1) {
                }
                try {
                    System.out.println("Attempting to start NeoDatis ODB Server on " + NEODATIS_SERVER + ":" + NEODATIS_SERVER_PORT);
                    NeoDatisConfig cfg = NeoDatisGlobalConfig.get().setDatabaseCharacterEncoding("UTF-8");
                    // force 127.0.0.1, so it won't accidentally try localhost...
                    // ...which can map to ipv6 and fuck up!
                    cfg.setHostAndPort(NEODATIS_SERVER, NEODATIS_SERVER_PORT);
                    // Set the database directory
                    cfg.setBaseDirectory(I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath());
                    // Creates the server
                    server = NeoDatis.openServer(NEODATIS_SERVER_PORT, cfg);
                    // Starts the server to run in an independent thread
                    ((ODBServer)server).startServer(true);
                    try {
                        // Settle...
                        Thread.sleep(1000);
                    } catch(InterruptedException ex1) {
                        try {
                            ((ODBServer)server).close();
                        } catch(Exception e) {
                        }
                        server = null;
                    }

                } catch(SecurityException ex) {
                    server = null;
                    System.out.println("Can't start NeoDatis ODB, SecurityException!!");
                } catch(UnsupportedEncodingException ex) {
                    server = null;
                    System.out.println("Can't start NeoDatis ODB, no UTF-8?? WTF??!!");
                } catch(IOException ex) {
                    server = null;
                    System.out.println("Can't start NeoDatis ODB, IOException!!");
                }
                if(server != null) {
                    System.setProperty("NeoDatisRunning", "true");
                }
            } else if(args[0].equalsIgnoreCase("stop")) {
                System.out.println("NeoDatis server stopping");
                if(server != null) {
                    ODB odb;
                    // Get a lock on NeoDatis
                    try {
                        odb = NeoDatis.openClient(NEODATIS_SERVER, NEODATIS_SERVER_PORT, "Seedlessdb");
                        // Need a way to tell everybody to stop. This would allow class unloading.
                        odb.close();
                    } catch(Exception e) {
                    }
                    try {
                        // Settle... let stuff exit
                        Thread.sleep(6000);
                    } catch(InterruptedException ex1) {
                    }
                    try {
                        odb = NeoDatis.openClient(NEODATIS_SERVER, NEODATIS_SERVER_PORT, "Seedlessdb");
                        odb.close();
                        ((ODBServer)server).close();
                    } catch(Exception e) {
                        // Don't dump on exit anymore.
                        // e.printStackTrace();
                    }
                    server = null;
                } else {
                    System.out.println("NeoDatis server was never started! BUG in I2P!");
                }
                System.setProperty("NeoDatisRunning", "false");
            } else if(args[0].equalsIgnoreCase("cnfetest")) {
                real = false;
                throw new ClassNotFoundException("ClassNotFoundException Test");
            } else {
                System.out.println("No start or stop arg given to net.i2p.neodatis.I2P.main");
            }
        } catch(ClassNotFoundException cnfe) {
            if(real) {
                System.out.println("NeoDatis server was never started! BUG in I2P!");
            } else {
                System.out.println("NeoDatis ClassNotFoundException test completed.");
            }
        }
    }
}
