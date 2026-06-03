import org.springframework.web.util.DefaultUriBuilderFactory;
import java.net.URI;

public class TestRestClient {
    public static void main(String[] args) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://dev-online-gateway.ghn.vn/shiip/public-api/v2/");
        String overrideUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/province";
        URI uri = factory.expand(overrideUrl);
        System.out.println(uri.toString());
    }
}
