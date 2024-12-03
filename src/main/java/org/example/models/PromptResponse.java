package org.example.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponse {
    private List<Choice> choices;
    private long created;
    private String model;
    private String object;
    private Usage usage;
}
