package ar.rulosoft.mimanganu.servers;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.rulosoft.mimanganu.R;
import ar.rulosoft.mimanganu.componentes.Chapter;
import ar.rulosoft.mimanganu.componentes.Manga;
import ar.rulosoft.mimanganu.componentes.ServerFilter;
import ar.rulosoft.mimanganu.utils.Util;

/**
 * Created by xtj-9182 on 01.12.2016.
 */
class ReadMangaToday extends ServerBase {
    private static final String HOST = "http://www.readmanga.today";
    private static final int[] fltGenre = {
            R.string.flt_tag_all,
            R.string.flt_tag_action,
            R.string.flt_tag_adventure,
            R.string.flt_tag_comedy,
            R.string.flt_tag_doujinshi,
            R.string.flt_tag_drama,
            R.string.flt_tag_ecchi,
            R.string.flt_tag_fantasy,
            R.string.flt_tag_gender_bender,
            R.string.flt_tag_harem,
            R.string.flt_tag_historical,
            R.string.flt_tag_horror,
            R.string.flt_tag_josei,
            R.string.flt_tag_lolicon,
            R.string.flt_tag_martial_arts,
            R.string.flt_tag_mature,
            R.string.flt_tag_mecha,
            R.string.flt_tag_mystery,
            R.string.flt_tag_one_shot,
            R.string.flt_tag_psychological,
            R.string.flt_tag_romance,
            R.string.flt_tag_school_life,
            R.string.flt_tag_sci_fi,
            R.string.flt_tag_seinen,
            R.string.flt_tag_shotacon,
            R.string.flt_tag_shoujo,
            R.string.flt_tag_shoujo_ai,
            R.string.flt_tag_shounen,
            R.string.flt_tag_shounen_ai,
            R.string.flt_tag_slice_of_life,
            R.string.flt_tag_smut,
            R.string.flt_tag_sports,
            R.string.flt_tag_supernatural,
            R.string.flt_tag_tragedy,
            R.string.flt_tag_yaoi,
            R.string.flt_tag_yuri
    };
    private static final String[] valGenre = {
            "/category/",
            "/category/action/",
            "/category/adventure/",
            "/category/comedy/",
            "/category/doujinshi/",
            "/category/drama/",
            "/category/ecchi/",
            "/category/fantasy/",
            "/category/gender-bender/",
            "/category/harem/",
            "/category/historical/",
            "/category/horror/",
            "/category/josei/",
            "/category/lolicon/",
            "/category/martial-arts/",
            "/category/mature/",
            "/category/mecha/",
            "/category/mystery/",
            "/category/one-shot/",
            "/category/psychological/",
            "/category/romance/",
            "/category/school-life/",
            "/category/sci-fi/",
            "/category/seinen/",
            "/category/shotacon/",
            "/category/shoujo/",
            "/category/shoujo-ai/",
            "/category/shounen/",
            "/category/shounen-ai/",
            "/category/slice-of-life/",
            "/category/smut/",
            "/category/sports/",
            "/category/supernatural/",
            "/category/tragedy/",
            "/category/yaoi/",
            "/category/yuri/"
    };

    ReadMangaToday(Context context) {
        super(context);
        setFlag(R.drawable.flag_en);
        setIcon(R.drawable.readmangatoday);
        setServerName("ReadMangaToday");
        setServerID(READMANGATODAY);
    }

    @Override
    public boolean hasList() {
        return false;
    }

    @Override
    public ArrayList<Manga> getMangas() throws Exception {
        return null;
    }

