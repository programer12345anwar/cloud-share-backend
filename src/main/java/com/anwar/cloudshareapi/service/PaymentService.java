package com.anwar.cloudshareapi.service;

import com.anwar.cloudshareapi.document.PaymentTransaction;
import com.anwar.cloudshareapi.document.ProfileDocument;
import com.anwar.cloudshareapi.dto.PaymentDTO;
import com.anwar.cloudshareapi.dto.PaymentVerificationDTO;
import com.anwar.cloudshareapi.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public PaymentDTO createOrder(PaymentDTO paymentDTO) {
        try {
            // System.out.println("🔑 Razorpay Key ID = " + razorpayKeyId);
            // System.out.println("🔑 Razorpay Secret = " + razorpayKeySecret);
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paymentDTO.getAmount());
            orderRequest.put("currency", paymentDTO.getCurrency());
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");
            // create pending transaction record
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(paymentDTO.getAmount())
                    .currency(paymentDTO.getCurrency())
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName() + " " + currentProfile.getLastName())
                    .build();

            paymentTransactionRepository.save(transaction);

            return PaymentDTO.builder()
                    .orderId(orderId)
                    .success(true)
                    .message("Order created successfully")
                    .build();

        } catch (Exception e) {
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order: " + e.getMessage())
                    .build();
        }
    }

    public PaymentDTO verifyPayment(PaymentVerificationDTO request) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            String data = request.getRazorpay_order_id() + "|" + request.getRazorpay_payment_id();
            String generatedSignature = generateHmaSha256Signature(data, razorpayKeySecret);

            if (!generatedSignature.equals(request.getRazorpay_signature())) {
                updateTransactionStatus(request.getRazorpay_order_id(), "FAILED", request.getRazorpay_payment_id(),
                        null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Payment signature verification failed")
                        .build();
            }

            // Add credits based on plan
            int creditsToAdd = 0;
            String plan = "BASIC";

            switch (request.getPlanId()) {
                case "premium":
                    creditsToAdd = 500;
                    plan = "PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    plan = "ULTIMATE";
                    break;
            }
            if (creditsToAdd > 0) {
                userCreditsService.addCredits(clerkId, creditsToAdd, plan);
                updateTransactionStatus(request.getRazorpay_order_id(), "SUCCESS", request.getRazorpay_payment_id(),
                        creditsToAdd);
                return PaymentDTO.builder()
                        .success(true)
                        .message("Payment verified and credits added successfully")
                        .credits(userCreditsService.getUserCredits(clerkId).getCredits())
                        .build();
            } else {
                updateTransactionStatus(request.getRazorpay_order_id(), "FAILED", request.getRazorpay_payment_id(),
                        null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Invalid plan selected")
                        .build();
            }
        } catch (Exception e) {
            try {
                updateTransactionStatus(request.getRazorpay_order_id(), "ERROR", request.getRazorpay_payment_id(),
                        null);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error verifying payment: " + e.getMessage())
                    .build();
        }
    }

    private String generateHmaSha256Signature(String data, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");

        sha256_HMAC.init(secret_key);

        byte[] hash = sha256_HMAC.doFinal(data.getBytes());

        // Convert to HEX string (Razorpay format)
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private void updateTransactionStatus(String razorpayOrderId, String status, String razorpayPaymentId,
            Integer creditsToAdd) {
        paymentTransactionRepository.findAll().stream()
                .filter(t -> t.getOrderId() != null && t.getOrderId().equals(razorpayOrderId))
                .findFirst()
                .map(transaction -> {
                    transaction.setStatus(status);
                    transaction.setPaymentId(razorpayPaymentId);
                    if (creditsToAdd != null) {
                        transaction.setCreditsAdded(creditsToAdd);
                    }
                    return paymentTransactionRepository.save(transaction);
                })
                .orElse(null);
    }

    /**
     * Webhook handler: Called when Razorpay confirms payment.authorized event
     * Automatically updates transaction and adds credits
     */
    public void handleWebhookPaymentAuthorized(String orderId, String paymentId) {
        try {
            // Find transaction by order ID
            PaymentTransaction transaction = paymentTransactionRepository.findAll().stream()
                    .filter(t -> t.getOrderId() != null && t.getOrderId().equals(orderId))
                    .findFirst()
                    .orElse(null);

            if (transaction == null) {
                System.err.println("⚠️  Transaction not found for order: " + orderId);
                return;
            }

            // Prevent duplicate processing (idempotency check)
            if (transaction.getStatus() != null &&
                    (transaction.getStatus().equals("SUCCESS") || transaction.getStatus().equals("PROCESSING"))) {
                System.out.println("⏭️  Transaction already processed for order: " + orderId);
                return;
            }

            // Mark as processing to prevent race conditions
            transaction.setStatus("PROCESSING");
            transaction.setPaymentId(paymentId);
            paymentTransactionRepository.save(transaction);

            // Add credits based on plan
            int creditsToAdd = 0;
            String plan = "BASIC";

            switch (transaction.getPlanId()) {
                case "premium":
                    creditsToAdd = 500;
                    plan = "PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    plan = "ULTIMATE";
                    break;
            }

            if (creditsToAdd > 0) {
                // Add credits to user
                userCreditsService.addCredits(transaction.getClerkId(), creditsToAdd, plan);

                // Update transaction as SUCCESS
                transaction.setStatus("SUCCESS");
                transaction.setCreditsAdded(creditsToAdd);
                paymentTransactionRepository.save(transaction);

                System.out.println("✅ Payment processed via webhook - Order: " + orderId +
                        ", Credits Added: " + creditsToAdd + ", User: " + transaction.getClerkId());
            } else {
                transaction.setStatus("FAILED");
                transaction.setPaymentId(paymentId);
                paymentTransactionRepository.save(transaction);
                System.err.println("❌ Invalid plan ID for order: " + orderId);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing webhook payment: " + e.getMessage());
            // Mark transaction as ERROR but don't throw to prevent webhook retry loops
            try {
                updateTransactionStatus(orderId, "ERROR", paymentId, null);
            } catch (Exception ex) {
                System.err.println("Failed to mark transaction as ERROR: " + ex.getMessage());
            }
        }
    }

    /**
     * Webhook handler: Called when Razorpay confirms payment.failed event
     * Updates transaction status to FAILED
     */
    public void handleWebhookPaymentFailed(String orderId, String paymentId) {
        try {
            // Find transaction by order ID
            PaymentTransaction transaction = paymentTransactionRepository.findAll().stream()
                    .filter(t -> t.getOrderId() != null && t.getOrderId().equals(orderId))
                    .findFirst()
                    .orElse(null);

            if (transaction == null) {
                System.err.println("⚠️  Transaction not found for order: " + orderId);
                return;
            }

            // Prevent duplicate processing
            if (transaction.getStatus() != null &&
                    (transaction.getStatus().equals("FAILED") || transaction.getStatus().equals("SUCCESS"))) {
                System.out.println("⏭️  Transaction already updated for order: " + orderId);
                return;
            }

            // Update transaction as FAILED
            transaction.setStatus("FAILED");
            transaction.setPaymentId(paymentId);
            paymentTransactionRepository.save(transaction);

            System.out.println("❌ Payment failed via webhook - Order: " + orderId +
                    ", Payment: " + paymentId + ", User: " + transaction.getClerkId());

        } catch (Exception e) {
            System.err.println("❌ Error processing webhook payment failure: " + e.getMessage());
        }
    }
}
