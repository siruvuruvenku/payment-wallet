package com.example.wallet.auth;

import com.example.wallet.api.ApiException;
import com.example.wallet.domain.UserAccount;
import com.example.wallet.domain.Wallet;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.security.JwtTokenService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.users = users;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        UserAccount user = users.save(new UserAccount(email, passwordEncoder.encode(request.password())));
        Wallet wallet = wallets.save(new Wallet(user.getId(), "USD"));
        return new AuthDtos.AuthResponse(tokens.issue(user.getId()), user.getId(), wallet.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        var wallet = wallets.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Account wallet is unavailable"));
        return new AuthDtos.AuthResponse(tokens.issue(user.getId()), user.getId(), wallet.getId(), user.getEmail());
    }
}
