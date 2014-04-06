/**
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                    Version 2, December 2004
 *
 * Copyright (C) sponge
 *   Planet Earth
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 * See...
 *
 *	http://sam.zoy.org/wtfpl/
 *	and
 *	http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */
package net.i2p.seedless.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import net.i2p.I2PAppContext;

/**
 *
 * @author sponge
 */
public class SetupServer {

    private final static String CONFIG_FILE = "Seedless.config";
    private String accepted[];
    private long expires[]; // in minutes

    public SetupServer() {
        String sep = java.io.File.separator;
        String cd = "." + sep;
        try {
            cd = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + sep;
        } catch(IOException ex) {
        }

        File cfg = new File(cd + CONFIG_FILE);
        FileInputStream fi = null;
        Boolean OK = false;
        Boolean broken = false;
        String line;
        accepted = new String[1];
        accepted[0] = "";
        expires = new long[1];
        expires[0] = 0;
        int i = 0;
        try {
            fi = new FileInputStream(cfg);
            BufferedReader br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
            while(br.ready()) {
                line = br.readLine().trim();
                accepted[i] = line.split(" ", 2)[0];
                if(accepted[i].length() > 20) {
                    accepted[i] = accepted[i].substring(20);
                    broken = true;
                }
                expires[i] = Integer.parseInt(line.split(" ", 2)[1]);
                if(expires[i] < 120) {
                    System.out.println("Detected to low of a TimeToLive for " + accepted[i] + ", fixing it to 120 minutes.");
                    expires[i] = 120;
                    broken = true;
                }
                if(expires[i] > 2160) {
                    System.out.println("Detected to high of a TimeToLive for " + accepted[i] + ", fixing it to 2160 minutes.");
                    expires[i] = 2160;
                    broken = true;
                }
                i++;
                accepted = this.expand(accepted, 1);
                expires = this.expand(expires, 1);
            }
            if(i > 0) {
                accepted = this.collapse(accepted, 1);
                expires = this.collapse(expires, 1);
                OK = true;
            } else {
                accepted = new String[1];
                expires = new long[1];
                accepted[0] = "seedless";
                expires[0] = 12 * 60; // 12 hours
            }
        } catch(FileNotFoundException fnfe) {
            accepted = new String[1];
            expires = new long[1];
            accepted[0] = "seedless";
            expires[0] = 12 * 60; // 12 hours
        } catch(IOException ioe) {
            accepted = new String[1];
            expires = new long[1];
            accepted[0] = "seedless";
            expires[0] = 12 * 60; // 12 hours
        }
        try {
            fi.close();
        } catch(Exception ex) {
        }

        if(!OK || broken) {
            SaveConfig();
        }
    }

    public long[] getExpires() {
        return this.expires;
    }

    public String[] getAccepted() {
        return this.accepted;
    }

    /* add/modify entry, with some sanity checks. */
    public void addEntry(String service, int ttl) {
        if(ttl < 120) ttl = 120;
        if(ttl > 2160) ttl = 2160;
        if(service.length() > 20) service = service.substring(20);
        delEntry(service);
        int ptr = accepted.length;
        accepted = this.expand(accepted, 1);
        expires = this.expand(expires, 1);
        accepted[ptr] = service;
        expires[ptr] = ttl;
        SaveConfig();
    }

    /* delete entries matching service name */
    public void delEntry(String service) {
        String newaccepted[]= new String[accepted.length];
        long newexpires[]=new long[accepted.length]; // in minutes
        int dele = 0;
        int oldlen = accepted.length;
        for(int i=0; i< oldlen; i++) {
            if(accepted[i].equals(service)) {
                dele += 1;
            } else {
                newaccepted[i - dele] = accepted[i];
                newexpires[i - dele] = expires[i];
            }
        }
        int newlen = oldlen - dele;
        accepted = resize(newaccepted, newlen);
        expires = resize(newexpires, newlen);
        SaveConfig();
    }

    private void SaveConfig() {
        PrintStream fo = null;
        String sep = java.io.File.separator;
        String cd = "." + sep;
        try {
            cd = I2PAppContext.getGlobalContext().getConfigDir().getCanonicalPath() + sep;
            File cfg = new File(cd + CONFIG_FILE);
            try {
                fo = new PrintStream(cfg, "UTF-8");
                for(int i = 0; i < accepted.length; i++) {
                    fo.println(accepted[i] + " " + expires[i]);
                }
                fo.close();
            } catch(FileNotFoundException ex) {
            }
        } catch(IOException ex) {
        }
    }

    private String[] resize(String[] array, int size) {
        String[] temp = new String[size];
        int amount = size;
        if(size > array.length) {
            amount = array.length;
        }

        System.arraycopy(array, 0, temp, 0, amount);

        for(int j = array.length; j < size; j++) {
            temp[j] = "";
        }

        return temp;
    }

    private String[] expand(String[] array, int amount) {
        return this.resize(array, array.length + amount);
    }

    private String[] collapse(String[] array, int amount) {
        return this.resize(array, array.length - amount);
    }

    private long[] resize(long[] array, int size) {
        long[] temp = new long[size];
        int amount = size;
        if(size > array.length) {
            amount = array.length;
        }

        System.arraycopy(array, 0, temp, 0, amount);

        for(int j = array.length; j < size; j++) {
            temp[j] = 0;
        }

        return temp;
    }

    private long[] expand(long[] array, int amount) {
        return this.resize(array, array.length + amount);
    }

    private long[] collapse(long[] array, int amount) {
        return this.resize(array, array.length - amount);
    }

}
