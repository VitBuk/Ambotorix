package vitbuk.com.Ambotorix.photochallenge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Production fetcher: GETs the configured Google Sheets CSV export URL. */
@Component
public class HttpSheetCsvFetcher implements SheetCsvFetcher {

    @Value("${photochallenge.sheet.csv-url:https://docs.google.com/spreadsheets/d/1_EUcRfUmpd2LMvm2n0uA08DfSJKEueesyExspFfEE70/export?format=csv&gid=1385678622}")
    private String csvUrl;

    @Override
    public String fetch() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(csvUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Sheet CSV request returned HTTP " + response.statusCode());
        }
        return response.body();
    }
}
