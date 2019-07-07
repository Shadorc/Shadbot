import com.shadorc.shadbot.utils.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestStringUtils {

    private enum TestEnum {
        TEST_ONE, test_Two, testThree;
    }

    @Test
    public void test_capitalize() {
        assertEquals("", StringUtils.capitalize(""));
        assertEquals("F", StringUtils.capitalize("f"));
        assertEquals("Fo", StringUtils.capitalize("fo"));
        assertEquals("Foo", StringUtils.capitalize("foo"));
        assertEquals("Foo", StringUtils.capitalize("Foo"));
        assertEquals("Foo", StringUtils.capitalize("foO"));
        assertNull(StringUtils.capitalize(null));
    }

    @Test
    public void test_capitalizeEnum() {
        assertEquals("Test one", StringUtils.capitalizeEnum(TestEnum.TEST_ONE));
        assertEquals("Test two", StringUtils.capitalizeEnum(TestEnum.test_Two));
        assertEquals("Testthree", StringUtils.capitalizeEnum(TestEnum.testThree));
        assertNull(StringUtils.capitalizeEnum(null));
    }

    @Test
    public void test_getQuotedElements() {
        assertEquals(List.of("hello"), StringUtils.getQuotedElements("\"hello\""));
        assertEquals(List.of("hello", "hi"), StringUtils.getQuotedElements("\"hello\" \"hi\""));
        assertEquals(List.of("hello", "hi"), StringUtils.getQuotedElements("\"hello\" foo \"hi\""));
        assertEquals(List.of("hello foo "), StringUtils.getQuotedElements("\"hello foo \"hi\""));
        assertEquals(List.of("hello", "foo"), StringUtils.getQuotedElements("\"hello\" \"\" \"foo\""));
        assertEquals(Collections.emptyList(), StringUtils.getQuotedElements("hello \"\"foo hi"));
        assertEquals(Collections.emptyList(), StringUtils.getQuotedElements(null));
        assertEquals(Collections.emptyList(), StringUtils.getQuotedElements("    "));
    }

    @Test
    public void test_normalizeSpace() {
        assertEquals("hello", StringUtils.normalizeSpace("   hello      "));
        assertEquals("he llo", StringUtils.normalizeSpace("   he   llo      "));
        assertNull(StringUtils.normalizeSpace(null));
    }

    @Test
    public void test_pluralOf() {
        assertEquals("-2 coin", StringUtils.pluralOf(-2, "coin"));
        assertEquals("-1 coin", StringUtils.pluralOf(-1, "coin"));
        assertEquals("0 coin", StringUtils.pluralOf(0, "coin"));
        assertEquals("1 coin", StringUtils.pluralOf(1, "coin"));
        assertEquals("2 coins", StringUtils.pluralOf(2, "coin"));
        assertNull(StringUtils.pluralOf(1, "   "));
        assertNull(StringUtils.pluralOf(2, "   "));
        assertNull(StringUtils.pluralOf(2, null));
    }

    @Test
    public void test_remove_list() {
        assertEquals("bar", StringUtils.remove("foo bar", List.of("foo ")));
        assertEquals(" ", StringUtils.remove("foo bar", List.of("foo", "bar")));
        assertEquals("foo bar", StringUtils.remove("foo bar", Collections.emptyList()));
        assertEquals("foo bar", StringUtils.remove("foo bar", List.of("*")));
        assertEquals("foo bar", StringUtils.remove("foo bar", List.of("")));
        assertNull(StringUtils.remove(null, Collections.emptyList()));
    }

    @Test
    public void test_remove_array() {
        assertEquals("bar", StringUtils.remove("foo bar", "foo "));
        assertEquals(" ", StringUtils.remove("foo bar", "foo", "bar"));
        assertEquals("foo bar", StringUtils.remove("foo bar"));
        assertEquals("foo bar", StringUtils.remove("foo bar", "*"));
        assertEquals("foo bar", StringUtils.remove("foo bar", ""));
        assertNull(StringUtils.remove(null));
    }

    @Test
    public void test_split() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo bar hi"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo bar hi "));
        assertEquals(List.of("foo"), StringUtils.split("foo"));
        assertEquals(Collections.emptyList(), StringUtils.split(""));
        assertEquals(Collections.emptyList(), StringUtils.split(null));
    }

    @Test
    public void test_split_limit() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo bar hi", -1));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo bar hi", 0));
        assertEquals(List.of("foo bar hi"), StringUtils.split("foo bar hi", 1));
        assertEquals(List.of("foo", "bar hi"), StringUtils.split("foo bar hi", 2));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo bar hi", 3));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo bar hi", 4));
        assertEquals(List.of("foo", "bar hi"), StringUtils.split("foo bar hi ", 2));
        assertEquals(List.of("foo"), StringUtils.split("foo", 2));
        assertEquals(Collections.emptyList(), StringUtils.split("", 2));
        assertEquals(Collections.emptyList(), StringUtils.split(null, 2));
    }

    @Test
    public void test_split_limitAndDelimiter() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo-bar-hi", -1, "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo-bar-hi", 0, "-"));
        assertEquals(List.of("foo-bar-hi"), StringUtils.split("foo-bar-hi", 1, "-"));
        assertEquals(List.of("foo", "bar-hi"), StringUtils.split("foo-bar-hi", 2, "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo-bar-hi", 3, "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo-bar-hi", 4, "-"));
        assertEquals(List.of("foo", "bar-hi-"), StringUtils.split("foo-bar-hi-", 2, "-"));
        assertEquals(List.of("foo"), StringUtils.split("foo", 2, "-"));
        assertEquals(Collections.emptyList(), StringUtils.split("", 2, "-"));
        assertEquals(Collections.emptyList(), StringUtils.split(null, 2, "-"));
        assertThrows(NullPointerException.class, () -> StringUtils.split("foo-bar", 2, null));
    }

    @Test
    public void test_split_delimiter() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo-bar-hi", "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtils.split("foo-bar-hi-", "-"));
        assertEquals(List.of("foo"), StringUtils.split("foo", "-"));
        assertEquals(Collections.emptyList(), StringUtils.split("", "-"));
        assertEquals(Collections.emptyList(), StringUtils.split(null, "-"));
        assertThrows(NullPointerException.class, () -> StringUtils.split("foo-bar", null));
    }

}
