package com.shadorc.shadbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import reactor.util.annotation.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class NetUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    // Source: https://urlregex.com/
    private static final Pattern URL_MATCH = Pattern.compile(
            "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;()]*[-a-zA-Z0-9+&@#/%=~_|])");

    /**
     * @param str The string to check.
     * @return {@code true} if the string is a valid URL, {@code false} otherwise.
     */
    public static boolean isUrl(String str) {
        return URL_MATCH.matcher(str).matches();
    }

    /**
     * @param html The HTML to convert to text with new lines preserved, may be {@code null}.
     * @return The provided HTML converted to text with new lines preserved or {@code null} if null string input.
     */
    @Nullable
    public static String cleanWithLinebreaks(@Nullable String html) {
        if (StringUtil.isBlank(html)) {
            return html;
        }
        final Document document = Jsoup.parse(html);
        // Makes html() preserve linebreak and spacing
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        final String str = document.html().replace("\\\\n", "\n");
        return Jsoup.clean(str, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * @param str The string to encode as UTF-8, may be {@code null}.
     * @return The string encoded as UTF-8 or {@code null} if null string input.
     */
    @Nullable
    public static String encode(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }
}
