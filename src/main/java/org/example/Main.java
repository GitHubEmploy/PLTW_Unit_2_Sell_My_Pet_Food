import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.safari.SafariDriver;

import org.tensorflow.ConcreteFunction;
import org.tensorflow.Signature;
import org.tensorflow.Tensor;
import org.tensorflow.types.TString;
import static com.sun.tools.classfile.Attribute.Signature;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class Main {

    static class SentimentAnalyzer {
        private static final long DEFAULT_SERVING = 55;
        private ConcreteFunction model;

        public SentimentAnalyzer(String modelDir) {
            model = ConcreteFunction.load(Paths.get(modelDir));
        }

        public float analyze(String text) {
            try (TString inputTensor = TString.scalarOf(text)) {
                try (Tensor<?> outputTensor = model.wait(DEFAULT_SERVING, inputTensor).get(0)) {
                    float[][] sentimentScores = outputTensor.copyTo(new float[1][2]); // Assuming binary classification: [negative, positive]
                    return sentimentScores[0][1] - sentimentScores[0][0]; // Return difference as a simple score
                }
            }
        }
    }

    public static void main(String[] args) {
        WebDriver driver = new SafariDriver();
        driver.get("https://www.amazon.com");
        searchForProduct(driver, "computer charger");

        List<String> data = extractReviews(driver);
        SentimentAnalyzer analyzer = new SentimentAnalyzer("./src/main/java/org.example/");
        List<String> socialMediaPosts = new ArrayList<>();
        List<String> goodAdvertisements = new ArrayList<>();
        List<String> badAdvertisements = new ArrayList<>();

        analyzeAndProcessReviews(data, analyzer, socialMediaPosts, goodAdvertisements, badAdvertisements);

        try {
            writeToFile("socialmediaposts.txt", new HashSet<>(socialMediaPosts));
            writeAdvertisements(goodAdvertisements, badAdvertisements);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Process completed.");
        driver.quit();
    }

    private static void searchForProduct(WebDriver driver, String query) {
        WebElement searchBar = driver.findElement(By.xpath("//input[@id='twotabsearchtextbox']"));
        searchBar.clear();
        searchBar.sendKeys(query + Keys.RETURN);
        delay(3000); // Simulate page load time
    }

    private static List<String> extractReviews(WebDriver driver) {
        List<String> reviews = new ArrayList<>();
        // Scroll to load all reviews
        scrollToEndOfPage(driver);
        List<WebElement> reviewElements = driver.findElements(By.xpath("//span[@data-hook='review-body']")); // Update XPath based on the actual structure
        for (WebElement element : reviewElements) {
            reviews.add(element.getText());
        }
        return reviews;
    }

    private static void scrollToEndOfPage(WebDriver driver) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        for (int i = 0; i < 5; i++) { // Adjust the iteration count as needed
            jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            delay(2000); // Wait for page to load
        }
    }

    private static void analyzeAndProcessReviews(List<String> data, SentimentAnalyzer analyzer, List<String> socialMediaPosts, List<String> goodAdvertisements, List<String> badAdvertisements) {
        Pattern reviewPattern = Pattern.compile("(.*)");
        for (String review : data) {
            Matcher matcher = reviewPattern.matcher(review);
            while (matcher.find()) {
                String reviewText = matcher.group(1);
                float sentimentScore = analyzer.analyze(reviewText);
                if (sentimentScore > 0) {
                    goodAdvertisements.add(reviewText);
                    socialMediaPosts.add("\"" + reviewText + "\"");
                } else {
                    badAdvertisements.add(reviewText);
                }
            }
        }
    }

    private static void writeToFile(String fileName, Set<String> lines) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
    }

    private static void writeAdvertisements(List<String> goodAds, List<String> badAds) throws IOException {
        writeToFile("advertisement.txt", new HashSet<>(goodAds));
        // writeToFile("bad_advertisements.txt", new HashSet<>(badAds));
    }

    private static void delay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
