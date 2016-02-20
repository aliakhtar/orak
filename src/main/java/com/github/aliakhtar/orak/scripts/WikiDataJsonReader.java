package com.github.aliakhtar.orak.scripts;

import java.io.*;
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

    public void run() throws Exception
    {
        File file = new File(path);
        try(BufferedReader reader = new BufferedReader( new FileReader(file) ))
        {
            int i = 0;
            String line;
            while ( (line = reader.readLine()) != null )
            {
                i++;
                log.info(i + ": " + line);
            }
        }
        catch (Exception e) {throw e;}


    }
}
