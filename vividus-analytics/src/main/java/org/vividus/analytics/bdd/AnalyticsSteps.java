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

package org.vividus.analytics.bdd;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;

import org.jbehave.core.annotations.AfterStories;
import org.jbehave.core.annotations.BeforeStories;
import org.vividus.analytics.AnalyticsStoryReporter;
import org.vividus.analytics.model.AnalyticsEvent;
import org.vividus.analytics.model.CustomFields;
import org.vividus.reporter.environment.EnvironmentConfigurer;
import org.vividus.reporter.environment.PropertyCategory;

public class AnalyticsSteps
{
    private static final String SESSION_CONTROL = "sc";

    private Stopwatch stopwatch;

    private final EventBus eventBus;
    private final AnalyticsStoryReporter analyticsStoryReporter;

    public AnalyticsSteps(EventBus eventBus, AnalyticsStoryReporter analyticsStoryReporter)
    {
        this.eventBus = eventBus;
        this.analyticsStoryReporter = analyticsStoryReporter;
    }

    @BeforeStories
    public void beforeStories()
    {
        stopwatch = Stopwatch.createStarted();
        Map<String, String> properties = new HashMap<>();
        Map<String, String> configuration = getEnvironmentProperties(PropertyCategory.CONFIGURATION);
        Map<String, String> modules = getEnvironmentProperties(PropertyCategory.VIVIDUS);

        CustomFields.PROFILE.add(properties, configuration.get("Profiles"));
        CustomFields.JAVA.add(properties, Runtime.version().toString());
        CustomFields.VIVIDUS.add(properties, modules.getOrDefault("vividus", "0.0.0-SNAPSHOT"));
        CustomFields.REMOTE.add(properties, getEnvironmentProperties(PropertyCategory.PROFILE).get("Remote Execution"));
        properties.put(SESSION_CONTROL, "start");
        eventBus.post(new AnalyticsEvent("startTests", properties));

        postPluginsAnalytic(modules);
    }

    private void postPluginsAnalytic(Map<String, String> modules)
    {
        modules.forEach((k, v) -> {
            if (k.startsWith("vividus-plugin-"))
            {
                Map<String, String> payload = new HashMap<>();
                CustomFields.PLUGIN_VERSION.add(payload, v);
                eventBus.post(new AnalyticsEvent(k, "use", payload));
            }
        });
    }

    @AfterStories
    public void afterStories()
    {
        long duration = stopwatch.elapsed().toSeconds();
        Map<String, String> payload = new HashMap<>();
        CustomFields.STORIES.add(payload, stringify(analyticsStoryReporter.getStoriesCount()));
        CustomFields.SCENARIOS.add(payload, stringify(analyticsStoryReporter.getScenariosCount()));
        CustomFields.STEPS.add(payload, stringify(analyticsStoryReporter.getStepsCount()));
        CustomFields.DURATION.add(payload, stringify(duration));
        payload.put(SESSION_CONTROL, "end");
        AnalyticsEvent testFinishEvent = new AnalyticsEvent("finishTests", payload);
        eventBus.post(testFinishEvent);
    }

    private String stringify(long toConvert)
    {
        return Long.toString(toConvert);
    }

    private Map<String, String> getEnvironmentProperties(PropertyCategory propertyCategory)
    {
        return EnvironmentConfigurer.ENVIRONMENT_CONFIGURATION.get(propertyCategory);
    }
}
