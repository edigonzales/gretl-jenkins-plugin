package ch.so.agi.gretl.console;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.RootAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import jenkins.security.stapler.StaplerDispatchable;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

@Extension
public final class GretlConsole implements RootAction {
    private static final Pattern ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern REPOSITORY =
            Pattern.compile("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");

    @Override
    public String getIconFileName() {
        return Jenkins.get().hasPermission(Jenkins.READ) ? "symbol-terminal" : null;
    }

    @Override
    public String getDisplayName() {
        return "GRETL Console";
    }

    @Override
    public String getUrlName() {
        return "gretl-console";
    }

    public List<ConsoleJob> getJobs() {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.READ);
        return jenkins.getItems(WorkflowJob.class).stream()
                .map(GretlConsole::entry)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ConsoleJob::getTitle))
                .toList();
    }

    @StaplerDispatchable
    public ConsoleJob getDynamic(String id) {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.READ);
        if (!validId(id) && !(id != null && id.startsWith("dev-") && validId(id.substring(4)))) {
            return null;
        }
        Item item = jenkins.getItem("gretl-" + id);
        return item instanceof WorkflowJob job ? entry(job).orElse(null) : null;
    }

    private static boolean validId(String id) {
        return id != null && id.length() <= 40 && ID.matcher(id).matches();
    }

    static ConsoleJob developerJob(String id, String repository, String revision) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return null;
        }
        Item item = Jenkins.get().getItem("gretl-dev-" + id);
        if (item instanceof WorkflowJob job) {
            return entry(job).filter(entry -> entry.isDeveloper() && entry.isCanBuild()
                    && repository.equals(entry.getRepository()) && revision.equals(entry.getRevision()))
                    .orElse(null);
        }
        return null;
    }

    private static Optional<ConsoleJob> entry(WorkflowJob job) {
        if (job.isDisabled() || !job.hasPermission(Item.READ)) {
            return Optional.empty();
        }
        ConsoleMetadata property = job.getProperty(ConsoleMetadata.class);
        String encoded = property == null ? null : property.encoded();
        if (encoded == null || encoded.length() > 128 * 1024) {
            return Optional.empty();
        }
        try {
            String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            JSONObject metadata = JSONObject.fromObject(json);
            String id = metadata.optString("id", "");
            boolean developer = metadata.optBoolean("developer");
            if (metadata.optInt("schemaVersion") == 2 && validId(id)
                    && job.getName().equals((developer ? "gretl-dev-" : "gretl-") + id)
                    && metadata.optString("revision", "").matches("[a-f0-9]{40}")
                    && REPOSITORY.matcher(metadata.optString("url", "")).matches()) {
                return Optional.of(new ConsoleJob(job, metadata));
            }
        } catch (IllegalArgumentException | JSONException ignored) {
            // Invalid display metadata does not affect the underlying Jenkins job.
        }
        return Optional.empty();
    }
}
