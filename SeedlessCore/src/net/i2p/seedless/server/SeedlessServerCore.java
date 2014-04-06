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
package net.i2p.seedless.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import net.i2p.seedless.data.SetupServer;
import net.i2p.seedless.data.Base64;
import net.i2p.seedless.classes.SeedlessException;
import net.i2p.seedless.classes.SeedlessServices;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import net.i2p.seedless.classes.TorrentSearch;
import org.neodatis.odb.ODB;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.criteria.Criterion;
import org.neodatis.odb.core.query.criteria.W;

/**
 *
 * @author sponge
 */
public class SeedlessServerCore {

    private static final String ANNOUNCE = "announce ";
    private static final String LOCATE = "locate ";
    private static final int ANNOUNCE_LEN = ANNOUNCE.length();
    private static final int LOCATE_LEN = LOCATE.length();
    private static Pattern LNUMERIC = Pattern.compile("[0-9,]+");
    private String accepted[];
    private long expires[]; // in minutes
    private SetupServer config = null;
    private PrintWriter out = null;
    private String STag = null;
    private String b32addr = null;
    private ODB odb = null;
    private String text = null;
    private boolean rss = false;
    public String cat_main = null;
    public String cat_languages = null;
    public String cat_subtitle_languages = null;
    public String cat_patch_crack = null;
    public String cat_ripper = null;
    public String cat_title = null;
    public String cat_album = null;
    public String cat_series_name = null;
    public String cat_length = null;
    public String cat_ISBN = null;
    public String cat_bitrate = null;
    public String cat_os = null;
    public String cat_format = null;
    public String cat_codec = null;
    public String cat_author = null;
    public String cat_composer = null;
    public String cat_publisher = null;
    public String cat_artist = null;
    public String cat_genre = null;
    public String cat_theme = null;
    public String cat_flavor = null;
    public String cat_reference_link = null;
    public String cat_release_date = null;
    public long cat_pages = 0;
    public long cat_season_number = 0;
    public long cat_disc_number = 0;
    public long cat_episode_number = 0;
    public int cat_track_number = 0;
    private String info_hash = null;
    private long page = 0;
    private String orequest;

    /**
     *
     * @param request
     * @param out
     * @param odb
     * @param rss
     * @param b32addr
     */
    public SeedlessServerCore(String orequest, Map<String, String[]> request,
                              PrintWriter out, ODB odb, boolean rss, String b32addr) {

        this.text = snag(request, "text");
        this.info_hash = snag(request, "info_hash");
        this.cat_main = snag(request, "cat_main");
        this.cat_languages = snag(request, "cat_languages");
        this.cat_subtitle_languages = snag(request, "cat_subtitle_languages");
        this.cat_patch_crack = snag(request, "cat_patch_crack");
        this.cat_ripper = snag(request, "cat_ripper");
        this.cat_title = snag(request, "cat_title");
        this.cat_album = snag(request, "cat_album");
        this.cat_series_name = snag(request, "cat_series_name");
        this.cat_length = snag(request, "cat_length");
        this.cat_ISBN = snag(request, "cat_ISBN");
        this.cat_bitrate = snag(request, "cat_bitrate");
        this.cat_os = snag(request, "cat_os");
        this.cat_format = snag(request, "cat_format");
        this.cat_codec = snag(request, "cat_codec");
        this.cat_author = snag(request, "cat_author");
        this.cat_composer = snag(request, "cat_composer");
        this.cat_publisher = snag(request, "cat_publisher");
        this.cat_artist = snag(request, "cat_artist");
        this.cat_genre = snag(request, "cat_genre");
        this.cat_theme = snag(request, "cat_theme");
        this.cat_flavor = snag(request, "cat_flavor");
        this.cat_reference_link = snag(request, "cat_reference_link");
        this.cat_release_date = snag(request, "cat_release_date");
        this.cat_pages = safelong(snag(request, "cat_pages"));
        this.cat_season_number = safelong(snag(request, "cat_season_number"));
        this.cat_disc_number = safelong(snag(request, "cat_disc_number"));
        this.cat_episode_number = safelong(snag(request, "cat_episode_number"));
        this.cat_track_number = safeint(snag(request, "cat_track_number"));
        this.page = safelong(snag(request, "showpage"));
        //System.out.println("Request string is `" + orequest + "'");
        // zap the showpage param if it exists.
        if(request.containsKey("showpage")) {
            this.orequest = orequest.replaceAll("(?<=&|^)showpage(=[^&]*)?(&|$)", "").replaceAll("(^&|&$)", "").trim();
        } else {
            this.orequest = orequest;
        }
        //System.out.println("Request string is now `" + this.orequest + "'");
        this.out = out;
        this.odb = odb;
        this.rss = rss;
        this.b32addr = b32addr;
    }

