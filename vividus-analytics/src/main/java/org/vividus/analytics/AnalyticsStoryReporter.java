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

import java.util.concurrent.atomic.AtomicLong;

import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.vividus.bdd.ChainedStoryReporter;

public class AnalyticsStoryReporter extends ChainedStoryReporter
{
    private static final AtomicLong STORIES = new AtomicLong();
    private static final AtomicLong SCENARIOS = new AtomicLong();
    private static final AtomicLong STEPS = new AtomicLong();

    @Override
    public void beforeStory(Story story, boolean givenStory)
    {
        super.beforeStory(story, givenStory);
        if (isSystem(story.getPath()))
        {
            return;
        }
        STORIES.incrementAndGet();
    }

    @Override
    public void beforeScenario(Scenario scenario)
    {
        super.beforeScenario(scenario);
        SCENARIOS.incrementAndGet();
    }

    @Override
    public void beforeStep(String step)
    {
        super.beforeStep(step);
        STEPS.incrementAndGet();
    }

    public long getStepsCount()
    {
        return STEPS.get();
    }

    public long getScenariosCount()
    {
        return SCENARIOS.get();
    }

    public long getStoriesCount()
    {
        return STORIES.get();
    }

    private static boolean isSystem(String story)
    {
        return story.startsWith("@Before") || story.startsWith("@After");
    }
}
