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
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        // Mock customer object
        customer = new Customer(1L, "John", "Doe", "john.doe@gmail.com");

        // Mock the account repository behavior for the save method
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account accountToSave = invocation.getArgument(0);
            if (accountToSave != null) {
                accountToSave.setAccountId(101L); // Simulate database-generated ID
            }
            return accountToSave;
        });

        // Mock findById behavior if used in tests
        when(accountRepository.findById(101L)).thenReturn(Optional.ofNullable(currentAccount));
    }


    @Test
    void testCreateCurrentAccount() {
        // Call the method to create a current account
        Account account = accountService.createCurrentAccount(customer);

        // Assertions
        assertNotNull(account); // Check that account is not null
        assertNotNull(account.getAccountId()); // Ensure accountId is set
        assertEquals(Account.AccountType.CURRENT, account.getAccountType());
        assertEquals(BigDecimal.ZERO, account.getBalance()); // Verify starting balance
        assertEquals(customer, account.getCustomer()); // Verify customer association

        // Verify save method is called
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testCreateSavingsAccount() {
        // Mock repository behavior for saving an account
        when(accountRepository.save(any(Account.class))).thenReturn(savingsAccount);

        // Call the method to create a savings account
        Account account = accountService.createSavingsAccount(customer);

        // Assertions
        assertNotNull(account);
        assertEquals(Account.AccountType.SAVINGS, account.getAccountType());
        assertEquals(BigDecimal.valueOf(500), account.getBalance()); // Verify the joining bonus
        verify(accountRepository, times(1)).save(any(Account.class)); // Verify save method is called
    }


    @Test
    void testGetAccountById() throws AccountNotFoundException {
        // Call the method to get account by ID
        Account fetchedAccount = accountService.getAccountById(1L);

        // Assertions
        assertNotNull(fetchedAccount);
        assertEquals(1L, fetchedAccount.getAccountId());
        assertEquals(customer, fetchedAccount.getCustomer());
    }

    @Test
    void testGetAccountByIdNotFound() {
        // Mock repository behavior to return empty for a non-existing account
        when(accountRepository.findById(3L)).thenReturn(Optional.empty());

        // Assertions
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(3L));
    }
}
