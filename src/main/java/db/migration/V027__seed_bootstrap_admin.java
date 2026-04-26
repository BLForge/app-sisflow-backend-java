package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Seeds the first admin user from environment variables.
 * Set BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD, BOOTSTRAP_ADMIN_NAME before first boot.
 * Idempotent — skips if the email already exists.
 */
public class V027__seed_bootstrap_admin extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String email = System.getenv("BOOTSTRAP_ADMIN_EMAIL");
        String password = System.getenv("BOOTSTRAP_ADMIN_PASSWORD");
        String name = System.getenv().getOrDefault("BOOTSTRAP_ADMIN_NAME", "Admin");

        if (email == null || email.isBlank() || password == null || password.isBlank()) return;

        // Check if email already exists
        try (PreparedStatement check = context.getConnection()
                .prepareStatement("SELECT id FROM users WHERE email = ?")) {
            check.setString(1, email);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return; // already exists
            }
        }

        // Find highest hierarchy role
        UUID roleId = null;
        try (PreparedStatement rs = context.getConnection()
                .prepareStatement("SELECT id FROM roles WHERE is_active = true ORDER BY hierarchy_level DESC LIMIT 1")) {
            try (ResultSet r = rs.executeQuery()) {
                if (r.next()) roleId = UUID.fromString(r.getString("id"));
            }
        }

        String hash = new BCryptPasswordEncoder().encode(password);
        UUID userId = UUID.randomUUID();

        try (PreparedStatement insert = context.getConnection().prepareStatement(
                "INSERT INTO users (id, email, password_hash, name, role, email_confirmed, created_at) " +
                "VALUES (?, ?, ?, ?, 'admin', false, NOW())")) {
            insert.setObject(1, userId);
            insert.setString(2, email);
            insert.setString(3, hash);
            insert.setString(4, name);
            insert.executeUpdate();
        }

        if (roleId != null) {
            try (PreparedStatement assign = context.getConnection().prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, is_active, assigned_at, created_at) " +
                    "VALUES (?, ?, ?, true, NOW(), NOW())")) {
                assign.setObject(1, UUID.randomUUID());
                assign.setObject(2, userId);
                assign.setObject(3, roleId);
                assign.executeUpdate();
            }
        }
    }
}
