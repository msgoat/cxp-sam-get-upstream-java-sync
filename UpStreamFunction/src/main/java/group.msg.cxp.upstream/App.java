package group.msg.cxp.upstream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private LambdaClient client;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders);

        try {
            String downStreamMessage = invokeDownStreamFunction();

            StringBuilder output = new StringBuilder();
            output.append("{ ");
            output.append("\"message\":").append("\"hello world from upstream\"");
            output.append(",\"downstream\":").append(downStreamMessage);
            output.append(" }");

            return response
                    .withStatusCode(200)
                    .withBody(output.toString());
        } catch (Exception e) {
            log.error("Unable to process event", e);
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private String invokeDownStreamFunction() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName(getDownStreamFunctionName())
                .invocationType(InvocationType.REQUEST_RESPONSE)
                .build();
        InvokeResponse response = getLambdaClient().invoke(request);
        String result = response.payload().asUtf8String();
        return result;
    }

    /**
     * Lazily builds a {@code LambdaClient} using the {@code UrlConnectionHttpClient}.
     * <p>
     * Note: It's essential to specify a concrete HTTP client implementation! Otherwise
     * the function will crash on AWS lambda.
     * </p>
     */
    private LambdaClient getLambdaClient() {
        if (this.client == null) {
            this.client = LambdaClient.builder()
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .build();
        }
        return this.client;
    }

    private String getDownStreamFunctionName() {
        String result = System.getenv("CXP_DOWNSTREAM_FUNCTION_NAME");
        if (result == null) {
            result = "arn:aws:lambda:eu-west-1:005913962162:function:cxp-sam-get-downstream-ja-CxpSamGetDownstreamJavaF-UaspRGJei3GV";
        }
        return result;
    }
}
