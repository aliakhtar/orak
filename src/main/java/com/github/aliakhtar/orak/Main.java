package com.github.aliakhtar.orak;

import java.util.logging.Logger;

import com.github.aliakhtar.orak.scripts.WikiDataJsonReader;
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
        new WikiDataJsonReader("/mnt/data/wikidata.json").run();
    }
}