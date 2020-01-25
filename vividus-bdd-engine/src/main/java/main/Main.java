package main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main
{
    public static void main(String[] args)
    {
        Map<String, Object> container = new HashMap<>();
                
        putByPath(container, "animal.cat.human.reply-1", "Good morning");
        putByPath(container, "animal.cat.human.reply-2", "Good night");
        putByPath(container, "animal.dog.nickname", "Bobby");
        putByPath(container, "key.value", "Pass12!");
        putByPath(container, "zero", "works!");

        System.out.println(container);
    }

    @SuppressWarnings("unchecked")
    private static void putByPath(Map<String, Object> container, String path, String value)
    {
        String[] paths = path.split("\\.");
        int limit = paths.length - 1;
        String key = paths[limit];
        Map<String, Object> target = Arrays.stream(paths).limit(limit).reduce(container, (map, pathKey) -> {
            Map<String, Object> nested = (Map<String, Object>) map.computeIfAbsent(pathKey, k -> new HashMap<>());
            map.put(pathKey, nested);
            return nested;
        }, (l, r) -> l);
        target.put(key, value);
    }
}
