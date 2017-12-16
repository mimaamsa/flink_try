package org.stsffap.cep.monitoring.authlogs.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author    Mahesh D
 */
public class LoginFailureSequence {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginFailureSequence.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int numFailedAttempts;
    private final String userName;
    private final List<LoginEvent>  failedEvents;

    public LoginFailureSequence(int numFailedAttempts, String userName, List<LoginEvent> failedEvents) {
        this.numFailedAttempts = numFailedAttempts;
        this.userName = userName;
        this.failedEvents = failedEvents;
    }

    public int getNumFailedAttempts() {
        return numFailedAttempts;
    }

    public String getUserName() {
        return userName;
    }

    public List<LoginEvent> getFailedEvents() {
        return Collections.unmodifiableList(failedEvents);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LoginFailureSequence) {
            LoginFailureSequence that = (LoginFailureSequence) obj;
            return this.canEquals(that) &&
                    Objects.equals(numFailedAttempts, that.numFailedAttempts) &&
                    Objects.equals(userName, that.userName) &&
                    Objects.equals(failedEvents, that.failedEvents);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(numFailedAttempts, userName, failedEvents);
    }

    public boolean canEquals(Object obj) {
        return obj instanceof LoginFailureSequence;
    }

    public void print() {
        try {
            LOGGER.info("Object sequence : {}", MAPPER.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
