package org.example.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RequestPromptModel {
    private String model;
    private List<Message> messages;
    private boolean stream;
    private int updateInterval;
}
