package com.bankX.BankingApplication.service;

import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Customer;
import com.bankX.BankingApplication.BankingApplication;
import com.bankX.BankingApplication.exception.AccountNotFoundException;
import com.bankX.BankingApplication.repository.AccountRepository;
import com.bankX.BankingApplication.repository.TransactionRepository;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = {BankingApplication.class})
@Transactional
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountService accountService;

    private Customer customer;
    private Account currentAccount;
    private Account savingsAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize mock customer
        customer = new Customer(1L, "John", "Doe", "john.doe@gmail.com");

        // Initialize mock accounts without hardcoding IDs
        currentAccount = new Account();
        currentAccount.setCustomer(customer);
        currentAccount.setAccountType(Account.AccountType.CURRENT);
        currentAccount.setBalance(BigDecimal.ZERO);

        savingsAccount = new Account();
        savingsAccount.setCustomer(customer);
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setBalance(BigDecimal.valueOf(500)); // Joining bonus

        // Mock repository save behavior to simulate database auto-generating IDs
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account accountToSave = invocation.getArgument(0);
            if (accountToSave.getAccountId() == null) {
                accountToSave.setAccountId((long) (Math.random() * 1000)); // Simulate auto-generated ID
            }
            return accountToSave;
        });

        // Mock findById behavior for test cases
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(currentAccount));
    }

    @Test
    void testCreateCurrentAccount() {
        // Call the method to create a current account
        Account account = accountService.createCurrentAccount(customer);

        // Assertions
        assertNotNull(account); // Ensure account is not null
        assertNotNull(account.getAccountId()); // Ensure accountId is generated
        assertEquals(Account.AccountType.CURRENT, account.getAccountType());
        assertEquals(BigDecimal.ZERO, account.getBalance()); // Verify starting balance
        assertEquals(customer, account.getCustomer()); // Verify customer association

        // Verify that save method was called
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testCreateSavingsAccount() {
        // Call the method to create a savings account
        Account account = accountService.createSavingsAccount(customer);

        // Assertions
        assertNotNull(account);
        assertNotNull(account.getAccountId()); // Ensure accountId is generated
        assertEquals(Account.AccountType.SAVINGS, account.getAccountType());
        assertEquals(BigDecimal.valueOf(500), account.getBalance()); // Verify the joining bonus
        assertEquals(customer, account.getCustomer());

        // Verify that save method was called
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testGetAccountById() throws AccountNotFoundException {
        // Call the method to get account by ID
        Account fetchedAccount = accountService.getAccountById(1L);

        // Assertions
        assertNotNull(fetchedAccount);
        assertEquals(Account.AccountType.CURRENT, fetchedAccount.getAccountType());
        assertEquals(customer, fetchedAccount.getCustomer());
    }

    @Test
    void testGetAccountByIdNotFound() {
        // Mock repository behavior to return empty for a non-existing account
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // Assertions
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(999L));
    }
}
