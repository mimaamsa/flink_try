package org.stsffap.cep.monitoring.authlogs.sources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stsffap.cep.monitoring.authlogs.events.LoginEvent;
import org.stsffap.cep.monitoring.utils.GetRandomIpString;
import org.stsffap.cep.monitoring.utils.GetRandomString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @author    Mahesh D
 */
public class LoginEventSource extends RichParallelSourceFunction<LoginEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginEventSource.class);

    private boolean running = true;
    private final LoginEventGenerator eventGenerator;
    private final long pause;

    public LoginEventSource(LoginEventGenerator eventGenerator, long pause) {
        this.eventGenerator = eventGenerator;
        this.pause = pause;
    }

    @Override
    public void open(Configuration configuration) {
        LOGGER.info("Opening event-source with runtime-context : numberOfTasks={}, taskIndex={}",
                getRuntimeContext().getNumberOfParallelSubtasks(),
                getRuntimeContext().getIndexOfThisSubtask());
    }

    @Override
    public void run(SourceContext<LoginEvent> sourceContext) throws Exception {
        long eventCounter = 0;
        int indexOfThisSubTask = getRuntimeContext().getIndexOfThisSubtask();
        LOGGER.info("Run invoked for subtask={}", indexOfThisSubTask);
        while (running) {
            sourceContext.collect(eventGenerator.get());
            if ((++eventCounter % 10) == 0) {
                LOGGER.info("Events generated so far for subtask={} is {}", indexOfThisSubTask, eventCounter);
            }
            Thread.sleep(pause);
        }
    }

    @Override
    public void cancel() {
        LOGGER.info("Cancel invoked for subtask={}", getRuntimeContext().getIndexOfThisSubtask());
        running = false;
    }

    /**
     * @author    Mahesh D
     */
    public static class LoginEventGenerator implements Serializable {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final String hostName;
        private final String userName;

        private final Random random;
        private final List<Pair<String,String>> programs;
        private final List<String> validStatus;

        public LoginEventGenerator() {
            this(GetRandomString.get(10),
                    GetRandomString.get(10),
                    GetRandomIpString.get(),
                    Arrays.asList(new Pair("a", "1"), new Pair("b", "2"), new Pair("c", "3"), new Pair("d", "4")),
                    Arrays.asList("SUCCESS", "FAILURE", "UNKNOWN", "FAILURE2", "FAILURE3", "FAILURE4"));
        }

        public LoginEventGenerator(
                String hostName, String userName, String fromIp,
                List<Pair<String, String>> programs, List<String> validStatus) {
            this.hostName = hostName;
            this.userName = userName;
            this.programs = programs;
            this.validStatus = validStatus;
            this.random = new Random();
        }

        public LoginEvent get() {
            final Pair<String,String> process = programs.get(random.nextInt(programs.size()));
            final LoginEvent event = new LoginEvent(
                    new Date().toString(), hostName,
                    process.getKey(), process.getValue(),
                    validStatus.get(random.nextInt(validStatus.size())),
                    userName, GetRandomIpString.get());

            if (false) {
                try {
                    LOGGER.debug("Event={}", MAPPER.writeValueAsString(event));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            return event;
        }
    }
}
