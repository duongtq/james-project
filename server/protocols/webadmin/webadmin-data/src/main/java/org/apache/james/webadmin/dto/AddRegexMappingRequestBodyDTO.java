package org.apache.james.webadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddRegexMappingRequestBodyDTO {

    private final String source;
    private final String regex;

    @JsonCreator
    public AddRegexMappingRequestBodyDTO(@JsonProperty("source") String source,
                                         @JsonProperty("regex") String regex) {
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
