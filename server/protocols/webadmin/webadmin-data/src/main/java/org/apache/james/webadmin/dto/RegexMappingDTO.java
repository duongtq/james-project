package org.apache.james.webadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class RegexMappingDTO {
    private final String source;
    private final String regex;

    @JsonCreator
    public RegexMappingDTO(@JsonProperty("source") String source,
                           @JsonProperty("regex") String regex) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(regex);
        this.source = source;
        this.regex  = regex;
    }

    public String getSource() {
        return source;
    }

    public String getRegex() {
        return regex;
    }
}
