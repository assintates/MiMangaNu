package ar.rulosoft.mimanganu.servers;

import android.content.Context;
import android.support.annotation.Nullable;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.rulosoft.mimanganu.R;
import ar.rulosoft.mimanganu.componentes.Chapter;
import ar.rulosoft.mimanganu.componentes.Manga;
import ar.rulosoft.mimanganu.componentes.ServerFilter;

/**
 * Created by Raul on 03/02/2016.
 */
class RawSenManga extends ServerBase {

    private static final String HOST = "http://raw.senmanga.com/";

    private static final int[] fltGenre = {
            R.string.flt_tag_all,
            R.string.flt_tag_action,
            R.string.flt_tag_adult,
            R.string.flt_tag_adventure,
            R.string.flt_tag_comedy,
            R.string.flt_tag_cooking,
            R.string.flt_tag_drama,
            R.string.flt_tag_ecchi,
            R.string.flt_tag_fantasy,
            R.string.flt_tag_gender_bender,
            R.string.flt_tag_harem,
            R.string.flt_tag_historical,
            R.string.flt_tag_horror,
            R.string.flt_tag_josei,
            R.string.flt_tag_light_novel,
            R.string.flt_tag_martial_arts,
            R.string.flt_tag_mature,
            R.string.flt_tag_music,
            R.string.flt_tag_mystery,
            R.string.flt_tag_psychological,
            R.string.flt_tag_romance,
            R.string.flt_tag_school_life,
            R.string.flt_tag_sci_fi,
            R.string.flt_tag_seinen,
            R.string.flt_tag_shoujo,
            R.string.flt_tag_shoujo_ai,
            R.string.flt_tag_shounen,
            R.string.flt_tag_shounen_ai,
            R.string.flt_tag_slice_of_life,
            R.string.flt_tag_smut,
            R.string.flt_tag_sports,
            R.string.flt_tag_supernatural,
            R.string.flt_tag_tragedy,
            R.string.flt_tag_webtoon,
            R.string.flt_tag_yuri
    };
    private static final String[] valGenre = {
            "Manga/",
            "directory/category/Action/",
            "directory/category/Adult/",
            "directory/category/Adventure/",
            "directory/category/Comedy/",
            "directory/category/Cooking/",
            "directory/category/Drama/",
            "directory/category/Ecchi/",
            "directory/category/Fantasy/",
            "directory/category/Gender-Bender/",
            "directory/category/Harem/",
            "directory/category/Historical/",
            "directory/category/Horror/",
            "directory/category/Josei/",
            "directory/category/Light_Novel/",
            "directory/category/Martial_Arts/",
            "directory/category/Mature/",
            "directory/category/Music/",
            "directory/category/Mystery/",
            "directory/category/Psychological/",
            "directory/category/Romance/",
            "directory/category/School_Life/",
            "directory/category/Sci-Fi/",
            "directory/category/Seinen/",
            "directory/category/Shoujo/",
            "directory/category/Shoujo-Ai/",
            "directory/category/Shounen/",
            "directory/category/Shounen-Ai/",
            "directory/category/Slice_of_Life/",
            "directory/category/Smut/",
            "directory/category/Sports/",
            "directory/category/Supernatural/",
            "directory/category/Tragedy/",
            "directory/category/Webtoons/",
            "directory/category/Yuri/"
    };
    private static final int[] fltOrder = {
            R.string.flt_order_views,
            R.string.flt_order_rating,
            R.string.flt_order_alpha,
    };
    private static final String[] valOrder = {
            "?order=popular",
            "?order=rating",
            "?order=title"
    };

    RawSenManga(Context context) {
        super(context);
        setFlag(R.drawable.flag_raw);
        setIcon(R.drawable.senmanga);
        setServerName("SenManga");
        setServerID(RAWSENMANGA);
    }

    @Nullable
    @Override
    public ArrayList<Manga> getMangas() throws Exception {
        return null;
    }

    @Override
    public boolean hasList() {
        return false;
    }

    @Override
    public ArrayList<Manga> search(String term) throws Exception {
        String web = HOST + "search?q=" + URLEncoder.encode(term, "UTF-8");
        String source = getNavigatorAndFlushParameters().get(web.replaceAll("^http", "https"));
        Pattern p = Pattern.compile("<a href='([^']+)' title='([^']+)' style", Pattern.DOTALL);
        Matcher m = p.matcher(source);
        ArrayList<Manga> mangas = new ArrayList<>();
        while (m.find()) {
            Manga manga = new Manga(getServerID(), m.group(2), HOST + m.group(1), false);
            mangas.add(manga);
        }
        return mangas;
    }

