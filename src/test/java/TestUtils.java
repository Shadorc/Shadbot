import com.shadorc.shadbot.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestUtils {

    @Test
    public void testRandValueList() {
        assertNull(Utils.randValue(Collections.emptyList()));
        assertEquals(5, Utils.randValue(List.of(5)));
    }

    @Test
    public void testRandValueArray() {
        assertNull(Utils.randValue(new Integer[0]));
        assertEquals(5, Utils.randValue(List.of(5).toArray(new Integer[0])));
    }

    @Test
    public void testSortMap() {
        final Map<String, Integer> expected = Map.of("1", 1, "2", 2, "3", 3);
        final Map<String, Integer> unsorted = Map.of("2", 2, "1", 1, "3", 3);
        final Map<String, Integer> singleton = Map.of("3", 3);
        final Map<String, Integer> empty = new HashMap<>();
        assertEquals(expected, Utils.sortMap(unsorted, Comparator.comparingInt(Map.Entry::getValue)));
        assertEquals(empty, Utils.sortMap(empty, Comparator.comparingInt(Map.Entry::getValue)));
        assertEquals(singleton, Utils.sortMap(singleton, Comparator.comparingInt(Map.Entry::getValue)));
    }

}
