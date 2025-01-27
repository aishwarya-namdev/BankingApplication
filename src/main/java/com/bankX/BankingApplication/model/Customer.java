package com.bankX.BankingApplication.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long customerId;

    @Column(name = "first_name")  // Mapping to the snake_case column
    private String firstName;

    @Column(name = "last_name")   // Mapping to the snake_case column
    private String lastName;

    @Column(name = "email", unique = true)
    private String email;
}
