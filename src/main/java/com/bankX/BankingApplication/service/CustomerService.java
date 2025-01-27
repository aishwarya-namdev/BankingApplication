package com.bankX.BankingApplication.service;

import org.springframework.stereotype.Service;

import com.bankX.BankingApplication.exception.CustomerAlreadyExistsException;
import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Customer;
import com.bankX.BankingApplication.repository.AccountRepository;
import com.bankX.BankingApplication.repository.CustomerRepository;

import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerService(CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Customer createCustomer(Customer customer) throws CustomerAlreadyExistsException {
        // Check if customer with the same email exists
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with this email already exists.");
        }
        // Save the customer if not exists
        return customerRepository.save(customer);
    }

    public Account createCurrentAccount(Customer customer) {
        Account currentAccount = new Account();
        currentAccount.setCustomer(customer);
        currentAccount.setAccountType(Account.AccountType.CURRENT);
        currentAccount.setBalance(BigDecimal.ZERO);
        return accountRepository.save(currentAccount);
    }

    public Account createSavingsAccount(Customer customer) {
        Account savingsAccount = new Account();
        savingsAccount.setCustomer(customer);
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setBalance(BigDecimal.valueOf(500)); // Joining bonus
        return accountRepository.save(savingsAccount);
    }

    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }
}