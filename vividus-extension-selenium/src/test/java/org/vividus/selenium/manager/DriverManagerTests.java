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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Rotatable;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.SauceLabsCapabilityType;

import io.appium.java_client.MobileDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.remote.MobilePlatform;

@SuppressWarnings({ "checkstyle:MethodCount", "PMD.GodClass"})
@ExtendWith(MockitoExtension.class)
class DriverManagerTests
{
    private static final String DESIREDCAPABILITIES_KEY = "desired";
    private static final String BROWSER_TYPE = "browser type";
    private static final String NATIVE_APP_CONTEXT = "NATIVE_APP";
    private static final String WEBVIEW_CONTEXT = "WEBVIEW_1";
    private static final Set<String> WINDOW_HANDLES = Set.of("windowHandle");
    private static final String COULD_NOT_CONNECT_TO_APP = "Could not connect to a valid app after 20 tries.";

    @Mock
    private IWebDriverProvider webDriverProvider;

    @Mock
    private IWebDriverManagerContext webDriverManagerContext;

    @Mock(extraInterfaces = HasCapabilities.class)
    private MobileDriver<MobileElement> mobileDriver;

    @InjectMocks
    private DriverManager driverManager;

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

    private Options mockOptions(WebDriver webDriver)
    {
        Options options = mock(Options.class);
        when(webDriver.manage()).thenReturn(options);
        return options;
    }

    private void mockMobileDriverContext(MobileDriver<?> mobileDriver, String returnContext,
            Set<String> returnContextHandles)
    {
        when(webDriverProvider.getUnwrapped(MobileDriver.class)).thenReturn(mobileDriver);
        when(mobileDriver.getContext()).thenReturn(returnContext);
        when(mobileDriver.getContextHandles()).thenReturn(returnContextHandles);
    }

    private DriverManager spyIsMobile(boolean returnValue)
    {
        DriverManager spy = Mockito.spy(driverManager);
        doReturn(returnValue).when(spy).isMobile();
        return spy;
    }

    @Test
    void testGetSize()
    {
        MobileDriver<?> mobileDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        Set<String> contexts = new HashSet<>();
        contexts.add(WEBVIEW_CONTEXT);
        contexts.add(NATIVE_APP_CONTEXT);
        mockMobileDriverContext(mobileDriver, WEBVIEW_CONTEXT, contexts);
        lenient().when(webDriverManagerContext.getParameter(WebDriverManagerParameter.SCREEN_SIZE)).thenReturn(null);
        lenient().when(webDriverManagerContext.getParameter(WebDriverManagerParameter.ORIENTATION))
                .thenReturn(ScreenOrientation.PORTRAIT);
        DriverManager spy = spyIsMobile(true);
        Window window = mock(Window.class);
        when(mockOptions(mobileDriver).window()).thenReturn(window);
        when(window.getSize()).thenReturn(mock(Dimension.class));
        assertNotNull(spy.getSize());
    }

    @Test
    void testPerformActionInNativeContextNotMobile()
    {
        WebDriver webDriverConfigured = mock(WebDriver.class);
        when(webDriverProvider.get()).thenReturn(webDriverConfigured);
        DriverManager spy = spyIsMobile(false);
        spy.performActionInNativeContext(webDriver -> assertEquals(webDriverConfigured, webDriver));
        verifyNoInteractions(webDriverConfigured);
    }

    @Test
    void testPerformActionInNativeContext()
    {
        MobileDriver<?> mobileDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        Set<String> contexts = new HashSet<>();
        contexts.add(WEBVIEW_CONTEXT);
        contexts.add(NATIVE_APP_CONTEXT);
        mockMobileDriverContext(mobileDriver, WEBVIEW_CONTEXT, contexts);
        DriverManager spy = spyIsMobile(true);
        spy.performActionInNativeContext(webDriver -> assertEquals(mobileDriver, webDriver));
        verify(mobileDriver).context(NATIVE_APP_CONTEXT);
        verify(mobileDriver).context(WEBVIEW_CONTEXT);
    }

