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
import com.github.aliakhtar.orak.util.io.*;
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

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(file))) ))
        {
            String line;

            while ( (line = reader.readLine()) != null )
            {
                count++;
                process(line);
            }
        }
        catch (Exception e) {throw e;}

        long elapsed = System.currentTimeMillis() - start;

        log.info("Elapsed: " + TimeUnit.MILLISECONDS.toMinutes( elapsed ));

    }


    private void process(String line) throws Exception
    {
        Optional<JsonObject> optionalJson = toJson(line);
        if (! optionalJson.isPresent())
            return;

        String type = optionalJson.get().getString("type", "unknown");
        if (Util.isBlank(type))
            type = "unknown";
        else
            type = Util.trimAndDownCase(type);

        batch.add(optionalJson.get());
        if (batch.size() < BATCH_SIZE)
            return;

        log.info("Sending to elastic, count: " + count);

        BulkResponse resp = es.putBulk(ElasticSearchEngine.WIKIDATA, type, batch);
        if (resp.hasFailures())
            throw new RuntimeException(resp.buildFailureMessage());

        log.info("Succeeded");
        batch.clear();


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
}
