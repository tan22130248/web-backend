public class TestUri {
    public static void main(String[] args) {
        String ghnApiUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/";
        System.out.println(ghnApiUrl.replace("/v2/", "") + "/master-data/province");
    }
}
