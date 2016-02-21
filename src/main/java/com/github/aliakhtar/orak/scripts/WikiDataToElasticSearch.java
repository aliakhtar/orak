package com.github.aliakhtar.orak.scripts;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.github.aliakhtar.orak.elasticsearch.ElasticSearchEngine;
import com.github.aliakhtar.orak.util.Logging;
import com.github.aliakhtar.orak.util.Util;
import com.github.aliakhtar.orak.util.io.Writer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.elasticsearch.action.bulk.BulkResponse;

public class WikiDataToElasticSearch
{
    private static final int BATCH_SIZE = 10000;

    private final Logger log = Logging.get(this);

    private final String path;
    private final ElasticSearchEngine es;

    private final List<JsonObject> batch;


    private int count = 0;
    public WikiDataToElasticSearch(String wikiDataJsonDumpPath, ElasticSearchEngine es)
    {
        this.path = wikiDataJsonDumpPath;
        this.es = es;
        batch = new ArrayList<>( BATCH_SIZE );

    }

    public void run() throws Exception
    {
        long start = System.currentTimeMillis();

        File file = new File(path);

        es.dropIndex(ElasticSearchEngine.WIKIDATA);

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(file))) ))
        {
            String line;

            while ( (line = reader.readLine()) != null )
            {
                count++;
                process(line);
            }
        }

        if (! batch.isEmpty())
        {
            log.info("Sending last " + batch.size() + " items: " );
            sendOff();
        }

        long elapsed = System.currentTimeMillis() - start;

        log.info("Elapsed: " + TimeUnit.MILLISECONDS.toMinutes( elapsed ));

    }


    private void process(String line) throws Exception
    {
        Optional<JsonObject> optionalJson = toJson(line);
        if (! optionalJson.isPresent())
            return;

        batch.add( getSummary( optionalJson.get() ) );
        if (batch.size() >= BATCH_SIZE)
            sendOff();
    }


    private JsonObject getSummary(JsonObject data)
    {
        String englishLabel = parseEngValue(Optional.ofNullable(data.getJsonObject("labels")));
        String englishDesc = parseEngValue(Optional.ofNullable(data.getJsonObject("descriptions")));
        String wikiTitle = parseEngWikiTitle( Optional.ofNullable( data.getJsonObject("sitelinks") ) );

        Optional<JsonObject> origAliases = Optional.ofNullable(data.getJsonObject("aliases"));

        data.put("label", englishLabel);
        data.put("description", englishDesc);
        data.put("wikiTitle", wikiTitle);
        data.put("aliases", parseEngAliases(origAliases));

        data.remove("labels");
        data.remove("descriptions");
        data.remove("sitelinks");

        data.remove("claims");
        data.remove("lastrevid");
        data.remove("modified");

        return data;
    }

    private String parseEngValue(Optional<JsonObject> languageMap)
    {
        /* Sample input:
              "labels" : {
                "es" : {
                  "language" : "es",
                  "value" : "Gabriel González Videla"
                }
         */
        if (! languageMap.isPresent())
            return  "";

        JsonObject engMap = languageMap.get().getJsonObject("en");
        if (engMap == null )
            return  "";

        String value = engMap.getString("value");
        return (value != null) ? value : "";
    }

    private String parseEngWikiTitle(Optional<JsonObject> siteLinks)
    {
        /* Sample input:
                "sitelinks" : {
                    "eswiki" : {
                      "site" : "eswiki",
                      "title" : "Gabriel González Videla",
                      "badges" : [ ]
                    }
         */

        if (! siteLinks.isPresent())
            return "";

        JsonObject engWikiMap = siteLinks.get().getJsonObject("enwiki");
        if (engWikiMap == null)
            return "";

        String title = engWikiMap.getString("title");
        return (title != null) ? title : "";
    }


    private JsonArray parseEngAliases(Optional<JsonObject> aliases)
    {
        /* Sample input:
                "aliases": {
                    "en": [
                      {
                        "language": "en",
                        "value": "NYC"
                      },
                      {
                        "language": "en",
                        "value": "New York"
                      },
                    ],
         */

        JsonArray result = new JsonArray();
        if (! aliases.isPresent())
            return result;

        JsonArray engAliases = aliases.get().getJsonArray("en");
        if (engAliases == null || engAliases.isEmpty())
            return result;

        for (int i = 0; i < engAliases.size(); i++)
        {
            JsonObject map = engAliases.getJsonObject(i);
            String alias = map.getString("value");

            if (!Util.isBlank(alias))
                result.add(alias);
        }

        return result;
    }


    private Optional<JsonObject> toJson(String line)
    {
        //Wikidata json is one json object per line. All objects are inside an array. Start and end lines contain the
        //opening and closing brackets. So, ignore the single close / end square brackets, and process each other line
        //as an individual json object.
        if (line.equals("[") || line.equals("]"))
            return Optional.empty();

        try
        {
            return Optional.of( new JsonObject(line) );
        }
        catch (Exception e)
        {
            log.severe("ERROR PARSING: " + e.getMessage() + " , json: " + line);
            return Optional.empty();
        }
    }

    private void sendOff() throws Exception
    {
        log.info("Sending to elastic, count: " + count);

        BulkResponse resp = es.putBulk(ElasticSearchEngine.WIKIDATA, ElasticSearchEngine.SUMMARY, batch);
        if (resp.hasFailures())
        {
            Writer.writeOrOverwrite("error.log", resp.buildFailureMessage());
            throw new RuntimeException(resp.buildFailureMessage());
        }

        log.info("Succeeded");
        batch.clear();
    }
}
