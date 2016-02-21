package com.github.aliakhtar.orak.elasticsearch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aliakhtar.orak.Environment;
import com.github.aliakhtar.orak.util.Logging;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionFuzzyBuilder;

import static org.elasticsearch.common.xcontent.XContentFactory.*;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders.*;


import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ElasticSearchEngine implements AutoCloseable
{
    public static final String WIKIDATA = "wikidata"; //Index for raw wikidata data
    public static final String ITEMS = "items";
    public static final String PROPS = "props";

    private static final int PORT = 9300;

    private final Logger log = Logging.get(this);

    private final Client client;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public ElasticSearchEngine(Environment env) throws Exception
    {
        this(env.esEndPoint(), env.esClusterName());
    }

    public ElasticSearchEngine(String endPoint, String clusterName) throws Exception
    {
        Settings settings = Settings.settingsBuilder()
                                             .put("cluster.name", clusterName)
                                             .build();
        client = TransportClient.builder().settings(settings).build()
                         .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(endPoint), PORT));
    }


    public boolean createIndex(String name)
    {
        CreateIndexRequest req = client.admin().indices().prepareCreate(name).request();
        return client.admin().indices().create(req).actionGet().isAcknowledged();
    }

    public boolean dropIndex(String name)
    {
        log.warning("Dropping index: " + name);
        DeleteIndexRequest req = client.admin().indices().prepareDelete(name).request();
        return client.admin().indices().delete(req).actionGet().isAcknowledged();
    }

    public String put(String index, String type, Map<String, ?> item) throws Exception
    {
        String json = jsonMapper.writeValueAsString(item);
        IndexResponse result = client.prepareIndex(index, type)
                                     .setSource(json)
                                     .execute()
                                     .actionGet();
        if (result.isCreated() )
            return result.getId();
        throw new RuntimeException("Failed to create item, " + result );
    }


    public BulkResponse putBulk(String index, String type, Collection<JsonObject> data)
            throws Exception
    {
        BulkRequestBuilder req = client.prepareBulk();

        for (JsonObject json : data)
        {
            IndexRequestBuilder indexReq = client.prepareIndex(index, type);

            indexReq.setSource( json.encodePrettily() );
            req.add( indexReq );
        }

        return req.execute().actionGet();
    }

    public boolean putMapping(String index, String type, String mappingJson)
    {
        return client.admin().indices().preparePutMapping(index)
                     .setType(type)
                     .setSource(mappingJson)
                     .execute().actionGet().isAcknowledged();
    }


    @SuppressWarnings(value = "unchecked")
    public List<? extends CompletionSuggestion.Entry.Option>
        getCompletionSuggestions(String index, String fieldName, String query)
    {
        CompletionSuggestionFuzzyBuilder bldr = new CompletionSuggestionFuzzyBuilder(fieldName)
                                                        .field(fieldName)
                                                        .text(query)
                                                        .setFuzziness(Fuzziness.TWO)
                                                        .size(3);

        SuggestRequestBuilder req = client.prepareSuggest(index)
                                          .addSuggestion(bldr);

        //Lord, forgive me for I have sinned.
        return ( List<? extends CompletionSuggestion.Entry.Option> )
                       req.execute().actionGet().getSuggest()
                          .iterator().next().getEntries().iterator()
                          .next().getOptions();
    }


    @Override
    public void close() throws Exception
    {
        System.out.println("Closed elastic");
        client.close();
    }

}

