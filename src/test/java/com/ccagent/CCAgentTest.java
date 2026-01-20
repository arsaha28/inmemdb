package com.ccagent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CCAgent
 */
public class CCAgentTest {
    private CCAgent agent;

    @BeforeEach
    public void setUp() {
        agent = new CCAgent();
    }

    @Test
    public void testAgentInitialization() {
        assertNotNull(agent, "CCAgent should be initialized");
    }

    @Test
    public void testAgentRun() {
        assertDoesNotThrow(() -> agent.run(), "Agent run should not throw exceptions");
    }
}
