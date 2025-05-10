package com.siemens.internship;

import jakarta.persistence.*;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;


    // added annotations to validate the fields for post requests
    // NotBlank checks if the field is not null and not empty
    // in case it is, the message will be returned to the user

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    // the regex checks that the email starts with an alphanumeric character, but allows +_.- in rest,
    // has an '@', a domain with at least one ".", and ends with a valid top-level domain with at
    // least 2 characters

    @NotBlank(message = "Email cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9+_.-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email must be a valid")
    private String email;
}