    private String snag(Map<String, String[]> request, String what) {
        if(request.containsKey(what)) {
            return request.get(what)[0];
        }
        return null;

    }

    /**
     *
     * @param what
     * @return
     */
    private long safelong(String what) {
        try {
            if(what != null) {
                long bar = Long.parseLong(what.trim());
                return bar;
            }
        } catch(NumberFormatException nfe) {
        }
        return 0;
    }

    /**
     *
     * @param what
     * @return
     */
    private int safeint(String what) {
        try {
            if(what != null) {
                int bar = Integer.parseInt(what.trim());
                return bar;
            }
        } catch(NumberFormatException nfe) {
        }
        return 0;
    }

    /**
     *
     * @param config
     * @param accepted
     * @param expires
     * @param out
     * @param STag
     * @param b32addr
     * @param odb
     */
    public SeedlessServerCore(SetupServer config, String[] accepted, long[] expires, PrintWriter out, String STag, String b32addr, ODB odb) {
        this.config = config;
        this.accepted = accepted;
        this.expires = expires;
        this.out = out;
        this.STag = STag;
        this.b32addr = b32addr;
        this.odb = odb;
    }

    /**
     *
     * @throws java.io.UnsupportedEncodingException
     * @throws net.i2p.seedless.classes.SeedlessException
     */
    public void Process() throws UnsupportedEncodingException, SeedlessException {
        int i, size;
        int p = -1;
        String info = null;
        boolean announce = false;
        boolean us = false;

        Date now = new Date();
        long date = now.getTime(); // in miliseconds
        SeedlessServices service = null;
        long sdate;
        long fdate;
        long udate;
        long xdate;
        // preen database of any old entries... except Seedless
        Objects<SeedlessServices> services = odb.query(SeedlessServices.class, W.le("date", date).and(W.not(W.like("service", "seedless")))).objects();
        while(services.hasNext()) {
            service = services.next();
            odb.delete(service);
            odb.commit();
        }
        if(STag != null) {
            STag = STag.trim();
            if(STag.startsWith(ANNOUNCE)) {
                info = Base64.decodeToString(STag.substring(ANNOUNCE_LEN), "UTF-8");
                announce = true;
            } else if(STag.startsWith(LOCATE)) {
                // Request to locate a database object.
                info = Base64.decodeToString(STag.substring(LOCATE_LEN), "UTF-8");
            }
            if(info != null) {
                String data[] = info.split(" ", 2);
                if(data.length != 2) {
                    throw new SeedlessException("Malformed request.");
                } else {
                    String svc = data[0];
                    String metadata = data[1].trim();
                    size = accepted.length;
                    for(i = 0; i < size; i++) {
                        if(accepted[i].equals(svc)) {
                            p = i;
                        }
                    }
                    if(metadata.length() > 1024) {
                        p = -2;
                    }
                    if(svc.length() > 20) {
                        p = -3;
                    }
                    if(p == -1) {
                        throw new SeedlessException("Service '" + svc + "' Not accepted on this server.");
                    } else if(p == -2) {
                        throw new SeedlessException("Metadata to long.");
                    } else if(p == -3) {
                        throw new SeedlessException("Service name to long.");
                    } else {
                        // We accept this content, update or search the database with the information.
                        sdate = expires[p];

                        fdate = (sdate / 5) * 4; // 1/5th of the ttl
                        if((sdate / 5) < 60) {
                            fdate = sdate - 60; // fixup for ttl < 300
                        }
                        udate = date + (sdate * 60000l);
                        xdate = date + (fdate * 60000l);
                        if(announce) {
                            boolean vfy = true;
                            // Update database.
                            try {
                                services = odb.query(SeedlessServices.class, W.equal("dest", b32addr)).objects();
                                // do not allow > 10 announce per minute from the same dest
                                // that's one every 6 seconds
                                int ct = 0;
                                while(services.hasNext()) {
                                    service = services.next();
                                    if((date - (service.date - (sdate * 60000l))) < 60001l) {
                                        ct++;
                                    }
                                }
                                if(ct > 6) {
                                    throw new SeedlessException("Announce too soon.");
                                }

                                boolean doit = true;
                                services = odb.query(SeedlessServices.class, W.equal("service", svc).and(W.equal("metadata", metadata)).and(W.equal("dest", b32addr))).objects();
                                // there will be only ___ONE___ if any, and all three fields must match.
                                if(services.hasNext()) {
                                    service = services.next();
                                    us = us | service.us;
                                    service.date = udate;
                                    if(us) {
                                        service.us = true;
                                    }
                                } else {
                                    // New record
                                    if(svc.equals("seedless")) {
                                        vfy = false;
                                        String[] ml;
                                        if(metadata.contains(",")) {
                                            ml = metadata.split(",");
                                        } else {
                                            ml = new String[1];
                                            ml[0] = metadata;
                                        }
                                        for(i = 0; i < ml.length; i++) {
                                            int l = ml[i].length();
                                            if(l > 20 || l < 1 || ml[i].matches("(?s)(?u)\\A(.)(\\s)(.)*\\z")) {
                                                doit = false;
                                            }
                                        }

                                    }
                                    if(svc.equals("torrent")) {
                                        String[] ml = metadata.split("\n");
                                        if(ml.length != 3) {
                                            doit = false;
                                        } else {
                                            if(ml[0].length() != 40) {
                                                doit = false;
                                            } else if(ml[1].length() != 28) {
                                                doit = false;
                                            } else if(!(ml[2].equals("seed") || ml[2].equals("leech"))) {
                                                doit = false;
                                            } else if(ml[0].contains(" ") || ml[1].contains(" ") || ml[2].contains(" ")) {
                                                doit = false;
                                            }
                                        }
                                    }
                                    // forgot to enforce...
                                    if(svc.equals("eepsite")) {
                                        if(!metadata.contains(" ")) {
                                            doit = false;
                                        }
                                        if(LNUMERIC.matcher(metadata).matches()) {
                                            doit = false;
                                        }
                                    }
                                    if(us) {
                                        service = new SeedlessServices(svc, metadata, b32addr, date + (expires[p] * 60000l), true, vfy);
                                    } else {
                                        service = new SeedlessServices(svc, metadata, b32addr, date + (expires[p] * 60000l), false, vfy);
                                    }
                                }
                                if(doit) {
                                    odb.store(service);
                                } else {
                                    throw new SeedlessException("Seedless Service format incorrect.");
                                }
                                odb.commit();
                            } catch(Exception e) {
                            }
                        } else {

                            // search database.
                            try {
                                if(metadata.length() == 0) {
                                    // Transfer request. Send only recent objects.
                                    services = odb.query(SeedlessServices.class, W.equal("service", svc).and(W.like("metadata", metadata).and(W.gt("date", xdate))).and(W.equal("good", true))).objects();
                                } else {
                                    // Search request. Send all objects.
                                    services = odb.query(SeedlessServices.class, W.equal("service", svc).and(W.like("metadata", metadata).and(W.equal("good", true)))).objects();
                                }
                                if(services.hasNext()) {
                                    File temp = File.createTempFile("seedlesscache", ".txt");
                                    PrintWriter tout = new PrintWriter(new FileWriter(temp));
                                    while(services.hasNext()) {
                                        service = services.next();
                                        tout.println(Base64.encode(service.dest + " " + ((service.date - date) / 60000) + " " + service.metadata, "UTF-8"));
                                    }
                                    odb.close();
                                    tout.close();

                                    BufferedReader in = new BufferedReader(new FileReader(temp));
                                    String s;
                                    while((s = in.readLine()) != null) {

                                        String UTF8Str = new String(s.getBytes(), "UTF-8");
                                        out.println(UTF8Str);
                                    }
                                    in.close();
                                    while(!temp.delete()) {
                                        Thread.sleep(500);
                                    }
                                }
                            } catch(Exception e) {
                            }
                        }
                    }
                }
            } else {
                throw new SeedlessException("Not Base64.");
            }
        } else {
            throw new SeedlessException("X-Seedless not set.");
        }

    }

