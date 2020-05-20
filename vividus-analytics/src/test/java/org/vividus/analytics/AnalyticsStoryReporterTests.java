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

package org.vividus.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.junit.jupiter.api.Test;

class AnalyticsStoryReporterTests
{
    private final AnalyticsStoryReporter reporter = new AnalyticsStoryReporter();

    @Test
    void shouldCountStories()
    {
        Stream.concat(Stream.of(storyOf("@BeforeStories"), storyOf("@AfterStories")),
                      Stream.generate(() -> storyOf("random")).limit(9))
              .forEach(s -> reporter.beforeStory(s, false));
        assertEquals(9, reporter.getStoriesCount());
    }

    private Story storyOf(String path)
    {
        return new Story(path);
    }

    @Test
    void shouldCountSteps()
    {
        Stream.generate(() -> 0).limit(10).forEach(v -> reporter.beforeStep(null));
        assertEquals(10, reporter.getStepsCount());
    }

    @Test
    void shouldCountScenarios()
    {
        Stream.generate(() -> 0).limit(20).forEach(v -> reporter.beforeScenario(mock(Scenario.class)));
        assertEquals(20, reporter.getScenariosCount());
    }
}
