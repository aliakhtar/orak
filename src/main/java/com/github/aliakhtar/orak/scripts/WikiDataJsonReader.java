package com.github.aliakhtar.orak.scripts;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.aliakhtar.orak.util.Logging;

public class WikiDataJsonReader
{
    private final Logger log = Logging.get(this);

    private final String path;

    public WikiDataJsonReader(String path)
    {
        this.path = path;
    }

    public void run() throws IOException
    {
        File file = new File(path);
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(file);

        int i = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT)
        {
            i++;
            log.info(  i + " : " + parser.getCurrentName());
            log.info(  i + " : " + parser.getCurrentToken().asString());
        }

    }
}
