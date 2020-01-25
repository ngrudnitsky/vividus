/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.selenium;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.vividus.selenium.manager.WebDriverManager;
import org.vividus.util.json.IJsonUtils;
import org.vividus.util.property.IPropertyParser;

public class WebDriverFactory extends DriverFactory implements IWebDriverFactory
{
    private static final String COMMAND_LINE_ARGUMENTS = "command-line-arguments";

    @Inject private ITimeoutConfigurer timeoutConfigurer;
    @Inject private IPropertyParser propertyParser;
    private WebDriverType webDriverType;

    private IJsonUtils jsonUtils;

    private final ConcurrentHashMap<WebDriverType, WebDriverConfiguration> configurations = new ConcurrentHashMap<>();

    @Override
    public WebDriver getWebDriver(DesiredCapabilities desiredCapabilities)
    {
        return getWebDriver(webDriverType, desiredCapabilities);
    }

    @Override
    public WebDriver getWebDriver(WebDriverType webDriverType, DesiredCapabilities desiredCapabilities)
    {
        WebDriverConfiguration configuration = getWebDriverConfiguration(webDriverType, true);
        return createWebDriver(webDriverType.getWebDriver(desiredCapabilities, configuration));
    }

    @Override
    public WebDriver getWebDriver(String webDriverType, DesiredCapabilities desiredCapabilities)
    {
        return webDriverType != null ? getWebDriver(WebDriverType.valueOf(webDriverType), desiredCapabilities)
                : getWebDriver(desiredCapabilities);
    }

    @Override
    protected DesiredCapabilities updateDesiredCapabilities(DesiredCapabilities desiredCapabilities)
    {
        WebDriverType webDriverType = WebDriverManager.detectType(desiredCapabilities);

        Capabilities capabilities = desiredCapabilities;
        if (webDriverType != null)
        {
            webDriverType.prepareCapabilities(desiredCapabilities);
            if (webDriverType == WebDriverType.CHROME)
            {
                WebDriverConfiguration configuration = getWebDriverConfiguration(webDriverType, false);
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments(configuration.getCommandLineArguments());
                configuration.getExperimentalOptions().forEach(chromeOptions::setExperimentalOption);
                capabilities = chromeOptions.merge(desiredCapabilities);
            }
        }
        return new DesiredCapabilities(capabilities);
    }

    @Override
    protected void configureWebDriver(WebDriver webDriver)
    {
        timeoutConfigurer.configure(webDriver.manage().timeouts());
    }

    private WebDriverConfiguration getWebDriverConfiguration(WebDriverType webDriverType, boolean localRun)
    {
        return configurations.computeIfAbsent(webDriverType, type ->
        {
            WebDriverConfiguration configuration = createWebDriverConfiguration(type);
            if (localRun)
            {
                webDriverType.setDriverExecutablePath(configuration.getDriverExecutablePath());
            }
            return configuration;
        });
    }

    @SuppressWarnings("unchecked")
    private WebDriverConfiguration createWebDriverConfiguration(WebDriverType webDriverType)
    {
        Optional<String> binaryPath = getPropertyValue("binary-path", webDriverType);
        if (binaryPath.isPresent() && !webDriverType.isBinaryPathSupported())
        {
            throw new UnsupportedOperationException("Configuring of binary-path is not supported for " + webDriverType);
        }

        Optional<String> commandLineArguments = getPropertyValue(COMMAND_LINE_ARGUMENTS, webDriverType);
        if (commandLineArguments.isPresent() && !webDriverType.isCommandLineArgumentsSupported())
        {
            throw new UnsupportedOperationException(
                    "Configuring of command-line-arguments is not supported for " + webDriverType);
        }

        WebDriverConfiguration configuration = new WebDriverConfiguration();
        configuration.setDriverExecutablePath(getPropertyValue("driver-executable-path", webDriverType));
        configuration.setBinaryPath(binaryPath);
        configuration.setCommandLineArguments(
                commandLineArguments.map(args -> StringUtils.split(args, ' ')).orElseGet(() -> new String[0]));
        getPropertyValue("experimental-options", webDriverType)
                .map(options -> jsonUtils.toObject(options, Map.class))
                .ifPresent(configuration::setExperimentalOptions);
        return configuration;
    }

    private Optional<String> getPropertyValue(String propertyKey, WebDriverType webDriverType)
    {
        return Optional.ofNullable(propertyParser.getPropertyValue("web.driver." + webDriverType + "." + propertyKey));
    }

    public void setWebDriverType(WebDriverType webDriverType)
    {
        this.webDriverType = webDriverType;
    }

    public void setJsonUtils(IJsonUtils jsonUtils)
    {
        this.jsonUtils = jsonUtils;
    }
}
