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

package org.vividus.bdd.steps;

import java.util.List;

import org.jbehave.core.annotations.Given;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.SauceLabsCapabilityType;
import org.vividus.selenium.manager.IWebDriverManagerContext;
import org.vividus.selenium.manager.WebDriverManagerParameter;
import org.vividus.selenium.model.DesiredCapability;
import org.vividus.util.json.JsonUtils;

import io.appium.java_client.HasSessionDetails;

public class ApplicationSteps
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationSteps.class);

    private final JsonUtils jsonUtils;
    private final IWebDriverProvider webDriverProvider;
    private final IWebDriverManagerContext webDriverManagerContext;

    public ApplicationSteps(JsonUtils jsonUtils, IWebDriverProvider webDriverProvider,
            IWebDriverManagerContext webDriverManagerContext)
    {
        this.jsonUtils = jsonUtils;
        this.webDriverProvider = webDriverProvider;
        this.webDriverManagerContext = webDriverManagerContext;
    }

    @Given("I run the application located at `$location` and capabilities:$capabilities")
    public void runApplicationAtLocation(String location, List<DesiredCapability> capabilities)
    {
        DesiredCapabilities desiredCapabilities = webDriverManagerContext
                .getParameter(WebDriverManagerParameter.DESIRED_CAPABILITIES);
        desiredCapabilities.setCapability(SauceLabsCapabilityType.APP, location);
        capabilities.forEach(c -> desiredCapabilities.setCapability(c.getCapabilityName(), c.getValue()));
        HasSessionDetails session = webDriverProvider.getUnwrapped(HasSessionDetails.class);
        LOGGER.atInfo()
            .addArgument(location)
            .addArgument(() -> jsonUtils.toPrettyJson(session.getSessionDetails()))
            .log("Start application located at {} with capabilities:\n{}");
    }
}
