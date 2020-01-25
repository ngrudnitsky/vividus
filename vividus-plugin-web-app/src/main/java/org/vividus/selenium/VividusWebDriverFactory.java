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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jbehave.core.model.Scenario;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.openqa.selenium.support.events.WebDriverEventListener;
import org.vividus.bdd.model.MetaWrapper;
import org.vividus.bdd.model.RunningStory;
import org.vividus.selenium.manager.WebDriverManager;

public class VividusWebDriverFactory extends AbstractVividusDriverFactory
{
    @Inject private IWebDriverFactory webDriverFactory;
    @Inject private IBrowserWindowSizeProvider browserWindowSizeProvider;
    private boolean remoteExecution;
    private List<WebDriverEventListener> webDriverEventListeners;

    @Override
    protected void configureVividusWebDriver(VividusWebDriver vividusWebDriver)
    {
        DesiredCapabilities capabilities = vividusWebDriver.getDesiredCapabilities();
        WebDriver webDriver = remoteExecution
                ? webDriverFactory.getRemoteWebDriver(capabilities)
                : webDriverFactory.getWebDriver((String) capabilities.getCapability(CapabilityType.BROWSER_NAME),
                        capabilities);

        EventFiringWebDriver eventFiringWebDriver = new EventFiringWebDriver(webDriver);
        webDriverEventListeners.forEach(eventFiringWebDriver::register);

        WebDriverManager.resize(eventFiringWebDriver,
                browserWindowSizeProvider.getBrowserWindowSize(remoteExecution));
        vividusWebDriver.setWebDriver(eventFiringWebDriver);
        vividusWebDriver.setRemote(remoteExecution);
    }

    @Override
    protected void setDesiredCapabilities(DesiredCapabilities desiredCapabilities, RunningStory runningStory,
            Scenario scenario, MetaWrapper metaWrapper)
    {
        if (remoteExecution)
        {
            desiredCapabilities.setCapability(SauceLabsCapabilityType.NAME, runningStory.getName());
        }
        else
        {
            if (scenario != null)
            {
                ControllingMetaTag.BROWSER_NAME.setCapability(desiredCapabilities, metaWrapper);
            }
        }
    }

    public void setRemoteExecution(boolean remoteExecution)
    {
        this.remoteExecution = remoteExecution;
    }

    public void setWebDriverEventListeners(List<WebDriverEventListener> webDriverEventListeners)
    {
        this.webDriverEventListeners = Collections.unmodifiableList(webDriverEventListeners);
    }
}
