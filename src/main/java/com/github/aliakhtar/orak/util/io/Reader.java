package com.github.aliakhtar.orak.util.io;


import com.github.aliakhtar.orak.util.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.github.aliakhtar.orak.util.io.IoConfig.*;

public class Reader
{
    private final Logger log = Logging.get(this);

    public static String readFile(String readPath)
            throws IOException
    {
        String absolutePath = resolve(readPath);
        Path path = Paths.get(absolutePath);
        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            StringBuilder sb = new StringBuilder();
            reader.lines().forEach(line -> addLine(sb, line));

            return sb.toString().trim();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public InputStreamReader getStreamReader(String readPath)
            throws IOException
    {
        String absolutePath = resolve(readPath);
        Path path = Paths.get(absolutePath);
        return new InputStreamReader( Files.newInputStream(path) );
    }


    /**
     * Resolves a path relative to the current working directory, to its
     * absolute path. If the path is already absolute, returns it as is.
     */
    public static String resolve(String path)
    {
        if ( path.startsWith( workingDir() ) && exists(path)  )
            return path;

        String newPath = workingDir();

        if (! path.startsWith( separator() ))
            newPath += separator();

        newPath += path;

        if (exists(newPath ))
            return newPath;

        throw new IllegalArgumentException("Path not found: " + path + " , attempted: " + newPath);
    }

    private static void addLine(StringBuilder sb, String line)
    {
        sb.append(line).append(eol() );
    }

    public static String readResource(String path)
    {
        path = "com/github/aliakhtar/orak/" + path;
        ClassLoader clsLoader = Reader.class.getClassLoader();
        try (
                   InputStream is = clsLoader.getResourceAsStream(path);
                   InputStreamReader isr = new InputStreamReader(is);
                   BufferedReader reader = new BufferedReader(isr);
            )
        {
            StringBuilder sb = new StringBuilder();
            reader.lines().forEach(line -> addLine(sb, line));
                return sb.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(path, e);
        }
    }
}
