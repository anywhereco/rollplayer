package dev.infernity.rollplayer.util;
import java.util.List;

public class RandomExt {
    public static <T> T weighted_choice(List<T> list, List<? extends Number> weights) {
        double totalWeight = 0.0;
        for (int i = 0; i<list.size(); i++) {
            totalWeight += weights.get(i).doubleValue();
        }
        int idx = 0;
        for (double r = Math.random() * totalWeight; idx < list.size() - 1; ++idx) {
            r -= weights.get(idx).doubleValue();
            if (r <= 0.0) break;
        }
        return list.get(idx);
    }
}
