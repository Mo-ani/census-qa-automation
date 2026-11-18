package com.census.qa.driver;

import com.census.qa.config.Config;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a configured browser. The matching driver binary is resolved by Selenium Manager,
 * so nothing has to be downloaded or put on the PATH by hand.
 *
 * <p>The factory holds no state: every scenario owns its own driver through the TestContext,
 * which keeps scenarios isolated and makes parallel execution possible later.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        String browser = Config.get("browser").toLowerCase();
        boolean headless = Config.getBoolean("headless");
        String[] size = Config.get("window.size").split(",");
        int width = Integer.parseInt(size[0].trim());
        int height = Integer.parseInt(size[1].trim());

        WebDriver driver;
        switch (browser) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--lang=en-US", "--window-size=" + width + "," + height);
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.setExperimentalOption("prefs", quietChromiumPrefs());
                driver = new ChromeDriver(options);
            }
            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                options.addArguments("--lang=en-US", "--window-size=" + width + "," + height);
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.setExperimentalOption("prefs", quietChromiumPrefs());
                driver = new EdgeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.addPreference("intl.accept_languages", "en-US");
                if (headless) {
                    options.addArguments("-headless");
                }
                driver = new FirefoxDriver(firefoxService(), options);
                driver.manage().window().setSize(new Dimension(width, height));
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + browser + "'. Use chrome, edge or firefox.");
        }

        overrideTimeZone(driver);
        fitViewport(driver);
        return driver;
    }

    /** Switches off the password-manager and "password found in a data breach" pop-ups that cover the page. */
    private static Map<String, Object> quietChromiumPrefs() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        prefs.put("intl.accept_languages", "en-US");
        return prefs;
    }

    /**
     * The dashboard prints the date of birth with the BROWSER's time zone while the server
     * stores it in its own (UTC in Docker). Pinning the browser to the configured zone keeps
     * the displayed date deterministic on any machine.
     * Chrome and Edge: through the DevTools protocol, after the browser has started.
     */
    private static void overrideTimeZone(WebDriver driver) {
        String zone = Config.getOrDefault("browser.timezone", "");
        if (!zone.isEmpty() && driver instanceof ChromiumDriver chromium) {
            chromium.executeCdpCommand("Emulation.setTimezoneOverride", Map.of("timezoneId", zone));
        }
    }

    /**
     * The record dialog of the application cannot be scrolled: on a small or scaled screen (a
     * laptop at 150 %, a projector) a visible browser window is too short and the dialog's
     * buttons end up outside the page. When the viewport is lower than min.viewport.height, the
     * page is zoomed out just enough to fit - the same thing a user would do with Ctrl and minus.
     * Headless runs get the configured window size and are not affected. Chrome and Edge only.
     */
    private static void fitViewport(WebDriver driver) {
        int wantedHeight = Integer.parseInt(Config.getOrDefault("min.viewport.height", "0"));
        if (wantedHeight <= 0 || !(driver instanceof ChromiumDriver chromium)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Number> metrics = (List<Number>) ((JavascriptExecutor) driver).executeScript(
                "return [window.innerWidth, window.innerHeight, window.devicePixelRatio];");
        double width = metrics.get(0).doubleValue();
        double height = metrics.get(1).doubleValue();
        double pixelRatio = metrics.get(2).doubleValue();
        if (height >= wantedHeight) {
            return;
        }
        double zoom = height / wantedHeight;
        Map<String, Object> viewport = new HashMap<>();
        viewport.put("width", (int) Math.round(width / zoom));
        viewport.put("height", wantedHeight);
        viewport.put("deviceScaleFactor", pixelRatio * zoom);
        viewport.put("mobile", false);
        chromium.executeCdpCommand("Emulation.setDeviceMetricsOverride", viewport);
    }

    /** Firefox has no such command; it takes its time zone from the TZ environment variable at start-up. */
    private static GeckoDriverService firefoxService() {
        GeckoDriverService.Builder builder = new GeckoDriverService.Builder();
        String zone = Config.getOrDefault("browser.timezone", "");
        if (!zone.isEmpty()) {
            builder.withEnvironment(Map.of("TZ", zone));
        }
        return builder.build();
    }
}
