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
package net.i2p.seedless.classes;

import java.io.Serializable;

/**
 *
 * @author sponge
 */
public class TorrentSearch implements Serializable {

    // maggotlink+file+bfiledata+comment+cat1+cat2+cat3+cat4+cat5+cat6+cat7
    public String maggot = null;
    public String filename = null;
    public String filedata = null;
    public String comment = null;
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
    public long date = 0;
    public int code = 0;

    public TorrentSearch(String maggot, String filename, String filedata, String comment,
                         String cat_main,
                         String cat_languages,
                         String cat_subtitle_languages,
                         String cat_patch_crack,
                         String cat_ripper,
                         String cat_title,
                         String cat_album,
                         String cat_series_name,
                         String cat_length,
                         String cat_ISBN,
                         String cat_bitrate,
                         String cat_os,
                         String cat_format,
                         String cat_codec,
                         String cat_author,
                         String cat_composer,
                         String cat_publisher,
                         String cat_artist,
                         String cat_genre,
                         String cat_theme,
                         String cat_flavor,
                         String cat_reference_link,
                         String cat_release_date,
                         long cat_pages,
                         long cat_season_number,
                         long cat_disc_number,
                         long cat_episode_number,
                         int cat_track_number,
                         long date,
                         int code) {
        super();
        this.maggot = maggot;
        this.filename = filename;
        this.filedata = filedata;
        this.comment = comment;
        this.cat_main = cat_main;
        this.cat_languages = cat_languages;
        this.cat_subtitle_languages = cat_subtitle_languages;
        this.cat_patch_crack = cat_patch_crack;
        this.cat_ripper = cat_ripper;
        this.cat_title = cat_title;
        this.cat_album = cat_album;
        this.cat_series_name = cat_series_name;
        this.cat_length = cat_length;
        this.cat_ISBN = cat_ISBN;
        this.cat_bitrate = cat_bitrate;
        this.cat_os = cat_os;
        this.cat_format = cat_format;
        this.cat_codec = cat_codec;
        this.cat_author = cat_author;
        this.cat_composer = cat_composer;
        this.cat_publisher = cat_publisher;
        this.cat_artist = cat_artist;
        this.cat_genre = cat_genre;
        this.cat_theme = cat_theme;
        this.cat_flavor = cat_flavor;
        this.cat_reference_link = cat_reference_link;
        this.cat_release_date = cat_release_date;
        this.cat_pages = cat_pages;
        this.cat_season_number = cat_season_number;
        this.cat_disc_number = cat_disc_number;
        this.cat_episode_number = cat_episode_number;
        this.cat_track_number = cat_track_number;

        this.code = code;

    }

    public TorrentSearch() {
        super();
    }

    private String ns(String what) {
        if(what != null) {
            return new String(what);
        }
        return null;
    }
    // make a copy

    public TorrentSearch(TorrentSearch ss) {
        super();
        this.maggot = ns(ss.maggot);
        this.filename = ns(ss.filename);
        this.filedata = ns(ss.filedata);
        this.comment = ns(ss.comment);
        this.cat_ISBN = ns(ss.cat_ISBN);
        this.cat_album = ns(ss.cat_album);
        this.cat_artist = ns(ss.cat_artist);
        this.cat_author = ns(ss.cat_author);
        this.cat_bitrate = ns(ss.cat_bitrate);
        this.cat_codec = ns(ss.cat_codec);
        this.cat_composer = ns(ss.cat_composer);
        this.cat_flavor = ns(ss.cat_flavor);
        this.cat_format = ns(ss.cat_flavor);
        this.cat_genre = ns(ss.cat_genre);
        this.cat_languages = ns(ss.cat_languages);
        this.cat_length = ns(ss.cat_length);
        this.cat_main = ns(ss.cat_main);
        this.cat_os = ns(ss.cat_os);
        this.cat_patch_crack = ns(ss.cat_patch_crack);
        this.cat_publisher = ns(ss.cat_publisher);
        this.cat_reference_link = ns(ss.cat_reference_link);
        this.cat_release_date = ns(ss.cat_release_date);
        this.cat_ripper = ns(ss.cat_ripper);
        this.cat_series_name = ns(ss.cat_series_name);
        this.cat_subtitle_languages = ns(ss.cat_subtitle_languages);
        this.cat_theme = ns(ss.cat_theme);
        this.cat_title = ns(ss.cat_title);

        this.cat_pages = ss.cat_pages;
        this.cat_season_number = ss.cat_season_number;
        this.cat_disc_number = ss.cat_disc_number;
        this.cat_episode_number = ss.cat_episode_number;
        this.cat_track_number = ss.cat_track_number;
        this.date = ss.date;
        this.code = ss.code;
    }
}
