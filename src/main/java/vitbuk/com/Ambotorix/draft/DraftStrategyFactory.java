package vitbuk.com.Ambotorix.draft;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DraftStrategyFactory {

    private final Map<String, DraftStrategy> strategies;

    public DraftStrategyFactory(List<DraftStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        DraftStrategy::getName,
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Duplicate draft strategy name: " + a.getName()); }
                ));
    }

    public DraftStrategy getStrategy(String name) {
        DraftStrategy s = strategies.get(name);
        if (s == null) throw new IllegalArgumentException("Unknown draft strategy: " + name);
        return s;
    }

    public Set<String> getStrategyNames() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(strategies.keySet()));
    }
}