    @Override
    public ArrayList<Manga> search(String term) throws Exception {
        ArrayList<Manga> mangas = new ArrayList<>();
        String web = HOST + "/manga-list/" + term.toLowerCase().charAt(0);
        int count = -1;
        // starting with 't', as this is a common prefix in English
        String[] alphabet = {"t", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "v", "w", "x", "y", "z"};
        while (mangas.isEmpty()) {
            // if the Manga was not found (yet) search through the alphabet
            String source = getNavigatorAndFlushParameters().get(web);
            Pattern pattern = Pattern.compile("<a href=\"(http://www\\.readmanga\\.today/[^\"]+?)\">(.+?)</a>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(source);
            while (matcher.find()) {
                if (matcher.group(2).toLowerCase().contains(term.toLowerCase())) {
                    Manga manga = new Manga(getServerID(), matcher.group(2), matcher.group(1), false);
                    mangas.add(manga);
                }
            }
            if (count == alphabet.length) {
                break;
            }

            web = HOST + "/manga-list/";
            if (count != -1) {
                web = web + alphabet[count];
            }
            count++;
        }
        return mangas;
    }

    @Override
    public void loadChapters(Manga manga, boolean forceReload) throws Exception {
        loadMangaInformation(manga, forceReload);
    }

    @Override
    public void loadMangaInformation(Manga manga, boolean forceReload) throws Exception {
        if (manga.getChapters().isEmpty() || forceReload) {
            String source = getNavigatorAndFlushParameters().getAndReturnResponseCodeOnFailure(manga.getPath());
            if (source.equals("400")) {
                // ReadMangaToday returns 400 Bad Request sometimes
                // deleting it's cookies will usually get rid of the error
                Util.getInstance().removeSpecificCookies(context, HOST);
                source = getNavigatorAndFlushParameters().get(manga.getPath());
            }

            // Cover
            if (manga.getImages() == null || manga.getImages().isEmpty() || manga.getImages().contains("thumb")) {
                String img = getFirstMatchDefault("<div class=\"col-md-3\">.+?<img src=\"(.+?)\" alt=", source, "");
                manga.setImages(img);
            }

            // Summary
            String summary = getFirstMatchDefault("<li class=\"list-group-item movie-detail\">(.+?)</li>", source, context.getString(R.string.nodisponible));
            manga.setSynopsis(summary);

            // Status
            manga.setFinished(!source.contains("<dd>Ongoing</dd>"));

            // Author (can be multi-part)
            String author = "";
            //String author = getFirstMatchDefault("<li class=\"director\">.+?<li><a href=\".+?\">(.+?)</a>", source, "");
            Pattern p = Pattern.compile("<li><a href=\"http://www\\.readmanga\\.today/people/[^\"]+?\">([^\"]+?)</a>", Pattern.DOTALL);
            Matcher matcher = p.matcher(source);
            while (matcher.find()) {
                if (!author.equals(matcher.group(1) + ", ")) {
                    author += matcher.group(1);
                    author += ", ";
                }
            }
            if (!author.isEmpty()) {
                manga.setAuthor(author.substring(0, author.length() - 2));
            }
            else {
                manga.setAuthor(context.getString(R.string.nodisponible));
            }

            // Genre
            String genre = getFirstMatchDefault("<dt>Categories:</dt>.+?<dd>(.+?)</dd>", source, context.getString(R.string.nodisponible))
                    .replaceAll("</a>", ",");
            if (genre.endsWith(",")) {
                genre = genre.substring(0, genre.length() - 1);
            }
            manga.setGenre(genre);

            // Chapters
            p = Pattern.compile("<li>[\\s]*<a href=\"([^\"]+?)\">[\\s]*<span class=\"val\"><span class=\"icon-arrow-.\"></span>(.+?)</span>", Pattern.DOTALL);
            matcher = p.matcher(source);
            while (matcher.find()) {
                Chapter chapter = new Chapter(matcher.group(2), matcher.group(1));
                chapter.addChapterFirst(manga);
            }
        }
    }

    @Override
    public String getImageFrom(Chapter chapter, int page) throws Exception {
        assert chapter.getExtra() != null;
        return chapter.getExtra().split("\\|")[page - 1];
    }

    @Override
    public void chapterInit(Chapter chapter) throws Exception {
        if(chapter.getPages() == 0) {
            String source = getNavigatorAndFlushParameters().getAndReturnResponseCodeOnFailure(chapter.getPath() + "/all-pages");
            if (source.equals("400")) {
                // ReadMangaToday returns 400 Bad Request sometimes
                // deleting it's cookies will usually get rid of the error
                Util.getInstance().removeSpecificCookies(context, HOST);
                source = getNavigatorAndFlushParameters().get(chapter.getPath() + "/all-pages");
            }
            ArrayList<String> images = getAllMatch("<img src=\"([^\"]+)\" class=\"img-responsive-2\">", source);

            if (images.isEmpty()) {
                throw new Exception(context.getString(R.string.server_failed_loading_page_count));
            }
            chapter.setExtra(TextUtils.join("|", images));
            chapter.setPages(images.size());
        }
    }

    @Override
    public ServerFilter[] getServerFilters() {
        return new ServerFilter[] {
                new ServerFilter(
                        context.getString(R.string.flt_genre),
                        buildTranslatedStringArray(fltGenre), ServerFilter.FilterType.SINGLE)
        };
    }

    @Override
    public ArrayList<Manga> getMangasFiltered(int[][] filters, int pageNumber) throws Exception {
        String web;
        if (fltGenre[filters[0][0]] == R.string.flt_tag_all) {
            if (pageNumber == 1) {
                web = HOST + "/hot-manga/";
            }
            else {
                web = HOST + "/hot-manga/" + pageNumber;
            }
        } else {
            web = HOST + valGenre[filters[0][0]] + pageNumber;
        }
        String source = getNavigatorAndFlushParameters().getAndReturnResponseCodeOnFailure(web);
        if (source.equals("400")) {
            // ReadMangaToday returns 400 Bad Request sometimes
            // deleting it's cookies will usually get rid of the error
            Util.getInstance().removeSpecificCookies(context, HOST);
            source = getNavigatorAndFlushParameters().get(web);
        }
        // regex to generate genre ids: <li>.+?title="All Categories - (.+?)">
        Pattern pattern = Pattern.compile("<div class=\"left\">.+?<a href=\"(.+?)\" title=\"(.+?)\"><img src=\"(.+?)\" alt=\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(source);
        ArrayList<Manga> mangas = new ArrayList<>();
        while (matcher.find()) {
            Manga m = new Manga(getServerID(), matcher.group(2), matcher.group(1), false);
            m.setImages(matcher.group(3).replace("thumb/", ""));
            mangas.add(m);
        }
        return mangas;
    }
}
