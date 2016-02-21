package com.github.aliakhtar.orak.wikidata;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
//import com.github.aliakhtar.orak.wikidata.util.Logging;

import com.github.aliakhtar.orak.util.Logging;
import io.vertx.core.json.JsonObject;

public class ClaimParser implements Callable<Optional<JsonObject>>
{
    private final Logger log = Logging.get(this);

    public ClaimParser(String propId,
                       JsonObject claimInput)
    {

    }

    @Override
    public Optional<JsonObject> call() throws Exception
    {
        return Optional.empty();
    }
}
