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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.manager.IWebDriverManagerContext;
import org.vividus.selenium.manager.WebDriverManagerParameter;
import org.vividus.selenium.model.DesiredCapability;
import org.vividus.util.json.JsonUtils;

import io.appium.java_client.HasSessionDetails;

@ExtendWith({ MockitoExtension.class, TestLoggerFactoryExtension.class })
class ApplicationStepsTests
{
    @Mock
    private JsonUtils jsonUtils;

    @Mock
    private IWebDriverProvider webDriverProvider;

    @Mock
    private IWebDriverManagerContext webDriverManagerContext;

    @InjectMocks
    private ApplicationSteps applicationSteps;

    @Test
    void testRunApplicationAtLocation()
    {
        String location = "location";
        String capabilityName = "capabilityName";
        String value = "value";
        String app = "app";
        DesiredCapability capability = new DesiredCapability();
        capability.setCapabilityName(capabilityName);
        capability.setValue(value);
        DesiredCapabilities capabilities = mock(DesiredCapabilities.class);
        when(webDriverManagerContext.getParameter(WebDriverManagerParameter.DESIRED_CAPABILITIES))
                .thenReturn(capabilities);
        HasSessionDetails hasSessionDetails = mock(HasSessionDetails.class);

        when(webDriverProvider.getUnwrapped(HasSessionDetails.class)).thenReturn(hasSessionDetails);

        applicationSteps.runApplicationAtLocation(location, List.of(capability));

        verify(capabilities).setCapability(app, location);
        verify(capabilities).setCapability(capabilityName, value);
        verifyNoMoreInteractions(capabilities);
    }
}
