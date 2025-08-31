package com.rivers.core.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Data
@ToString
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -6660462189662856019L;

    private String accountNo;

    private String username;

    private String organizeCode;

    private String organizeName;

    private String type;
}
