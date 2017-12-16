package org.stsffap.cep.monitoring.authlogs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static org.stsffap.cep.monitoring.authlogs.constants.SUCCESSFUL_LOGIN;

/**
 * @author    Mahesh D
 */
public class LoginEvent {
    // Dec 16 14:33:32 ip-172-31-16-250 sshd[19275]: Invalid user ubuntu1 from 49.207.63.253
    @JsonProperty
    private final String timeStamp;
    @JsonProperty
    private final String hostName;
    @JsonProperty
    private final String program;
    @JsonProperty
    private final String processId;
    @JsonProperty
    private final String status;
    @JsonProperty
    private final String userName;
    @JsonProperty
    private final String fromIp;

    public LoginEvent(
            String timeStamp, String hostName,
            String program, String processId,
            String status, String userName,
            String fromIp) {
        this.timeStamp = timeStamp;
        this.hostName = hostName;
        this.program = program;
        this.processId = processId;
        this.status = status;
        this.userName = userName;
        this.fromIp = fromIp;
    }

    public String getUserName() {
        return this.userName;
    }

    @JsonIgnore
    public boolean isSuccessfulLogin() {
        return status.compareTo(SUCCESSFUL_LOGIN) == 0;
    }

    @JsonIgnore
    public boolean isFailedLogin() {
        return !isSuccessfulLogin();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LoginEvent) {
            LoginEvent that = (LoginEvent) obj;
            return this.canEquals(that) &&
                    Objects.equals(timeStamp, that.timeStamp) &&
                    Objects.equals(hostName, that.hostName) &&
                    Objects.equals(program, that.program) &&
                    Objects.equals(processId, that.processId) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(userName, that.userName) &&
                    Objects.equals(fromIp, that.fromIp);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeStamp, hostName, program, processId, status, userName, fromIp);
    }

    public boolean canEquals(Object obj) {
        return obj instanceof LoginEvent;
    }
}
