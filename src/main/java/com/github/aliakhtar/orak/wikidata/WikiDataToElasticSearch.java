package com.github.aliakhtar.orak.wikidata;

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

import static java.util.Optional.ofNullable;

public class WikiDataToElasticSearch
{
    private static final int BATCH_SIZE = 500;

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

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(file))) ))
        {
            String line;

            while ( (line = reader.readLine()) != null )
            {
                count++;
                process(line);
                log.info(count + " , " + batch.size());
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

        batch.add( getSummary(optionalJson.get()) );
        if (batch.size() >= BATCH_SIZE)
            sendOff();
    }


    private JsonObject getSummary(JsonObject data) throws Exception
    {
        String englishLabel = parseEngValue(ofNullable(data.getJsonObject("labels")));
        String englishDesc = parseEngValue(ofNullable(data.getJsonObject("descriptions")));
        String wikiTitle = parseEngWikiTitle( ofNullable(data.getJsonObject("sitelinks")) );

        //Since synonyms filter is used, label is kept as an array and all aliases are grouped into it.
        JsonArray aliases = parseEngAliases( ofNullable( data.getJsonObject("aliases") ) );
        aliases.add(englishLabel);

        JsonArray claims = parseClaims( ofNullable( data.getJsonObject("claims") ) );

        data.put("labels", aliases);
        data.put("description", englishDesc);
        data.put("wikiTitle", wikiTitle);
        data.put("claims", claims);

        data.remove("aliases");
        data.remove("descriptions");
        data.remove("sitelinks");

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

        List<JsonObject> items = new ArrayList<>( batch.size() );
        List<JsonObject> props = new ArrayList<>( batch.size() );

        for (int i = 0; i < batch.size(); i++)
        {
            JsonObject item = batch.get(i);
            String type = item.getString("type");
            if (type.equals("item"))
                items.add(item);
            else
                props.add(item);
        }

        BulkResponse resp = (! items.isEmpty()) ? es.putBulk(ElasticSearchEngine.WIKIDATA, ElasticSearchEngine.ITEMS, items) : null;
        if (resp != null && resp.hasFailures())
        {
            Writer.writeOrOverwrite("error.log", resp.buildFailureMessage());
            throw new RuntimeException(resp.buildFailureMessage());
        }

        resp = (! props.isEmpty()) ? es.putBulk(ElasticSearchEngine.WIKIDATA, ElasticSearchEngine.PROPS, props) : null;
        if (resp != null && resp.hasFailures())
        {
            Writer.writeOrOverwrite("error.log", resp.buildFailureMessage());
            throw new RuntimeException(resp.buildFailureMessage());
        }

        log.info("Succeeded");
        batch.clear();
    }


    private JsonArray parseClaims(Optional<JsonObject> claims) throws Exception
    {
        if (! claims.isPresent())
            return new JsonArray();

        JsonArray result = new JsonArray();
        for (String propId: claims.get().fieldNames())
        {
            JsonArray propClaims = claims.get().getJsonArray(propId);
            for (int i = 0; i < propClaims.size(); i++ )
            {
                JsonObject claimInput = propClaims.getJsonObject(i);
                Optional<JsonObject> claimResult = new ClaimParser(claimInput).call();
                if (claimResult.isPresent())
                    result.add( claimResult.get() );
            }
        }

        return result;
    }
}