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

package org.vividus.selenium.manager;

import static org.vividus.selenium.manager.DriverManager.getCapabilities;
import static org.vividus.selenium.manager.DriverManager.isBrowserAnyOf;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.remote.BrowserType;
import org.vividus.selenium.BrowserWindowSize;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.WebDriverType;
import org.vividus.selenium.WebDriverUtil;

public class WebDriverManager extends DriverManager implements IWebDriverManager
{
    @Inject private IWebDriverProvider webDriverProvider;

    @Override
    public void resize(BrowserWindowSize browserWindowSize)
    {
        WebDriver webDriver = webDriverProvider.get();
        resize(webDriver, browserWindowSize);
        // Chrome-only workaround for situations when custom browser viewport size was set before 'window.maximize();'
        // and it prevents following window-resizing actions
        // Reported issue: https://bugs.chromium.org/p/chromedriver/issues/detail?id=1638
        if (isBrowserAnyOf(BrowserType.CHROME) && !isAndroid())
        {
            Window window = webDriver.manage().window();
            Dimension size = window.getSize();
            window.setSize(size);
        }
    }

    public static void resize(WebDriver webDriver, BrowserWindowSize browserWindowSize)
    {
        if (!DriverManager.isMobile(WebDriverUtil.unwrap(webDriver, HasCapabilities.class).getCapabilities()))
        {
            Window window = webDriver.manage().window();
            if (browserWindowSize == null)
            {
                window.maximize();
            }
            else
            {
                window.setSize(browserWindowSize.toDimension());
            }
        }
    }

    @Override
    public boolean isTypeAnyOf(WebDriverType... webDriverTypes)
    {
        return isTypeAnyOf(getWebDriver(), webDriverTypes);
    }

    @Override
    public WebDriverType detectType()
    {
        return detectType(getCapabilities());
    }

    public static boolean isTypeAnyOf(WebDriver webDriver, WebDriverType... webDriverTypes)
    {
        Capabilities capabilities = getCapabilities(webDriver);
        return Stream.of(webDriverTypes).anyMatch(type -> isBrowserAnyOf(capabilities, type.getBrowserNames()));
    }

    public static WebDriverType detectType(Capabilities capabilities)
    {
        return Stream.of(WebDriverType.values()).filter(type -> isBrowserAnyOf(capabilities, type.getBrowserNames()))
                .findFirst().orElse(null);
    }

    private WebDriver getWebDriver()
    {
        return webDriverProvider.get();
    }
}
