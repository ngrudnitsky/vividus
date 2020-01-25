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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.remote.BrowserType;
import org.vividus.selenium.BrowserWindowSize;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.WebDriverType;

@ExtendWith(MockitoExtension.class)
class WebDriverManagerTests
{
    private static final String BROWSER_TYPE = "browser type";

    @Mock
    private IWebDriverProvider webDriverProvider;

    @InjectMocks
    private WebDriverManager webDriverManager;

    private WebDriver mockWebDriver(Class<? extends WebDriver> webDriverClass, MockSettings mockSettings)
    {
        WebDriver webDriver = mock(webDriverClass, mockSettings);
        when(webDriverProvider.get()).thenReturn(webDriver);
        return webDriver;
    }

    private Options mockOptions(WebDriver webDriver)
    {
        Options options = mock(Options.class);
        when(webDriver.manage()).thenReturn(options);
        return options;
    }

    @Test
    void testResizeWebDriverWithDesiredBrowserSize()
    {
        WebDriver webDriver = mockWebDriver(WebDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        BrowserWindowSize browserWindowSize = mock(BrowserWindowSize.class);
        when(((HasCapabilities) webDriver).getCapabilities()).thenReturn(mock(Capabilities.class));
        Window window = mock(Window.class);
        when(mockOptions(webDriver).window()).thenReturn(window);
        Dimension dimension = mock(Dimension.class);
        when(browserWindowSize.toDimension()).thenReturn(dimension);
        webDriverManager.resize(browserWindowSize);
        verify(window).setSize(dimension);
    }

    @Test
    void testResizeWebDriverWithNullBrowserSize()
    {
        WebDriver webDriver = mockWebDriver(WebDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        Window window = mock(Window.class);
        when(mockOptions(webDriver).window()).thenReturn(window);
        webDriverManager.resize(null);
        verify(window).maximize();
    }

    @Test
    void testResizeWebDriverWithNullBrowserSizeChrome()
    {
        WebDriverManager spy = Mockito.spy(webDriverManager);
        WebDriver webDriver = mockWebDriver(WebDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        Window window = mock(Window.class);
        when(mockOptions(webDriver).window()).thenReturn(window);
        Dimension maxSize = new Dimension(1920, 1200);
        when(window.getSize()).thenReturn(maxSize);
        doReturn(true).when(spy).isBrowserAnyOf(BrowserType.CHROME);
        spy.resize(null);
        verify(window).maximize();
        verify(window).setSize(maxSize);
    }

    // CHECKSTYLE:OFF
    static Stream<Arguments> webDriverTypeChecks()
    {
        return Stream.of(
            Arguments.of(BrowserType.FIREFOX,         WebDriverType.FIREFOX,      true   ),
            Arguments.of(BrowserType.FIREFOX_CHROME,  WebDriverType.FIREFOX,      false  ),
            Arguments.of(BrowserType.CHROME,          WebDriverType.CHROME,       true   ),
            Arguments.of(BrowserType.FIREFOX_CHROME,  WebDriverType.CHROME,       false  ),
            Arguments.of(BrowserType.EDGE,            WebDriverType.EDGE,         true   ),
            Arguments.of(BrowserType.IE,              WebDriverType.EDGE,         false  ),
            Arguments.of(BrowserType.IEXPLORE,        WebDriverType.IEXPLORE,     true   ),
            Arguments.of(BrowserType.IE_HTA,          WebDriverType.IEXPLORE,     false  ),
            Arguments.of(BrowserType.SAFARI,          WebDriverType.SAFARI,       true   ),
            Arguments.of(BrowserType.SAFARI_PROXY,    WebDriverType.SAFARI,       true   ),
            Arguments.of(BrowserType.IPHONE,          WebDriverType.SAFARI,       false  )
        );
    }

    static Stream<Arguments> webDriverTypeDetections()
    {
        return Stream.of(
            Arguments.of(BrowserType.FIREFOX,         WebDriverType.FIREFOX   ),
            Arguments.of(BrowserType.FIREFOX_CHROME,  null                    ),
            Arguments.of(BrowserType.CHROME,          WebDriverType.CHROME    ),
            Arguments.of(BrowserType.FIREFOX_CHROME,  null                    ),
            Arguments.of(BrowserType.EDGE,            WebDriverType.EDGE      ),
            Arguments.of(BrowserType.IE,              WebDriverType.IEXPLORE  ),
            Arguments.of(BrowserType.IEXPLORE,        WebDriverType.IEXPLORE  ),
            Arguments.of(BrowserType.IE_HTA,          null                    ),
            Arguments.of(BrowserType.SAFARI,          WebDriverType.SAFARI    ),
            Arguments.of(BrowserType.SAFARI_PROXY,    WebDriverType.SAFARI    ),
            Arguments.of(BrowserType.IPHONE,          null                    )
        );
    }
    // CHECKSTYLE:ON

    @ParameterizedTest
    @MethodSource("webDriverTypeChecks")
    void testIsTypeAnyOf(String browserName, WebDriverType webDriverType, boolean result)
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, browserName);
        assertEquals(result, webDriverManager.isTypeAnyOf(webDriverType));
    }

    @ParameterizedTest
    @MethodSource("webDriverTypeDetections")
    void testDetectType(String browserName, WebDriverType webDriverType)
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, browserName);
        assertEquals(webDriverType, webDriverManager.detectType());
    }

    private HasCapabilities mockWebDriverWithCapabilities()
    {
        WebDriver webDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        return (HasCapabilities) webDriver;
    }

    private Capabilities mockCapabilities(HasCapabilities havingCapabilitiesWebDriver)
    {
        Capabilities capabilities = mock(Capabilities.class);
        when(havingCapabilitiesWebDriver.getCapabilities()).thenReturn(capabilities);
        return capabilities;
    }

    private void setCapabilities(HasCapabilities havingCapabilitiesWebDriver, String capabilityType,
            Object capabilityValue)
    {
        if (BROWSER_TYPE.equals(capabilityType))
        {
            when(mockCapabilities(havingCapabilitiesWebDriver).getBrowserName()).thenReturn((String) capabilityValue);
        }
        else
        {
            lenient().when(mockCapabilities(havingCapabilitiesWebDriver).getCapability(capabilityType))
                    .thenReturn(capabilityValue);
        }
    }
}
