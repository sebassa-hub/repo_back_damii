package com.rutasproyect.damii.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressInfo {
    private String status;
    private int current;
    private int total;
    private String message;
}
