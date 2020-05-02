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

package org.vividus.bdd.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Named;

import org.jbehave.core.model.ExamplesTable.ExamplesTableProperties;
import org.jbehave.core.model.TableParsers;
import org.jbehave.core.steps.ParameterControls;
import org.vividus.bdd.util.ExamplesTableProcessor;

@Named("RESOLVING_SELF_REFERENCES_EAGERLY")
public class ResolvingSelfReferencesEagerlyTransformer implements ExtendedTableTransformer
{
    private final ParameterControls parameterControls;
    private final Pattern placeholderPattern;

    public ResolvingSelfReferencesEagerlyTransformer(ParameterControls parameterControls)
    {
        this.parameterControls = parameterControls;
        placeholderPattern = Pattern.compile(addDelimiters("(.*?)"));
    }

    @Override
    public String transform(String tableAsString, TableParsers tableParsers, ExamplesTableProperties properties)
    {
        List<String> tableAsRows = ExamplesTableProcessor.parseRows(tableAsString);
        List<String> header = tableParsers.parseRow(tableAsRows.get(0), true, properties);
        List<List<String>> inputRows = ExamplesTableProcessor.parseDataRows(tableAsRows, tableParsers, properties);
        SelfReferencesResolver resolver = new SelfReferencesResolver(header);
        List<List<String>> resolvedRows = inputRows.stream().map(resolver::resolveRow).collect(Collectors.toList());
        return ExamplesTableProcessor.buildExamplesTable(header, resolvedRows, properties, true);
    }

    private String addDelimiters(String s)
    {
        return parameterControls.nameDelimiterLeft() + s + parameterControls.nameDelimiterRight();
    }

    private final class SelfReferencesResolver
    {
        private Map<String, String> unresolvedRow;
        private Map<String, String> resolvedRow;
        private final List<String> header;

        private SelfReferencesResolver(List<String> header)
        {
            this.header = header;
        }

        private List<String> resolveRow(List<String> row)
        {
            int range = Integer.min(row.size(), header.size());
            resolvedRow = new HashMap<>(range, 1);
            unresolvedRow = IntStream.range(0, range).boxed().collect(Collectors.toMap(header::get, row::get));
            List<String> result = new ArrayList<>();
            for (int i = 0; i < range; i++)
            {
                String key = header.get(i);
                resolveValue(key);
                result.add(resolvedRow.get(key));
            }
            return result;
        }

        private void resolveValue(String key)
        {
            if (notResolved(key))
            {
                String valueToResolve = unresolvedRow.get(key);
                if (doNotResolve(valueToResolve))
                {
                    putPlaceholderWithoutResolving(key);
                }
                else
                {
                    putResolvedPlaceholder(key, valueToResolve);
                    resolveValue(key, valueToResolve);
                }
            }
        }

        private void resolveValue(String key, String valueToResolve)
        {
            String result = valueToResolve;
            Matcher matcher = placeholderPattern.matcher(result);
            while (matcher.find())
            {
                String placeholder = matcher.group(1);
                if (!placeholder.equals(key))
                {
                    resolveValue(placeholder);
                    result = parameterControls
                            .replaceAllDelimitedNames(result, placeholder, resolvedRow.get(placeholder));
                }
            }
            resolvedRow.put(key, result);
        }

        private boolean doNotResolve(String valueToResolve)
        {
            return valueToResolve == null;
        }

        private void putPlaceholderWithoutResolving(String key)
        {
            putResolvedPlaceholder(key, addDelimiters(key));
        }

        private void putResolvedPlaceholder(String key, String value)
        {
            resolvedRow.put(key, value);
        }

        private boolean notResolved(String key)
        {
            return !resolvedRow.containsKey(key);
        }
    }
}
