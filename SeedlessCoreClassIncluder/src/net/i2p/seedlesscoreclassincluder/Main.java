/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.i2p.seedlesscoreclassincluder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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
            Class.forName("net.i2p.seedless.Version");
            System.out.println("Replacement of SeedlessCore jar file requires a full router stop and start.");
            System.out.println("Please STOP and then START your router!");
            return;
        } catch(ClassNotFoundException cnfe) {
        }

        // Wait a maximum of 10 seconds for NeoDatis.
        // We would not have to do this if load ordering was enforced.
        boolean goahead = false;
        int lcount = 0;
        while(lcount < 10) {
            String chk = System.getProperty("NeoDatisLoaded", "false");
            if(chk.equalsIgnoreCase("true")) {
                goahead = true;
                break;
            }
            lcount++;
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                break;
            }
        }
        if(goahead) {
            try {

                System.out.println("Adding SeedlessCore to the JVM globally.");
                // Add SeedlessCore to the whole JVM. It's needed _everywhere_
                String s = java.io.File.separator;
                String cp = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + s + "plugins" + s + "02_seedless" + s + "lib" + s + "SeedlessCore.jar";
                System.out.println("Adding `" + cp + "` to classpath");
                File f = new File(cp);
                URL u = f.toURI().toURL();
                mine = new Object[] {u};
                URLClassLoader urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
                Class urlClass = URLClassLoader.class;
                Method method = urlClass.getDeclaredMethod("addURL", parameters);
                method.setAccessible(true);
                method.invoke(urlClassLoader, mine);
                net.i2p.seedless.Version.main(null);
                System.out.println("Added SeedlessCore to the JVM Successfully.");
                // Check version file, if wrong or missing, nuke the database files and rewrite with new version

                System.out.println("Checking Version");
                String sep = java.io.File.separator;
                String cd = "." + sep;
                try {
                    cd = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + sep;
                } catch(IOException ex) {
                }
                String sv = "";
                String cv = net.i2p.seedless.Version.getVersion();
                File cfg = new File(cd + "SeedlessVersion");
                BufferedReader br = null;
                FileInputStream fi = null;
                PrintStream fo = null;

                BufferedWriter bw = null;
                try {
                    fi = new FileInputStream(cfg);
                    br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
                    sv = br.readLine().trim();
                    fi.close();
                } catch(UnsupportedEncodingException ex) {
                } catch(FileNotFoundException ex) {
                } catch(IOException ex) {
                }
                if(!cv.equals(sv)) {
                    // Erase database files.
                    System.out.println("Old Version, killing database...");
                    File mf = new File(cd + "Seedlessdb");
                    mf.delete();
                    mf = new File(cd + "Seedlessdb.lg");
                    mf.delete();
                    // Update the config file.
                    try {
                        fo = new PrintStream(cfg, "UTF-8");
                        fo.println(cv);
                        fo.close();
                    } catch(UnsupportedEncodingException ex1) {
                    } catch(FileNotFoundException ex1) {
                    }

                } else {
                    System.out.println("Version OK");
                }
                System.setProperty("SeedlessCoreLoaded", "true");

            } catch(MalformedURLException ex) {
                System.out.println("Can't Add SeedlessCore, MalformedURLException!!");
            } catch(IllegalAccessException ex) {
                System.out.println("Can't Add SeedlessCore, IllegalAccessException!!");
            } catch(IllegalArgumentException ex) {
                System.out.println("Can't Add SeedlessCore, IllegalArgumentException!!");
            } catch(InvocationTargetException ex) {
                System.out.println("Can't Add SeedlessCore, InvocationTargetException!!");
            } catch(NoSuchMethodException ex) {
                System.out.println("Can't Add SeedlessCore, NoSuchMethodException!!");
            } catch(IOException ex) {
            }
        } else {
            throw new ClassNotFoundException("Can't Add SeedlessCore, NeoDatis not found.");
        }
    }
}
