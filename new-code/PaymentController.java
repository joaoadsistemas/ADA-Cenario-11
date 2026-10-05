package com.marketplace.payment.controller;

import com.marketplace.payment.dto.PaymentRequestDTO;
import com.marketplace.payment.dto.PaymentResponseDTO;
import com.marketplace.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> pay(@Valid @RequestBody PaymentRequestDTO request) {
        PaymentResponseDTO response = paymentService.processPayment(request);

        if (StatusResponseEnum.APPROVED.equals(response.status())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }
}