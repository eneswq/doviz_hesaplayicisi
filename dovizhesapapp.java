package tablo;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale; // main metodu için gerekebilir
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.geometry.Pos; // Hizalama için
import javafx.geometry.Insets; // Kenar boşluğu için


public class dovizhesapapp extends Application {
    // Sınıfın içine, start metodunun dışına yapıştırılacak
    private static final String API_URL_TEMPLATE =
            "https://open.er-api.com/v6/latest/%s";

    // JSON kullanmadan "rates":{"TRY":XX.XXXX} kalıbından kuru okur
    private static double parseRate(String json, String targetCurrency) {
        // Regex deseni: "HEDEF_PARA_BİRİMİ": SAYI
        String regex = "\"" + Pattern.quote(targetCurrency) + "\"\\s*:\\s*([0-9.]+)";
        Matcher m = Pattern.compile(regex).matcher(json);

        if (!m.find()) {
            throw new RuntimeException("Hedef para birimi (" + targetCurrency + ") API yanıtında bulunamadı.");
        }
        return Double.parseDouble(m.group(1));
    }

    // API'den döviz kurunu çeken asıl metot
    public static double getRate(String baseCurrency, String targetCurrency) throws Exception {
        String url = String.format(API_URL_TEMPLATE,
                URLEncoder.encode(baseCurrency, StandardCharsets.UTF_8));

        // HTTP Kütüphanelerinin Çalışması için bu kod ZORUNLUDUR
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API isteği başarısız. HTTP code: " + response.statusCode());
        }

        if (response.body().contains("\"result\":\"error\"")) {
            throw new RuntimeException("API isteği başarısız. Lütfen girilen para birimi kodlarını kontrol edin.");
        }

        return parseRate(response.body(), targetCurrency);
    }

    @Override
    public void start (Stage primaryStage) throws Exception{

        GridPane grid = new GridPane();
        // GridPane ayarları (estetik ve kolay kullanım için)
        grid.setPadding(new Insets(30));    // Kenar boşluğu
        grid.setHgap(10);                   // Sütunlar arası boşluk
        grid.setVgap(10);                   // Satırlar arası boşluk
        grid.setAlignment(Pos.CENTER);      // Pencerede ortala

// Arka plan rengini GridPane'e uygulayalım
        grid.setStyle("-fx-background-color: white; -fx-font-family: 'Arial';");

        // --- GİRİŞ KUTULARININ (TEXTFIELD) GÜNCELLENMESİ ---

// Giriş Kutularına hafif bir gölge ve yuvarlaklık ekliyoruz
        String inputStyle = "-fx-background-color: #f7f7f7; " + // Hafif gri/beyaz arka plan
                "-fx-border-color: #bdc3c7; " +   // Açık gri kenarlık
                "-fx-border-radius: 5; " +        // Yuvarlak kenar
                "-fx-padding: 8; " +              // İç boşluk
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);"; // Hafif gölge

        TextField amountInput = new TextField();
        TextField baseInput = new TextField("EUR");
        TextField targetInput = new TextField("TRY");

        amountInput.setStyle(inputStyle);
        baseInput.setStyle(inputStyle);
        targetInput.setStyle(inputStyle);


// --- ETİKETLERİN (LABEL) GÜNCELLENMESİ ---

// Etiketlere koyu gri renk vererek okunurluğu artırıyoruz
        String labelStyle = "-fx-text-fill: #34495e; -fx-font-weight: bold;";

        Label amountLabel = new Label("Miktar:");
        Label baseLabel = new Label("Kaynak Para Birimi (örn:USD)");
        Label targetLabel = new Label("Hedef Para Birimi (örn:TRY)");

        amountLabel.setStyle(labelStyle);
        baseLabel.setStyle(labelStyle);
        targetLabel.setStyle(labelStyle);

        Button buton = new Button("Tıkla ve Hesapla");
        buton.setStyle(
                "-fx-background-color: #2c3e50;" + // Koyu Gri/Mavi Buton Rengi
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);"
        );

        // Sonuç ve Hata Mesajları için Etiket
        Label resultLabel = new Label("");

        buton.setOnAction(   e -> {

            // Tüm riskli işlemleri try-catch içine alıyoruz
            try {
                // A. Girdileri Oku ve Hazırla
                String base = baseInput.getText().trim().toUpperCase(Locale.ROOT);
                String target = targetInput.getText().trim().toUpperCase(Locale.ROOT);
                String amountText = amountInput.getText().trim().replace(',', '.');
                double amount = Double.parseDouble(amountText);

                // B. Kontrol: Aynı para birimi mi?
                if (base.equals(target)) {
                    resultLabel.setText(String.format("Aynı para birimi seçildi. Sonuç: %.2f %s", amount, target));
                    resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px;");
                    return;
                }

                // C. API'den Kuru Çek ve Hesapla
                // NOT: getRate metodu, sınıfın içine daha önce kopyaladığınız API fonksiyonudur.
                double rate = getRate(base, target);
                double converted = amount * rate;

                // D. Sonucu Görsel Etikete Yazdır (Başarı Durumu)
                String resultText = String.format(
                        "1 %s = %.4f %s\n%.2f %s = %.2f %s",
                        base, rate, target, amount, base, converted, target
                );

                resultLabel.setText(resultText);
                resultLabel.setStyle("-fx-text-fill: #020130; -fx-font-size: 16px;"); // Yeşil sonuç

            } catch (NumberFormatException ex) {
                // Sayı Formatı Hatası
                resultLabel.setText("HATA: Miktar alanına geçerli bir sayı girin.");
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 16px;"); // Kırmızı hata
            } catch (Exception ex) {
                // API Bağlantı veya Para Birimi Bulunamama Hatası
                resultLabel.setText("HATA: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 16px;"); // Kırmızı hata
            }

        });





        grid.add(amountLabel, 0, 0);
        grid.add(amountInput, 1, 0);

        grid.add(baseLabel, 0, 1);
        grid.add(baseInput, 1, 1);

        grid.add(targetLabel, 0, 2);
        grid.add(targetInput, 1, 2);

        // 4. SATIR: Hesapla Butonu (0. sütundan başla, 2 sütun genişliğinde yer kapla)
        grid.add(buton, 0, 3, 2, 1);

// 5. SATIR: Sonuç Etiketi
        grid.add(resultLabel, 0, 4, 2, 1);
        Scene sahne = new Scene (grid, 400 , 600);

        primaryStage.setTitle("Döviz Hesapla");
        primaryStage.setScene(sahne);
        primaryStage.show();


    }

    public static void main(String[] args){
        launch(args); //Java Fx e kontorolü devret.
    }
}
