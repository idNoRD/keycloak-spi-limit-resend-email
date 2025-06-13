package idnord.keycloak.utilities;

import org.openqa.selenium.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserBehaviorTestUtils {

    private final WebDriver driver;
    private final static WebDriverManager wdm;

    private final String host = System.getProperty("test.host", "host.docker.internal");
    private final KeycloakTestUtils kc;

    static {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--no-sandbox");

        // Disable password manager leak detection
        Map<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("profile.password_manager_leak_detection", false);
        chromePrefs.put("credentials_enable_service", false);
        chromePrefs.put("profile.password_manager_enabled", false);
        chromeOptions.setExperimentalOption("prefs", chromePrefs);
        // Optional: Add arguments if needed
        chromeOptions.addArguments("--disable-infobars");
        chromeOptions.addArguments("--disable-notifications");

        wdm = WebDriverManager.chromedriver().capabilities(chromeOptions)
                //.enableVnc().enableRecording() // uncomment for demo purposes or manual testing
                .browserInDocker();
    }

    public UserBehaviorTestUtils(KeycloakTestUtils kc) {
        this.kc = kc;
        driver = wdm.create();
        driver.manage().window().maximize();
    }

    public void attemptLogin(String username, String password, String expectedUrl) {
        driver.get("http://" + host + ":" + kc.getKcHttpPort() + "/realms/master/account");

        // Wait until username input is present (optional but recommended)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        // Fill username
        WebElement usernameInput = driver.findElement(By.id("username"));
        usernameInput.clear();
        usernameInput.sendKeys(username);

        // Fill password
        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        // Click login button
        WebElement loginButton = driver.findElement(By.id("kc-login"));
        loginButton.click();

        // Optionally wait for next page to load or check login success/failure
        wait.until(ExpectedConditions.urlContains(expectedUrl));
    }

    public void tearDown() {
        System.out.println("Tearing down ub start kcHttpPort=" + kc.getKcHttpPort());
        if (wdm != null) {
            wdm.quit();
        }
        System.out.println("Tearing down ub end kcHttpPort=" + kc.getKcHttpPort());
    }

    public boolean clickResend(String expectedTextInThePage, String expectedUrl) {
        WebElement clickHereToResendVerificationEmail = driver.findElement(By.xpath("//a[contains(., 'Click here')]"));
        clickHereToResendVerificationEmail.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.urlContains(expectedUrl));

        return Objects.requireNonNull(driver.getPageSource()).contains(expectedTextInThePage);
    }

}
