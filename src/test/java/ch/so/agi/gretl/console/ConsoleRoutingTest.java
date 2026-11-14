package ch.so.agi.gretl.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class ConsoleRoutingTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private static final String REPO = "https://github.com/edigonzales/gretl-runtime-jobs";
    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";

    private WorkflowJob create(String name, boolean developer) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, name);
        String json = """
                {"schemaVersion":2,"id":"hello","revision":"%s","url":"%s","developer":%s}
                """.formatted(REVISION, REPO, developer);
        job.addProperty(new ConsoleMetadata(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8))));
        return job;
    }

    @Test
    public void routesOnlyJobsVisibleToTheCurrentUser() throws Exception {
        WorkflowJob normal = create("gretl-hello", false);
        WorkflowJob dev = create("gretl-dev-hello", true);
        GretlConsole console = new GretlConsole();
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER).everywhere().to("admin")
                .grant(Jenkins.READ).everywhere().to("viewer")
                .grant(Item.READ).onItems(normal).to("viewer"));

        try (var context = ACL.as(User.getById("viewer", true))) {
            assertEquals(1, console.getJobs().size());
            assertFalse(console.getDynamic("hello").isCanBuild());
            assertNull(console.getDynamic("dev-hello"));
            assertNull(GretlConsole.developerJob("hello", REPO, REVISION));
        }
        try (var context = ACL.as(User.getById("admin", true))) {
            ConsoleJob developer = GretlConsole.developerJob("hello", REPO, REVISION);
            assertNotNull(developer);
            assertTrue(developer.isCanBuild());
            assertEquals("dev-hello", developer.getRoute());
            assertNull(GretlConsole.developerJob("hello", REPO, "another-revision"));
            dev.setDisabled(true);
            assertNull(GretlConsole.developerJob("hello", REPO, REVISION));
        }
    }

    @Test
    public void acceptsOrdinaryBranches() {
        assertTrue(BranchPreview.validBranch("feature/2027-catalog"));
        assertFalse(BranchPreview.validBranch("refs/heads/main"));
        assertFalse(BranchPreview.validBranch("feature//catalog"));
    }
}