    /**
     *
     */
    private void PukeRSS() {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<rss version=\"2.0\">");
        out.println("<channel>");
        out.println("<title>Seedless Decentralized I2P BitTorrent Search Results</title>");
        out.println("<link>http://sponge.i2p/</link>");
        out.println("<description>Seedless Decentralized I2P BitTorrent Search</description>");
    }

    /**
     *
     */
    private void PukeRSSEND() {
        out.println("</channel></rss>");
    }

    /**
     *
     * @param cril
     * @param crir
     * @return
     */
    private Criterion addor(Criterion cril, Criterion crir) {
        if(cril == null) {
            return crir;
        }
        return cril.or(crir);
    }

    /**
     *
     * @param cri
     * @param find
     * @param what
     * @return
     */
    private Criterion addlike(Criterion cri, String find, String what) {
        if(what != null) {
            return addor(cri, W.ilike(find, what));
        }
        return cri;
    }

    /**
     *
     * @param cri
     * @param find
     * @param what
     * @return
     */
    private Criterion addlike(Criterion cri, String find, long what) {
        if(what != 0) {
            return addor(cri, W.equal(find, what));
        }
        return cri;
    }

    /**
     *
     * @param cri
     * @param find
     * @param what
     * @return
     */
    private Criterion addlike(Criterion cri, String find, int what) {
        if(what != 0) {
            return addor(cri, W.equal(find, what));
        }
        return cri;
    }

    /**
     *
     * @throws java.io.UnsupportedEncodingException
     * @throws net.i2p.seedless.classes.SeedlessException
     */
    public void ProcessSearch() throws UnsupportedEncodingException, SeedlessException {
        // build query
        Criterion com = null;
        TorrentSearch tso = new TorrentSearch();


        if(info_hash != null) {
            com = addlike(com, "maggot", "maggot://" + info_hash + ":");
        }
        com = addlike(com, "comment", text);
        com = addlike(com, "filename", text);
        com = addlike(com, "filedata", text);
        com = addlike(com, "cat_main", cat_main);

        com = addlike(com, "cat_languages", cat_languages);
        com = addlike(com, "cat_subtitle_languages", cat_subtitle_languages);
        com = addlike(com, "cat_patch_crack", cat_patch_crack);
        com = addlike(com, "cat_ripper", cat_ripper);
        com = addlike(com, "cat_title", cat_title);
        com = addlike(com, "cat_album", cat_album);
        com = addlike(com, "cat_series_name", cat_series_name);
        com = addlike(com, "cat_length", cat_length);
        com = addlike(com, "cat_ISBN", cat_ISBN);
        com = addlike(com, "cat_bitrate", cat_bitrate);
        com = addlike(com, "cat_os", cat_os);
        com = addlike(com, "cat_format", cat_format);
        com = addlike(com, "cat_codec", cat_codec);
        com = addlike(com, "cat_author", cat_author);
        com = addlike(com, "cat_composer", cat_composer);
        com = addlike(com, "cat_publisher", cat_publisher);
        com = addlike(com, "cat_artist", cat_artist);
        com = addlike(com, "cat_genre", cat_genre);
        com = addlike(com, "cat_theme", cat_theme);
        com = addlike(com, "cat_flavor", cat_flavor);
        com = addlike(com, "cat_reference_link", cat_reference_link);
        com = addlike(com, "cat_release_date", cat_release_date);

        com = addlike(com, "cat_pages", cat_pages);
        com = addlike(com, "cat_season_number", cat_season_number);
        com = addlike(com, "cat_disc_number", cat_disc_number);
        com = addlike(com, "cat_episode_number", cat_episode_number);
        com = addlike(com, "cat_track_number", cat_track_number);


        // TO-DO: make pageprettier, but not so pretty that the simplistic browser can't work.
        if(com == null) {
            // no results page...
            if(rss) {
                PukeRSS();
                PukeRSSEND();
            } else {
                out.print("<HTML><BODY>No Results. You didn't search for anything!</BODY></HTML>");
            }
        } else {
            com = com.and(W.equal("code", 200));
            Objects<TorrentSearch> ts = odb.query(TorrentSearch.class, com).orderByDesc("date").objects();
            if(ts.isEmpty()) {
                // no results page...
                if(rss) {
                    PukeRSS();
                    PukeRSSEND();
                } else {
                    out.print("<HTML><BODY>No Results.</BODY></HTML>");
                }
            } else {
                long skip = page * 50l;
                long ttl = 0l;
                if(rss) {
                    skip = 0l; // disallow skipping pages on rss
                    PukeRSS();
                } else {
                    out.println("<HTML><BODY>");
                }
                while(ts.hasNext() && ttl < 50) {
                    TorrentSearch s = ts.next();

                    Objects<SeedlessServices> peers = odb.query(SeedlessServices.class, W.equal("service", "torrent").and(W.like("metadata", s.maggot.substring(9, 49)))).objects();
                    boolean good = peers.hasNext();
                    //System.out.println("good?"+good+" looking for "+s.maggot.substring(9, 49));
                    if(good) { // TO-DO: check if torrent is alive, this is a placeholder.
                        if(skip < 1) {
                            ttl++;
                            boolean gotcats = false;
                            String cats = "Category lineup:\n";
                            Field[] flds = s.getClass().getFields(); //  .getDeclaredFields();
                            List<Field> ss = new ArrayList<Field>();
                            ss.addAll(Arrays.asList(flds));
                            Iterator<Field> it = ss.iterator();
                            while(it.hasNext()) {
                                Boolean valueIsSet = false;
                                Boolean href = false;
                                Field thing = it.next();
                                String who = thing.getName();
                                if(who.startsWith("cat_")) {
                                    if(!who.equals("cat_main")) {
                                        // A bit sloppy, but will work...
                                        String is = thing.getType().toString();

                                        String value = null;
                                        href = false;
                                        try {
                                            if(is.equals("class java.lang.String")) {
                                                value = (String)(thing.get(s));
                                                if(value != null) {
                                                    valueIsSet = true;
                                                    href = true;
                                                }
                                            }
                                            if(is.equals("long")) {
                                                long bar = thing.getLong(s);
                                                if(bar != 0l) {
                                                    value = Long.toString(thing.getLong(s));
                                                    valueIsSet = true;
                                                }
                                            }
                                            if(is.equals("int")) {
                                                int bar = thing.getInt(s);
                                                if(bar != 0) {
                                                    value = Integer.toString(thing.getInt(s));
                                                    valueIsSet = true;
                                                }
                                            }
                                        } catch(IllegalArgumentException ex) {
                                        } catch(IllegalAccessException ex) {
                                        }
                                        if(valueIsSet) {
                                            gotcats = true;
                                            if(href) {
                                                cats = cats + "<A HREF=\"http://" + b32addr + "/Seedless/search?" + who + "=" + URLEncoder.encode(value, "UTF-8") + "\">" + who.substring(4) + "</A>:" + protectSpecialCharacters(value) + "\n";
                                            } else {
                                                cats = cats + who.substring(4) + ":" + value + "\n";
                                            }
                                        }
                                    }
                                }
                            }

                            if(rss) {
                                out.println("<item>");
                                out.println("<title>" + s.filename + "</title>");
                                if(s.cat_main == null) {
                                    out.println("<category>Not specified</category>");
                                } else {
                                    out.println("<category>" + s.cat_main + "</category>");
                                }
                                out.println("<description><![CDATA[" + protectSpecialCharacters(s.comment).replaceAll("(\r\n|\r|\n|\n\r)", "<br>\n"));
                                out.print(("\n" + protectSpecialCharacters(s.filedata) + "\n").replaceAll("(\r\n|\r|\n|\n\r)", "<br>\n"));
                                if(gotcats) {
                                    out.println(cats.replaceAll("(\r\n|\r|\n|\n\r)", "<br>\n"));
                                }
                                out.println("]]></description>");
                                SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                out.println("<pubDate>" + sdf.format(s.date) + "</pubDate>");
                                out.println("<author>Automagically indexed by Seedless!</author>");
                                out.println("<link> http://" + b32addr + "/Seedless/search?info_hash=" + s.maggot.substring(9, 49) + "</link>");
                                out.println("<enclosure url=\"" + s.maggot + "\" file=\"\" type=\"application/x-bittorrent\" />");
                                out.println("</item>");
                            } else {
                                // ugly results
                                out.println("<A HREF='" + s.maggot + "'>" + s.filename + "</A><BR><PRE>Description:\n</PRE>");
                                out.println(protectSpecialCharacters(s.comment).replaceAll("(\r\n|\r|\n|\n\r)", "<br>\n"));
                                out.println("<PRE>Archive contents:\n" + protectSpecialCharacters(s.filedata));
                                if(s.cat_main == null) {
                                    out.println("Category: Not specified\n");
                                } else {
                                    out.println("Category: " + protectSpecialCharacters(s.cat_main) + "\n");
                                }
                                if(gotcats) {
                                    out.print(cats);
                                }
                                out.print("</PRE>");
                                if(ts.hasNext()) {
                                    out.println("<HR>");
                                }
                            }
                        } else {
                            skip--;
                        }
                    }
                }
                if(rss) {
                    PukeRSSEND();
                } else {
                    if(page > 0l) {
                        out.println("<A HREF=\"http://" + b32addr + "/Seedless/search?showpage=" + (page - 1l) + "&" + orequest + "\">Previous</A>"); // prev
                    }
                    if(ts.hasNext()) {
                        out.println("<A HREF=\"http://" + b32addr + "/Seedless/search?showpage=" + (page + 1l) + "&" + orequest + "\">Next</A>"); // next
                    }
                    out.println("</BODY></HTML>");
                }
            }
        }
    }

    private static String protectSpecialCharacters(String originalUnprotectedString) {
        if(originalUnprotectedString == null) {
            return null;
        }
        boolean anyCharactersProtected = false;

        StringBuffer stringBuffer = new StringBuffer();
        for(int i = 0; i < originalUnprotectedString.length(); i++) {
            char ch = originalUnprotectedString.charAt(i);

            boolean controlCharacter = (ch < '0' && ch != '\n' && ch != '\r') || (ch < '@' && ch > '9') || (ch < 'a' && ch > 'Z') || (ch > 'z');

            if(controlCharacter) {
                stringBuffer.append("&#" + (int)ch + ";");
                anyCharactersProtected = true;
            } else {
                stringBuffer.append(ch);
            }
        }
        if(anyCharactersProtected == false) {
            return originalUnprotectedString;
        }

        return stringBuffer.toString();
    }
}