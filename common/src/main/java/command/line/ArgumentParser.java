package command.line;

import exception.ArgumentsNotFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArgumentParser {

    /**
     * Converts the given arguments into a Map. The values have to be delimited by equals and
     *  the strings will be converted to lower case.
     *
     * @param args
     * @return
     * @throws ArgumentsNotFoundException
     */
    public static Map<String, String> convertCommandLineArgs(String[] args) throws ArgumentsNotFoundException {
        return Arrays.stream(args)
                .map(s -> s.split("=")) // [0] key, [1] String
                .map(strings -> {
                    Map<String, String> map = new HashMap<>();

                    map.put(strings[0].toLowerCase(), strings[1].toLowerCase());

                    return map;
                })
                .reduce((map, map2) -> {
                    map.putAll(map2);

                    return map;
                })
                .orElseThrow(ArgumentsNotFoundException::new);
    }
}
