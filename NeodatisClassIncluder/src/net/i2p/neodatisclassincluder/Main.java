/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.i2p.neodatisclassincluder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import net.i2p.I2PAppContext;

/**
 *
 * @author sponge
 */
public class Main {

    private static final Class[] parameters = new Class[] {URL.class};
    private static Object[] mine = null;

    /**
     * @param args the command line arguments
     * TO-DO: Unload the class.
     */
    public static void main(String[] args) throws ClassNotFoundException {
        try {
            Class.forName("org.neodatis.odb.PatchLevel");
            System.out.println("Replacement of Neodatis ODB jar file requires a full router stop and start.");
            System.out.println("Please STOP and then START your router!");
            return;
        } catch (ClassNotFoundException cnfe) {
            // good!
        }
        try {
            System.out.println("Adding Neodatis ODB to the JVM globally.");
            // Add Neodatis to the whole JVM. It's needed _everywhere_
            String s = java.io.File.separator;
            String cp = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + s + "plugins" + s + "01_neodatis" + s + "lib" + s + "neodatis.jar";
            System.out.println("Adding `" + cp + "` to classpath");
            File f = new File(cp);
            URL u = f.toURI().toURL();
            mine = new Object[] {u};
            URLClassLoader urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
            Class urlClass = URLClassLoader.class;
            Method method = urlClass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(urlClassLoader, mine);
            org.neodatis.odb.PatchLevel.main(null);
            System.out.println("NeoDatis ODB Server is to be located at " + net.i2p.neodatis.I2P.getNEODATIS_SERVER() + ":" + net.i2p.neodatis.I2P.getNEODATIS_SERVER_PORT());
            System.out.println("Added Neodatis ODB to the JVM Successfully.");
            System.setProperty("NeoDatisLoaded", "true");
        } catch(MalformedURLException ex) {
            System.out.println("Can't Add NeoDatis ODB, MalformedURLException!!");
        } catch(IllegalAccessException ex) {
            System.out.println("Can't Add NeoDatis ODB, IllegalAccessException!!");
        } catch(IllegalArgumentException ex) {
            System.out.println("Can't Add NeoDatis ODB, IllegalArgumentException!!");
        } catch(InvocationTargetException ex) {
            System.out.println("Can't Add NeoDatis ODB, InvocationTargetException!!");
        } catch(NoSuchMethodException ex) {
            System.out.println("Can't Add NeoDatis ODB, NoSuchMethodException!!");
        } catch(IOException ex) {
        }
    }
}
