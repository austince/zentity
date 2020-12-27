package io.zentity.common;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;

import java.util.concurrent.CompletableFuture;

public class ActionRequestUtil {
    /**
     * Wrap an {@link ActionRequestBuilder} in a {@link CompletableFuture}.
     *
     * @see <a href="https://mincong.io/2020/07/26/es-client-completablefuture/"></a>
     * @param reqBuilder The request builder.
     * @param <T> The response type.
     * @return A completable future that resolves with the response.
     */
    public static <T extends ActionResponse> CompletableFuture<T> toCompletableFuture(ActionRequestBuilder<?, T> reqBuilder) {
        CompletableFuture<T> fut = new CompletableFuture<>();
        reqBuilder.execute(ActionListener.wrap(fut::complete, fut::completeExceptionally));
        return fut;
    }
}
