package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    /**
     * Razorpay Webhook Handler
     * Handles payment.authorized and payment.failed events
     * Signature: X-Razorpay-Signature header with HMAC-SHA256
     */
    @PostMapping("/razorpay")
    public ResponseEntity<?> handleRazorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String payload) {
        try {
            // Verify webhook signature
            if (!verifyWebhookSignature(payload, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid webhook signature");
            }

            // Parse webhook payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(payload);
            String eventType = rootNode.path("event").asText();

            // Handle webhook event
            switch (eventType) {
                case "payment.authorized":
                    handlePaymentAuthorized(rootNode.path("payload").path("payment").path("entity"));
                    break;
                case "payment.failed":
                    handlePaymentFailed(rootNode.path("payload").path("payment").path("entity"));
                    break;
                case "order.paid":
                    handleOrderPaid(rootNode.path("payload").path("order").path("entity"));
                    break;
                // Add more event types as needed
            }

            // Return 200 OK to acknowledge receipt (Razorpay requirement)
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // Log but return 200 OK to prevent Razorpay retries
            System.err.println("❌ Razorpay Webhook Error: " + e.getMessage());
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Handle payment.authorized event
     * Called when payment is successfully authorized
     */
    private void handlePaymentAuthorized(JsonNode paymentEntity) {
        try {
            String orderId = paymentEntity.path("order_id").asText();
            String paymentId = paymentEntity.path("id").asText();

            System.out.println("✅ Payment Authorized - Order: " + orderId + ", Payment: " + paymentId);

            // Update transaction status via service
            paymentService.handleWebhookPaymentAuthorized(orderId, paymentId);

        } catch (Exception e) {
            System.err.println("Error handling payment.authorized: " + e.getMessage());
        }
    }

    /**
     * Handle payment.failed event
     * Called when payment fails
     */
    private void handlePaymentFailed(JsonNode paymentEntity) {
        try {
            String orderId = paymentEntity.path("order_id").asText("");
            String paymentId = paymentEntity.path("id").asText();

            System.out.println("❌ Payment Failed - Order: " + orderId + ", Payment: " + paymentId);

            // Update transaction status via service
            paymentService.handleWebhookPaymentFailed(orderId, paymentId);

        } catch (Exception e) {
            System.err.println("Error handling payment.failed: " + e.getMessage());
        }
    }

    /**
     * Handle order.paid event
     * Alternative event for order completion (optional)
     */
    private void handleOrderPaid(JsonNode orderEntity) {
        try {
            String orderId = orderEntity.path("id").asText();

            System.out.println("✅ Order Paid - Order: " + orderId);

            // Can be used as additional confirmation

        } catch (Exception e) {
            System.err.println("Error handling order.paid: " + e.getMessage());
        }
    }

    /**
     * Verify Razorpay webhook signature
     * Razorpay uses: HMAC-SHA256(body, secret)
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // Generate signature: HMAC-SHA256(payload, webhook_secret)
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes());

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            String generatedSignature = hexString.toString();

            // Compare signatures
            boolean isValid = generatedSignature.equals(signature);

            if (!isValid) {
                System.err.println("⚠️  Webhook signature mismatch - Expected: " + signature +
                        ", Generated: " + generatedSignature);
            }

            return isValid;

        } catch (Exception e) {
            System.err.println("Error verifying webhook signature: " + e.getMessage());
            return false;
        }
    }
}
