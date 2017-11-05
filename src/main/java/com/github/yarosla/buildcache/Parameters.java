package com.github.yarosla.buildcache;

import com.beust.jcommander.Parameter;
import org.springframework.util.StringUtils;

public class Parameters {

    @Parameter(names = {"--debug", "-v"}, description = "Show debug log")
    private boolean debug;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(names = {"--limit", "-l"}, description = "Memory limit in megabytes")
    private long memoryLimit = 1000;

    @Parameter(names = {"--host", "-H"}, description = "Set http host to listen on")
    private String host = "0.0.0.0";

    @Parameter(names = {"--port", "-P"}, description = "Set http port to listen to")
    private int port = 8080;

    @Parameter(names = {"--user", "-u"}, description = "Set username for authenticated access")
    private String username;

    @Parameter(names = {"--password", "-p"}, description = "Set password for authenticated access")
    private String password;

    @Parameter(names = {"--help", "-h"}, help = true, description = "Display help")
    private boolean help;

    public boolean isDebug() {
        return debug;
    }

    public boolean isSecure() {
        return StringUtils.hasText(username) && StringUtils.hasText(password);
    }

    public long getMemoryLimit() {
        return memoryLimit * 1_000_000; // convert megabytes to bytes
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isHelp() {
        return help;
    }
}
