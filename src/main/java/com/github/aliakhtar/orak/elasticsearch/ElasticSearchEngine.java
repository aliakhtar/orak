package com.github.aliakhtar.orak.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aliakhtar.orak.util.Logging;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionFuzzyBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ElasticSearchEngine implements AutoCloseable
{
    private static final String CLUSTER_NAME = "Realagogo_es_xxxx";
    private static final String DEV_ENDPOINT = "107.20.20.40";
    private static final String PROD_ENDPOINT = "10.35.202.149";

    private static final int PORT = 9300;

    private final Logger log = Logging.get(this);

    private final Client client;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public ElasticSearchEngine(String endPoint, String clusterName) throws UnknownHostException
    {
        Settings settings = Settings.settingsBuilder()
                                             .put("cluster.name", clusterName)
                                             .build();
        client = TransportClient.builder().build()
                         .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(endPoint), PORT));
    }


    public boolean createIndex(String name)
    {
        CreateIndexRequest req = client.admin().indices().prepareCreate(name).request();
        return client.admin().indices().create(req).actionGet().isAcknowledged();
    }

    public boolean dropIndex(String name)
    {
        DeleteIndexRequest req = client.admin().indices().prepareDelete(name).request();
        return client.admin().indices().delete(req).actionGet().isAcknowledged();
    }

    public void dropIndexIfExists(String name)
    {
        dropIndex(name);
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


    public BulkResponse putBulk(String index, String type, Collection<Map<String, ?>> data)
            throws Exception
    {
        BulkRequestBuilder req = client.prepareBulk();

        for (Map<String, ?> item : data)
        {
            String id = item.containsKey("_id") ? String.valueOf((item.get("_id"))) : "";
            IndexRequestBuilder indexReq = client.prepareIndex(index, type);

            if (! id.isEmpty())
            {
                item.remove("_id");
                indexReq.setId(id);
            }

            indexReq.setSource( jsonMapper.writeValueAsString(item) );
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
