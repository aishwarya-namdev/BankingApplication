package com.bankX.BankingApplication.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Account.AccountType;
import com.bankX.BankingApplication.model.Customer;
import com.bankX.BankingApplication.repository.AccountRepository;
import com.bankX.BankingApplication.repository.CustomerRepository;

@SpringBootTest
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@gmail.com");
    }

    @Test
    void testCreateCustomerWithDatabase() {
        // Create a new customer object
        Customer savedCustomer = customerService.createCustomer(customer);

        // Verify the saved customer is not null
        assertNotNull(savedCustomer);
        assertNotNull(savedCustomer.getCustomerId());

        // Fetch the customer from the database using the repository
        Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());

        // Verify the customer exists in the database
        assertTrue(retrievedCustomer.isPresent());

        // Verify the properties of the retrieved customer
        assertEquals("John", retrievedCustomer.get().getFirstName());
        assertEquals("Doe", retrievedCustomer.get().getLastName());
        assertEquals("john.doe@gmail.com", retrievedCustomer.get().getEmail());

        // Verify that customer has a SAVINGS account associated
        Account savedAccount = accountRepository.findByCustomerAndAccountType(savedCustomer, AccountType.SAVINGS);
        assertNotNull(savedAccount);
        assertEquals(AccountType.SAVINGS, savedAccount.getAccountType());
        assertEquals(savedCustomer, savedAccount.getCustomer());
    }

    @Test
    void testCreateCustomerWithCurrentAccount() {
        // Create a new customer object
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("jane.smith@gmail.com");

        // Create the customer and retrieve their current account
        Customer savedCustomer = customerService.createCustomer(customer);

        // Verify the customer creation
        assertNotNull(savedCustomer);
        assertNotNull(savedCustomer.getCustomerId());

        // Create and verify current account
        Account savedCurrentAccount = accountRepository.findByCustomerAndAccountType(savedCustomer, AccountType.CURRENT);
        assertNotNull(savedCurrentAccount);
        assertEquals(AccountType.CURRENT, savedCurrentAccount.getAccountType());
        assertEquals(savedCustomer, savedCurrentAccount.getCustomer());
    }
}
