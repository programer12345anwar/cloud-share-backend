package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.document.PaymentTransaction;
import com.anwar.cloudshareapi.document.ProfileDocument;
import com.anwar.cloudshareapi.repository.PaymentTransactionRepository;
import com.anwar.cloudshareapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getUserTransactions(){
        System.out.println("payment transactions");
        ProfileDocument currentProfile=profileService.getCurrentProfile();
        String clerkId=currentProfile.getClerkId();

        List<PaymentTransaction> transactions=paymentTransactionRepository.findByClerkIdAndStatusOrderByTransactionDateDesc(clerkId,"SUCCESS");
        return ResponseEntity.ok(transactions);
    }
}