    @Override
    public void loadChapters(Manga manga, boolean forceReload) throws Exception {
        if (manga.getChapters().isEmpty() || forceReload) {
            String data = getNavigatorAndFlushParameters().get(manga.getPath());
            // Summary
            manga.setSynopsis(
                    getFirstMatchDefault(
                            "itemprop=\"description\">([^<]+)", data, context.getString(R.string.nodisponible)));
            if(manga.getSynopsis().startsWith("No Description.")) {
                manga.setSynopsis(context.getString(R.string.nodisponible));
            }
            // Cover
            manga.setImages(
                    HOST + getFirstMatchDefault(
                            "itemprop=\"image\" src=\"([^\"]+)", data, ""));
            // Author(s)
            manga.setAuthor(
                    getFirstMatchDefault(
                            "<strong class='data'>Author:</strong>(.+?)</a></span>", data,
                            context.getString(R.string.nodisponible)).replace("</a>", "</a>,"));
            // Genre
            manga.setGenre(
                    getFirstMatchDefault("<strong class='data'>Categorize in:</strong>(.+?)</a></p>", data,
                            context.getString(R.string.nodisponible)));
            // Status
            manga.setFinished(
                    getFirstMatchDefault(
                            "<strong class='data'>Status:</strong>(.+?)</span>", data, "").contains("Complete"));
            // Chapters
            Pattern p = Pattern.compile("</td><td><a href=\"([^\"]+)\" title=\"([^\"]+)", Pattern.DOTALL);
            Matcher m = p.matcher(data);
            while (m.find()) {
                Chapter mc;
                if (m.group(1).endsWith("/1"))
                    // strip off page suffix if present
                    mc = new Chapter(m.group(2), HOST + m.group(1).substring(0, m.group(1).length() - 2));
                else
                    mc = new Chapter(m.group(2), HOST + m.group(1));
                mc.addChapterFirst(manga);
            }
        }
    }

    @Override
    public void loadMangaInformation(Manga manga, boolean forceReload) throws Exception {
        loadChapters(manga, forceReload);
    }

    @Override
    public String getImageFrom(Chapter chapter, int page) throws Exception {
        String path = chapter.getPath().substring(HOST.length());
        return HOST + "viewer/" + path + "/" + page;
    }

    @Override
    public void chapterInit(Chapter chapter) throws Exception {
        if(chapter.getPages() == 0) {
            String data = getNavigatorAndFlushParameters().get(chapter.getPath());
            String number = getFirstMatch(
                    "</select> of (\\d+)", data,
                    context.getString(R.string.server_failed_loading_page_count));
            chapter.setPages(Integer.parseInt(number));
        }
    }

    @Override
    public ServerFilter[] getServerFilters() {
        return new ServerFilter[]{
                new ServerFilter(
                        context.getString(R.string.flt_genre),
                        buildTranslatedStringArray(fltGenre), ServerFilter.FilterType.SINGLE),
                new ServerFilter(
                        context.getString(R.string.flt_order) + " (" + context.getString(R.string.flt_hint_order_unfiltered_only) + ")",
                        buildTranslatedStringArray(fltOrder), ServerFilter.FilterType.SINGLE)
        };
    }

    @Override
    public ArrayList<Manga> getMangasFiltered(int[][] filters, int pageNumber) throws Exception {
        String web;
        if (fltGenre[filters[0][0]] == R.string.flt_tag_all) {
            web = HOST + valGenre[filters[0][0]] + valOrder[filters[1][0]] + "&page=" + pageNumber;
        }
        else {
            web = HOST + valGenre[filters[0][0]] + "?page=" + pageNumber;
        }
        String source = getNavigatorAndFlushParameters().get(web);
        Pattern p = Pattern.compile("cover\">\\s*<a href=\"([^\"]+)\" title=\"([^\"]+)", Pattern.DOTALL);
        Matcher m = p.matcher(source);
        ArrayList<Manga> mangas = new ArrayList<>();
        while (m.find()) {
            Manga manga = new Manga(getServerID(), m.group(2), HOST + m.group(1), false);
            manga.setImages(HOST + "/Manga/cover/" + m.group(1).replaceAll("/", "") + ".jpg");
            mangas.add(manga);
        }
        return mangas;
    }
}
