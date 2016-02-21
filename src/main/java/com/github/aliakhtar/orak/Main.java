package com.github.aliakhtar.orak;

import java.util.logging.Logger;

import com.github.aliakhtar.orak.elasticsearch.ElasticSearchEngine;
import com.github.aliakhtar.orak.scripts.WikiDataToElasticSearch;
import com.github.aliakhtar.orak.util.Logging;

public class Main
{
    private final Logger log = Logging.get(this);

    public static void main(String[] args) throws Exception
    {
        new Main();
    }

    public Main() throws Exception
    {
        Environment env = Environment.get();
        try(ElasticSearchEngine  es = new ElasticSearchEngine(env))
        {
            new WikiDataToElasticSearch("/mnt/data/wikidata.json.bz2", es).run();
        }
    }
}