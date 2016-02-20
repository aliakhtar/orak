package com.github.aliakhtar.orak.util.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

public class Writer
{
    public static void writeOrOverwrite(String absPath, String output)
    {
        Path path = Paths.get(absPath);
        try
        {
            Files.write(path, output.getBytes(), CREATE, TRUNCATE_EXISTING, WRITE);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void append(String absPath, String output)
    {
        Path path = Paths.get(absPath);
        try
        {
            Files.write(path, output.getBytes(), WRITE, APPEND);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
