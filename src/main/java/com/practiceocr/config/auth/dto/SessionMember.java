package com.practiceocr.config.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionMember {

    private String name;
    private String email;
    private String picture;

}
