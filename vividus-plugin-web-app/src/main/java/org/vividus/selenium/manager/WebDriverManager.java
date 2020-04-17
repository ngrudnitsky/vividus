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

import java.util.stream.Stream;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.remote.BrowserType;
import org.vividus.selenium.BrowserWindowSize;
import org.vividus.selenium.WebDriverType;
import org.vividus.selenium.WebDriverUtil;

public class WebDriverManager extends DriverManager implements IWebDriverManager
{
    private boolean electronApp;

    @Override
    public void resize(BrowserWindowSize browserWindowSize)
    {
        if (isElectronApp())
        {
            return;
        }
        WebDriver webDriver = getWebDriver();
        resize(webDriver, browserWindowSize);
        // Chrome-only workaround for situations when custom browser viewport size was set before 'window.maximize();'
        // and it prevents following window-resizing actions
        // Reported issue: https://bugs.chromium.org/p/chromedriver/issues/detail?id=1638
        if (isBrowserAnyOf(BrowserType.CHROME) && !isAndroid())
        {
            Window window = getWebDriver().manage().window();
            Dimension size = window.getSize();
            window.setSize(size);
        }
    }

    public void resize(WebDriver webDriver, BrowserWindowSize browserWindowSize)
    {
        if (isElectronApp() || isMobile(WebDriverUtil.unwrap(webDriver, HasCapabilities.class).getCapabilities()))
        {
            return;
        }
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

    @Override
    public boolean isTypeAnyOf(WebDriverType... webDriverTypes)
    {
        return isTypeAnyOf(getWebDriver(), webDriverTypes);
    }

    public static boolean isTypeAnyOf(WebDriver webDriver, WebDriverType... webDriverTypes)
    {
        Capabilities capabilities = getCapabilities(webDriver);
        return Stream.of(webDriverTypes).anyMatch(type -> isBrowserAnyOf(capabilities, type.getBrowserNames()));
    }

    @Override
    public WebDriverType detectType()
    {
        return detectType(getCapabilities());
    }

    public static WebDriverType detectType(Capabilities capabilities)
    {
        return Stream.of(WebDriverType.values()).filter(type -> isBrowserAnyOf(capabilities, type.getBrowserNames()))
                .findFirst().orElse(null);
    }

    @Override
    public boolean isElectronApp()
    {
        return electronApp;
    }

    public void setElectronApp(boolean electronApp)
    {
        this.electronApp = electronApp;
    }
}
