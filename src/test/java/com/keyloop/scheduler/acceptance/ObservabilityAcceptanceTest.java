package com.keyloop.scheduler.acceptance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.keyloop.scheduler.booking.BookingService;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.C2;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.V2;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest turns metric export off; this brings /actuator/prometheus back for this class's context.
@AutoConfigureMetrics
class ObservabilityAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";
    private static final String COMPLETED = "booking.completed";
    private static final String NO_TECHNICIAN = "NO_QUALIFIED_TECHNICIAN_AVAILABLE";
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "outcome", "appointmentId", "dealershipId", "serviceTypeCode", "durationMs", "attempts", "replayed");

    private BookingApi api;
    private Logger bookingLogger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        api = new BookingApi(mockMvc);

        bookingLogger = (Logger) LoggerFactory.getLogger(BookingService.class);
        originalLevel = bookingLogger.getLevel();
        bookingLogger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        bookingLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        bookingLogger.detachAppender(appender);
        bookingLogger.setLevel(originalLevel);
        appender.stop();
    }

    // ---- AC-E02.4 ----

    @Test
    @DisplayName("AC-E02.4: Exactly one booking.completed event per booking, success and rejection")
    void bookingCompletedEventPerRequest() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));
        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated());

        List<ILoggingEvent> afterSuccess = completedEvents();
        assertThat(afterSuccess).hasSize(1);
        Map<String, Object> success = keyValues(afterSuccess.get(0));
        assertThat(afterSuccess.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(success).containsEntry("outcome", "CONFIRMED");
        assertThat(success).containsEntry("dealershipId", D1.toString());
        assertThat(success.get("appointmentId")).asString().isNotBlank();
        assertThat(success.get("serviceTypeCode")).asString().isNotBlank();
        assertThat(success.get("durationMs")).isNotNull();

        api.book(C2, V2, D1, S2, MONDAY + "T09:30:00")
                .andExpect(status().isConflict());

        List<ILoggingEvent> afterRejection = completedEvents();
        assertThat(afterRejection).hasSize(2);
        Map<String, Object> rejected = keyValues(afterRejection.get(1));
        assertThat(afterRejection.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(rejected).containsEntry("outcome", NO_TECHNICIAN);
        assertThat(rejected).containsEntry("dealershipId", D1.toString());
        assertThat(rejected.get("serviceTypeCode")).asString().isNotBlank();
        assertThat(rejected.get("durationMs")).isNotNull();
        assertThat(rejected).doesNotContainKey("appointmentId");
    }

    @Test
    @DisplayName("AC-E02.4: Log events contain no customer name, email, phone or VIN")
    void bookingEventsContainNoPersonalData() throws Exception {
        List<String> personalValues = personalDataValues();
        assertThat(personalValues).as("fixture personal data").isNotEmpty();

        seeder.seed(T1, B1, at("09:00"), at("10:30"));
        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated());
        api.book(C2, V2, D1, S2, MONDAY + "T09:30:00")
                .andExpect(status().isConflict());

        assertThat(completedEvents()).hasSize(2);
        for (ILoggingEvent event : completedEvents()) {
            assertThat(keyValues(event).keySet()).isSubsetOf(ALLOWED_KEYS);
        }
        for (ILoggingEvent event : appender.list) {
            String text = render(event).toLowerCase(Locale.ROOT);
            for (String value : personalValues) {
                assertThat(text)
                        .as("log event '%s' must not contain '%s'", event.getFormattedMessage(), value)
                        .doesNotContain(value.toLowerCase(Locale.ROOT));
            }
        }
    }

    // ---- AC-E02.5 ----

    @Test
    @DisplayName("AC-E02.5: Business metrics are exposed at /actuator/prometheus")
    void businessMetricsExposed() throws Exception {
        String before = scrape();
        double confirmedBefore = counterValue(before, "scheduler_booking_attempts_total", "outcome", "CONFIRMED");
        double rejectedBefore = counterValue(before, "scheduler_booking_attempts_total", "outcome", NO_TECHNICIAN);

        seeder.seed(T1, B1, at("09:00"), at("10:30"));
        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated());
        api.book(C2, V2, D1, S2, MONDAY + "T09:30:00")
                .andExpect(status().isConflict());

        String after = scrape();
        assertThat(counterValue(after, "scheduler_booking_attempts_total", "outcome", "CONFIRMED"))
                .isEqualTo(confirmedBefore + 1.0);
        assertThat(counterValue(after, "scheduler_booking_attempts_total", "outcome", NO_TECHNICIAN))
                .isEqualTo(rejectedBefore + 1.0);

        assertThat(after).containsPattern("(?m)^scheduler_booking_duration_seconds_bucket\\{");
        assertThat(after).containsPattern("(?m)^scheduler_booking_duration_seconds_count\\{");
        assertThat(after).containsPattern("(?m)^scheduler_booking_duration_seconds_sum\\{");
        assertThat(after).containsPattern(
                "(?m)^scheduler_booking_conflict_retries_total\\{[^}]*constraint=\"[^\"]+\"");
    }

    // ---- AC-E02.6 ----

    @Test
    @DisplayName("AC-E02.6: Liveness probe is UP")
    void livenessIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("AC-E02.6: Readiness probe is UP and includes the database check")
    void readinessIsUpAndIncludesDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    // ---- helpers ----

    private List<ILoggingEvent> completedEvents() {
        List<ILoggingEvent> events = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            if (COMPLETED.equals(event.getFormattedMessage())) {
                events.add(event);
            }
        }
        return events;
    }

    private static Map<String, Object> keyValues(ILoggingEvent event) {
        Map<String, Object> result = new HashMap<>();
        if (event.getKeyValuePairs() != null) {
            event.getKeyValuePairs().forEach(kv -> result.put(kv.key, kv.value));
        }
        return result;
    }

    /** Everything an event could carry: message, key-value pairs, arguments, MDC, throwable. */
    private static String render(ILoggingEvent event) {
        StringBuilder text = new StringBuilder(event.getFormattedMessage());
        keyValues(event).forEach((key, value) -> text.append(' ').append(key).append('=').append(value));
        if (event.getArgumentArray() != null) {
            for (Object argument : event.getArgumentArray()) {
                text.append(' ').append(argument);
            }
        }
        if (event.getMDCPropertyMap() != null) {
            event.getMDCPropertyMap().forEach((key, value) -> text.append(' ').append(key).append('=').append(value));
        }
        if (event.getThrowableProxy() != null) {
            text.append(' ').append(event.getThrowableProxy().getMessage());
        }
        return text.toString();
    }

    /** Fixture customer names, emails, phones and vehicle VINs. Each column must yield data, so a rename fails loudly. */
    private List<String> personalDataValues() {
        List<String> values = new ArrayList<>();
        values.addAll(column("customer", "name"));
        values.addAll(column("customer", "email"));
        values.addAll(column("customer", "phone"));
        values.addAll(column("vehicle", "vin"));
        return values;
    }

    private List<String> column(String table, String column) {
        List<String> values = jdbc.sql("select " + column + " from " + table + " where " + column + " is not null")
                .query(String.class)
                .list();
        assertThat(values).as("fixture %s.%s", table, column).isNotEmpty();
        return values;
    }

    private String scrape() throws Exception {
        return mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** Value of the series {@code metric{...label="value"...}}, or 0 when the series is not present yet. */
    private static double counterValue(String scrape, String metric, String label, String value) {
        Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(metric) + "\\{[^}]*"
                + Pattern.quote(label + "=\"" + value + "\"") + "[^}]*\\}\\s+(\\S+)");
        Matcher matcher = pattern.matcher(scrape);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0;
    }
}
