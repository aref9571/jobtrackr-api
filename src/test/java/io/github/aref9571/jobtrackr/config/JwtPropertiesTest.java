package io.github.aref9571.jobtrackr.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String SECRET_32_BYTES = "s".repeat(32);

    @Test
    void constructor_secretWith32BytesAndPositiveExpiration_keepsValues() {
        JwtProperties properties = new JwtProperties(SECRET_32_BYTES, Duration.ofHours(1));

        assertThat(properties.secret()).isEqualTo(SECRET_32_BYTES);
        assertThat(properties.expiration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void constructor_missingSecret_throwsException() {
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jobtrackr.jwt.secret must be at least 32 bytes");
    }

    @Test
    void constructor_secretWith31Bytes_throwsExceptionWithoutShowingSecret() {
        String secret = "s".repeat(31);

        assertThatThrownBy(() -> new JwtProperties(secret, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(secret);
    }

    @Test
    void constructor_multiByteSecret_isMeasuredInUtf8Bytes() {
        String thirtyTwoBytes = "\u00fc".repeat(16);
        String thirtyOneBytesInSixteenCharacters = "\u00fc".repeat(15) + "a";

        assertThat(new JwtProperties(thirtyTwoBytes, Duration.ofHours(1)).secret()).isEqualTo(thirtyTwoBytes);
        assertThatThrownBy(() -> new JwtProperties(thirtyOneBytesInSixteenCharacters, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_zeroNegativeOrMissingExpiration_throwsException() {
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jobtrackr.jwt.expiration must be positive");
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
