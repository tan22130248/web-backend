import org.springframework.web.client.RestClient;
public class TestRestClientUri {
    public static void main(String[] args) {
        RestClient client = RestClient.builder().baseUrl("https://example.com/api/v2/").build();
        // Since we are not in a spring context, we can't easily compile this without classpath.
    }
}
