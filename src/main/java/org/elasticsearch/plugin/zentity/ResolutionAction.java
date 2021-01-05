package org.elasticsearch.plugin.zentity;

import io.zentity.model.Model;
import io.zentity.resolution.Job;
import io.zentity.resolution.Job.JobResult;
import io.zentity.resolution.input.Input;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.plugin.zentity.exceptions.BadRequestException;
import org.elasticsearch.plugin.zentity.exceptions.NotFoundException;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import static org.elasticsearch.rest.RestRequest.Method.POST;

public class ResolutionAction extends BaseAction {

    @Inject
    ResolutionAction(RestController controller) {
        controller.registerHandler(POST, "_zentity/resolution", this);
        controller.registerHandler(POST, "_zentity/resolution/{entity_type}", this);
    }

    @Override
    public String getName() {
        return "zentity_resolution_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) {

        String body = restRequest.content().utf8ToString();

        // Parse the request params that will be passed to the job configuration
        String entityType = restRequest.param("entity_type");
        boolean includeAttributes = restRequest.paramAsBoolean("_attributes", Job.DEFAULT_INCLUDE_ATTRIBUTES);
        boolean includeErrorTrace = restRequest.paramAsBoolean("error_trace", Job.DEFAULT_INCLUDE_ERROR_TRACE);
        boolean includeExplanation = restRequest.paramAsBoolean("_explanation", Job.DEFAULT_INCLUDE_EXPLANATION);
        boolean includeHits = restRequest.paramAsBoolean("hits", Job.DEFAULT_INCLUDE_HITS);
        boolean includeQueries = restRequest.paramAsBoolean("queries", Job.DEFAULT_INCLUDE_QUERIES);
        boolean includeScore = restRequest.paramAsBoolean("_score", Job.DEFAULT_INCLUDE_SCORE);
        boolean includeSeqNoPrimaryTerm = restRequest.paramAsBoolean("_seq_no_primary_term", Job.DEFAULT_INCLUDE_SEQ_NO_PRIMARY_TERM);
        boolean includeSource = restRequest.paramAsBoolean("_source", Job.DEFAULT_INCLUDE_SOURCE);
        boolean includeVersion = restRequest.paramAsBoolean("_version", Job.DEFAULT_INCLUDE_VERSION);
        int maxDocsPerQuery = restRequest.paramAsInt("max_docs_per_query", Job.DEFAULT_MAX_DOCS_PER_QUERY);
        int maxHops = restRequest.paramAsInt("max_hops", Job.DEFAULT_MAX_HOPS);
        String maxTimePerQuery = restRequest.param("max_time_per_query", Job.DEFAULT_MAX_TIME_PER_QUERY);
        boolean pretty = restRequest.paramAsBoolean("pretty", Job.DEFAULT_PRETTY);
        boolean profile = restRequest.paramAsBoolean("profile", Job.DEFAULT_PROFILE);

        // Parse any optional search parameters that will be passed to the job configuration.
        // Note: org.elasticsearch.rest.RestRequest doesn't allow null values as default values for integer parameters,
        // which is why the code below handles the integer parameters differently from the others.
        boolean searchAllowPartialSearchResults = restRequest.paramAsBoolean("search.allow_partial_search_results", Job.DEFAULT_SEARCH_ALLOW_PARTIAL_SEARCH_RESULTS);

        int searchBatchedReduceSize = Job.DEFAULT_SEARCH_BATCHED_REDUCE_SIZE;
        if (restRequest.hasParam("search.batched_reduce_size")) {
            searchBatchedReduceSize = Integer.parseInt(restRequest.param("search.batched_reduce_size"));
        }


        int searchMaxConcurrentShardRequests = Job.DEFAULT_SEARCH_MAX_CONCURRENT_SHARD_REQUESTS;
        if (restRequest.hasParam("search.max_concurrent_shard_requests"))
            searchMaxConcurrentShardRequests = Integer.parseInt(restRequest.param("search.max_concurrent_shard_requests"));


        int searchPreFilterShardSize = Job.DEFAULT_SEARCH_PRE_FILTER_SHARD_SIZE;
        if (restRequest.hasParam("search.pre_filter_shard_size")) {
            searchPreFilterShardSize = Integer.parseInt(restRequest.param("search.pre_filter_shard_size"));
        }

        String searchPreference = restRequest.param("search.preference", Job.DEFAULT_SEARCH_PREFERENCE);
        boolean searchRequestCache = restRequest.paramAsBoolean("search.request_cache", Job.DEFAULT_SEARCH_REQUEST_CACHE);

        int finalSearchBatchedReduceSize = searchBatchedReduceSize;
        int finalSearchMaxConcurrentShardRequests = searchMaxConcurrentShardRequests;
        int finalSearchPreFilterShardSize = searchPreFilterShardSize;

        return wrappedConsumer(channel -> {
            // Validate the request body.
            if (body == null || body.equals("")) {
                throw new BadRequestException("Request body is missing.");
            }

            // Parse and validate the job input.
            Input input;
            if (entityType == null || entityType.equals("")) {
                input = new Input(body);
            } else {
                GetResponse getResponse = ModelsAction.getEntityModel(entityType, client);
                if (!getResponse.isExists())
                    throw new NotFoundException("Entity type '" + entityType + "' not found.");
                String model = getResponse.getSourceAsString();
                input = new Input(body, new Model(model));
            }

            // Prepare the entity resolution job.
            Job job = Job.newBuilder()
                .client(client)
                .includeAttributes(includeAttributes)
                .includeErrorTrace(includeErrorTrace)
                .includeExplanation(includeExplanation)
                .includeHits(includeHits)
                .includeQueries(includeQueries)
                .includeScore(includeScore)
                .includeSeqNoPrimaryTerm(includeSeqNoPrimaryTerm)
                .includeSource(includeSource)
                .includeVersion(includeVersion)
                .maxDocsPerQuery(maxDocsPerQuery)
                .maxHops(maxHops)
                .maxTimePerQuery(maxTimePerQuery)
                .pretty(pretty)
                .profile(profile)
                .input(input)
                // Optional search parameters
                .searchAllowPartialSearchResults(searchAllowPartialSearchResults)
                .searchBatchedReduceSize(finalSearchBatchedReduceSize)
                .searchMaxConcurrentShardRequests(finalSearchMaxConcurrentShardRequests)
                .searchPreFilterShardSize(finalSearchPreFilterShardSize)
                .searchPreference(searchPreference)
                .searchRequestCache(searchRequestCache)
                .build();

            // Run the entity resolution job.
            JobResult res = job.run();
            if (res.failed()) {
                channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "application/json", res.getResponse()));
            } else {
                channel.sendResponse(new BytesRestResponse(RestStatus.OK, "application/json", res.getResponse()));
            }
        });
    }
}