    @Test
    void testPerformActionInNativeContextException()
    {
        MobileDriver<?> mobileDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        mockMobileDriverContext(mobileDriver, WEBVIEW_CONTEXT, new HashSet<>());
        DriverManager spy = spyIsMobile(true);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> spy.performActionInNativeContext(webDriver -> assertEquals(mobileDriver, webDriver)));
        assertEquals("MobileDriver doesn't have context: " + NATIVE_APP_CONTEXT, exception.getMessage());
    }

    @Test
    void testPerformActionInNativeContextSwitchNotNeeded()
    {
        when(webDriverProvider.getUnwrapped(MobileDriver.class)).thenReturn(mobileDriver);
        when(mobileDriver.getContext()).thenReturn(NATIVE_APP_CONTEXT);
        DriverManager spy = spyIsMobile(true);
        spy.performActionInNativeContext(webDriver -> assertEquals(mobileDriver, webDriver));
        verify(mobileDriver, never()).context(NATIVE_APP_CONTEXT);
        verify(mobileDriver, never()).getContextHandles();
    }

    @Test
    void testIsMobileAnyDevice()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        assertTrue(driverManager.isMobile());
    }

    @Test
    void testIsIOSDeviceWhenPlatformNameIsString()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        assertTrue(driverManager.isIOS());
    }

    @Test
    void testIsIOSDeviceWhenPlatformNameIsEnum()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, Platform.IOS);
        assertTrue(driverManager.isIOS());
    }

    @Test
    void testIsNotIOSDeviceWhenPlatformNameIsEnum()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, Platform.ANDROID);
        assertFalse(driverManager.isIOS());
    }

    @Test
    void testIsIpadIOSDevice()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, Platform.MAC.toString());
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, BrowserType.IPAD);
        assertTrue(driverManager.isIOS());
    }

    @Test
    void testIsIphoneIOSDevice()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, Platform.MAC.toString());
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, BrowserType.IPHONE);
        assertTrue(driverManager.isIOS());
    }

    @Test
    void testIsIOSDeviceFalse()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);
        assertFalse(driverManager.isIOS());
    }

    @Test
    void testIsIOSDeviceFromInnerCapabilities()
    {
        Map<String, String> map = new HashMap<>();
        map.put(SauceLabsCapabilityType.DEVICE, BrowserType.IPHONE);
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, SauceLabsCapabilityType.CAPABILITIES, map);
        assertTrue(driverManager.isIOS());
    }

    @Test
    void testIsIOSUsingDeviceType()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, SauceLabsCapabilityType.DEVICE, BrowserType.IPHONE);
        assertTrue(driverManager.isIOS());
    }

    @Test
    void testIsAndroidByPlatformName()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);
        assertTrue(driverManager.isAndroid());
    }

    @Test
    void testIsMobileAndroidPlatform()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);
        assertTrue(driverManager.isMobile());
    }

    @Test
    void testIsMobileAndroidBrowser()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, BrowserType.ANDROID);
        assertTrue(driverManager.isMobile());
    }

    @Test
    void testIsMobileAndroidCapabilitiesBrowser()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        Capabilities androidCapabilities = mock(Capabilities.class);
        when(havingCapabilitiesWebDriver.getCapabilities()).thenReturn(androidCapabilities);
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, BrowserType.ANDROID);
        assertTrue(driverManager.isAndroid());
    }

    @Test
    void testIsMobileAndroidCapabilitiesMobilePlatform()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        Capabilities androidCapabilities = mock(Capabilities.class);
        when(havingCapabilitiesWebDriver.getCapabilities()).thenReturn(androidCapabilities);
        lenient().when(androidCapabilities.getCapability(DESIREDCAPABILITIES_KEY)).thenReturn(Collections.emptyMap());
        lenient().when(androidCapabilities.getCapability(CapabilityType.PLATFORM_NAME))
                .thenReturn(MobilePlatform.ANDROID);
        assertTrue(driverManager.isAndroid());
    }

    @Test
    void testIsMobileAndroidDesiredCapabilitiesMobilePlatform()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        Capabilities androidCapabilities = mock(Capabilities.class);
        when(havingCapabilitiesWebDriver.getCapabilities()).thenReturn(androidCapabilities);
        lenient().when(androidCapabilities.getCapability(DESIREDCAPABILITIES_KEY))
                .thenReturn(Collections.singletonMap(CapabilityType.PLATFORM_NAME, "android"));
        lenient().when(androidCapabilities.getCapability(CapabilityType.PLATFORM_NAME))
                .thenReturn(MobilePlatform.ANDROID);
        assertTrue(driverManager.isAndroid());
    }

    @Test
    void testIsMobileAndroidIncorrectDesiredCapabilitiesMobilePlatform()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        Capabilities androidCapabilities = mock(Capabilities.class);
        when(havingCapabilitiesWebDriver.getCapabilities()).thenReturn(androidCapabilities);
        lenient().when(androidCapabilities.getCapability(DESIREDCAPABILITIES_KEY)).thenReturn("DesiredAndroid");
        lenient().when(androidCapabilities.getCapability(CapabilityType.PLATFORM_NAME))
                .thenReturn(MobilePlatform.ANDROID);
        assertTrue(driverManager.isAndroid());
    }

    @Test
    void testIsAndroidFalse()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, BrowserType.CHROME);
        assertFalse(driverManager.isAndroid());
    }

    @Test
    void testIsMobileFalse()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, SauceLabsCapabilityType.DEVICE_NAME, null);
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, Platform.LINUX.toString());
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, BrowserType.CHROME);
        assertFalse(driverManager.isMobile());
    }

    @Test
    void testIsBrowser()
    {
        String browserName = "chrome";
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, browserName);
        assertTrue(driverManager.isBrowserAnyOf(BrowserType.CHROME));
    }

    @Test
    void testIsBrowserFalse()
    {
        String browserName = "browser";
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, BROWSER_TYPE, browserName);
        assertFalse(driverManager.isBrowserAnyOf(BrowserType.CHROME));
    }

    @Test
    void shouldBeAndroidNativeApp()
    {
        driverManager.setNativeApp(true);
        MobileDriver<?> webDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        setCapabilities((HasCapabilities) webDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);
        assertTrue(driverManager.isAndroidNativeApp());
    }

    @Test
    void shouldNotBeAndroidNativeApp()
    {
        driverManager.setNativeApp(false);
        assertFalse(driverManager.isAndroidNativeApp());
    }

    @Test
    void shouldNotBeAndroidNativeAppAsItIsIOS()
    {
        driverManager.setNativeApp(true);
        MobileDriver<?> webDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        setCapabilities((HasCapabilities) webDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        assertFalse(driverManager.isAndroidNativeApp());
    }

    @Test
    void shouldBeIOSNativeApp()
    {
        driverManager.setNativeApp(true);
        MobileDriver<?> webDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        setCapabilities((HasCapabilities) webDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        assertTrue(driverManager.isIOSNativeApp());
    }

    @Test
    void shouldNotBeIOSNativeApp()
    {
        driverManager.setNativeApp(false);
        assertFalse(driverManager.isIOSNativeApp());
    }

    @Test
    void shouldNotBeIOSNativeAppAsItIsAndroid()
    {
        driverManager.setNativeApp(true);
        MobileDriver<?> webDriver = mock(MobileDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(webDriverProvider.get()).thenReturn(webDriver);
        setCapabilities((HasCapabilities) webDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);
        assertFalse(driverManager.isIOSNativeApp());
    }

    @ParameterizedTest
    @EnumSource(ScreenOrientation.class)
    void shouldNotDetectOrientationForDesktop(ScreenOrientation orientation)
    {
        when(webDriverProvider.get()).thenReturn(mobileDriver);
        setCapabilities((HasCapabilities) mobileDriver, CapabilityType.PLATFORM_NAME, Platform.WIN10.toString());
        assertFalse(driverManager.isOrientation(orientation));
    }

    static Stream<Arguments> orientationProvider()
    {
        return Stream.of(
            Arguments.of(ScreenOrientation.LANDSCAPE, ScreenOrientation.PORTRAIT,  MobilePlatform.ANDROID),
            Arguments.of(ScreenOrientation.PORTRAIT,  ScreenOrientation.PORTRAIT,  MobilePlatform.ANDROID),
            Arguments.of(ScreenOrientation.LANDSCAPE, ScreenOrientation.PORTRAIT,  MobilePlatform.IOS),
            Arguments.of(ScreenOrientation.PORTRAIT,  ScreenOrientation.PORTRAIT,  MobilePlatform.IOS),
            Arguments.of(ScreenOrientation.LANDSCAPE, ScreenOrientation.LANDSCAPE, MobilePlatform.ANDROID),
            Arguments.of(ScreenOrientation.PORTRAIT,  ScreenOrientation.LANDSCAPE, MobilePlatform.ANDROID),
            Arguments.of(ScreenOrientation.LANDSCAPE, ScreenOrientation.LANDSCAPE, MobilePlatform.IOS),
            Arguments.of(ScreenOrientation.PORTRAIT,  ScreenOrientation.LANDSCAPE, MobilePlatform.IOS)
        );
    }

    @ParameterizedTest
    @MethodSource("orientationProvider")
    void testIsOrientation(ScreenOrientation actualOrientation, ScreenOrientation orientationToCheck, String platform)
    {
        when(webDriverProvider.get()).thenReturn(mobileDriver);
        when(webDriverProvider.getUnwrapped(Rotatable.class)).thenReturn(mobileDriver);
        when(webDriverManagerContext.getParameter(WebDriverManagerParameter.ORIENTATION)).thenReturn(null);
        when(mobileDriver.getOrientation()).thenReturn(actualOrientation);
        setCapabilities((HasCapabilities) mobileDriver, CapabilityType.PLATFORM_NAME, platform);
        assertEquals(actualOrientation == orientationToCheck, driverManager.isOrientation(orientationToCheck));
        verify(webDriverManagerContext).putParameter(WebDriverManagerParameter.ORIENTATION, actualOrientation);
    }

    @Test
    void testOrientationCached()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        when(webDriverManagerContext.getParameter(WebDriverManagerParameter.ORIENTATION))
                .thenReturn(ScreenOrientation.PORTRAIT);
        assertTrue(driverManager.isOrientation(ScreenOrientation.PORTRAIT));
        verifyNoInteractions(mobileDriver);
    }

    static Stream<Arguments> nativeApplicationViewportProvider()
    {
        return Stream.of(
            Arguments.of(ScreenOrientation.LANDSCAPE, new Dimension(375, 667), new Dimension(667, 375)),
            Arguments.of(ScreenOrientation.PORTRAIT,  new Dimension(375, 667), new Dimension(375, 667))
        );
    }

    @ParameterizedTest
    @MethodSource("nativeApplicationViewportProvider")
    void testGetScreenSizeForPortraitOrientation(ScreenOrientation orientation, Dimension dimension,
            Dimension viewport)
    {
        lenient().when(webDriverProvider.getUnwrapped(MobileDriver.class)).thenReturn(mobileDriver);
        lenient().when(webDriverProvider.getUnwrapped(Rotatable.class)).thenReturn(mobileDriver);
        when(mobileDriver.getOrientation()).thenReturn(orientation);
        when(mobileDriver.getContext()).thenReturn(NATIVE_APP_CONTEXT);
        DriverManager spy = spyIsMobile(true);
        Options options = mock(Options.class);
        when(mobileDriver.manage()).thenReturn(options);
        Window window = mock(Window.class);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(dimension);
        assertEquals(viewport, spy.getSize());
        verify(webDriverManagerContext).putParameter(WebDriverManagerParameter.SCREEN_SIZE, dimension);
    }

    @Test
    void testGetNativeApplicationViewportCached()
    {
        when(webDriverProvider.get()).thenReturn(mobileDriver);
        when(webDriverProvider.getUnwrapped(Rotatable.class)).thenReturn(mobileDriver);
        lenient().when(webDriverManagerContext.getParameter(WebDriverManagerParameter.ORIENTATION)).thenReturn(null);
        when(mobileDriver.getOrientation()).thenReturn(ScreenOrientation.PORTRAIT);
        setCapabilities((HasCapabilities) mobileDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        Dimension dimension = new Dimension(375, 667);
        lenient().when(webDriverManagerContext.getParameter(WebDriverManagerParameter.SCREEN_SIZE))
                .thenReturn(dimension);
        Dimension actualDimension = driverManager.getSize();
        assertEquals(dimension.getHeight(), actualDimension.getHeight());
        assertEquals(dimension.getWidth(), actualDimension.getWidth());
        verify(mobileDriver, never()).manage();
    }

    @Test
    void testGetWindowHandlesNotIOS()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);
        when(((WebDriver) havingCapabilitiesWebDriver).getWindowHandles()).thenReturn(WINDOW_HANDLES);
        assertEquals(WINDOW_HANDLES, driverManager.getWindowHandles());
    }

    @Test
    void testGetWindowHandlesIOS()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        when(((WebDriver) havingCapabilitiesWebDriver).getWindowHandles()).thenReturn(WINDOW_HANDLES);
        assertEquals(WINDOW_HANDLES, driverManager.getWindowHandles());
    }

    @Test
    void testGetWindowHandlesUnacceptableException()
    {
        String exceptionMessage = "Exception message";
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        when(((WebDriver) havingCapabilitiesWebDriver).getWindowHandles())
                .thenThrow(new WebDriverException(exceptionMessage));
        WebDriverException exception = assertThrows(WebDriverException.class,
            () -> driverManager.getWindowHandles());
        assertThat(exception.getMessage(), containsString(exceptionMessage));
    }

    @Test
    void testGetWindowHandlesLargeNumberOfAttempts()
    {
        HasCapabilities havingCapabilitiesWebDriver = mockWebDriverWithCapabilities();
        setCapabilities(havingCapabilitiesWebDriver, CapabilityType.PLATFORM_NAME, MobilePlatform.IOS);
        when(((WebDriver) havingCapabilitiesWebDriver).getWindowHandles())
                .thenThrow(new WebDriverException(COULD_NOT_CONNECT_TO_APP));
        WebDriverException exception = assertThrows(WebDriverException.class,
            () -> driverManager.getWindowHandles());
        assertThat(exception.getMessage(), containsString(COULD_NOT_CONNECT_TO_APP));
    }
}
